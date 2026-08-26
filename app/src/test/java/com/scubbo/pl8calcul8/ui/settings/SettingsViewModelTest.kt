package com.scubbo.pl8calcul8.ui.settings

import com.scubbo.pl8calcul8.backup.BackupPayload
import com.scubbo.pl8calcul8.data.Exercise
import com.scubbo.pl8calcul8.data.FakeLiftDao
import com.scubbo.pl8calcul8.data.FakeWorkoutDao
import com.scubbo.pl8calcul8.data.Lift
import com.scubbo.pl8calcul8.data.Workout
import com.scubbo.pl8calcul8.data.backup.BackupException
import com.scubbo.pl8calcul8.data.backup.FakeBackupApi
import com.scubbo.pl8calcul8.data.backup.FakeConfigStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsViewModelTest {
    private val liftDao = FakeLiftDao()
    private val workoutDao = FakeWorkoutDao()
    private val api = FakeBackupApi()
    private val configStore = FakeConfigStore()
    private val vm = SettingsViewModel(
        liftDao = liftDao,
        workoutDao = workoutDao,
        configStore = configStore,
        apiFactory = { _, _ -> api },
    )

    @Test
    fun `adding a lift stores it with the default increment`() = runTest {
        val created = vm.addLift(" Incline Bench ")

        assertEquals("Incline Bench", created.name)
        assertEquals(5.0, created.incrementLb, 1e-9)
        assertEquals(created, liftDao.lifts.value.single())
    }

    @Test
    fun `setting an increment updates the lift`() = runTest {
        val lift = Lift(id = 0, name = "Deadlift")
        val id = liftDao.insert(lift)

        vm.setIncrement(liftDao.lifts.value.single(), 10.0)

        assertEquals(10.0, liftDao.lifts.value.single { it.id == id }.incrementLb, 1e-9)
    }

    @Test
    fun `saving config persists to the store`() {
        vm.saveConfig("https://backup.test", "tok")

        assertEquals("https://backup.test", configStore.serverUrl)
        assertEquals("tok", configStore.token)
    }

    private suspend fun seedDb() {
        liftDao.insert(Lift(name = "Squat", incrementLb = 10.0))
        val workoutId = workoutDao.insert(Workout(date = 1_000L))
        workoutDao.insert(
            Exercise(
                workoutId = workoutId, liftId = 1,
                assignedReps = 5, assignedRpe = 8.0, sets = 3,
                weightLb = 225.0, rpe = 8.5, notes = "hard",
            )
        )
    }

    @Test
    fun `backup uploads the database contents`() = runTest {
        seedDb()
        vm.saveConfig("https://backup.test", "tok")

        vm.backup()

        val payload = api.uploaded!!
        assertEquals(1, payload.lifts.size)
        assertEquals("Squat", payload.lifts[0].name)
        assertEquals(10.0, payload.lifts[0].incrementLb, 1e-9)
        assertEquals(1, payload.workouts.size)
        assertEquals(1, payload.exercises.size)
        assertEquals("hard", payload.exercises[0].notes)
        assertTrue(vm.backupStatus.value!!.contains("Backed up"))
    }

    @Test
    fun `backup failure reports the error`() = runTest {
        seedDb()
        vm.saveConfig("https://backup.test", "tok")
        api.failWith = BackupException("server said 401")

        vm.backup()

        assertTrue(vm.backupStatus.value!!.contains("401"))
    }

    @Test
    fun `restore replaces the database with the downloaded payload`() = runTest {
        seedDb()
        vm.saveConfig("https://backup.test", "tok")
        vm.backup()
        val snapshot = api.uploaded!!
        // Local data diverges after the backup
        liftDao.insert(Lift(name = "Bench Press"))
        api.stored = snapshot

        vm.restore()

        assertEquals(listOf("Squat"), liftDao.lifts.value.map { it.name })
        assertEquals(1, workoutDao.workouts.size)
        assertEquals(1, workoutDao.exercises.size)
        assertEquals("hard", workoutDao.exercises[0].notes)
        assertTrue(vm.backupStatus.value!!.contains("Restored"))
    }

    @Test
    fun `restore with no backup available reports it`() = runTest {
        vm.saveConfig("https://backup.test", "tok")
        api.stored = null

        vm.restore()

        assertTrue(vm.backupStatus.value!!.contains("No backup"))
        assertNull(api.uploaded)
    }
}
