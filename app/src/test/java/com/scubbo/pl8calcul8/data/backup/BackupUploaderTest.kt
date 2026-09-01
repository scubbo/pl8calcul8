package com.scubbo.pl8calcul8.data.backup

import com.scubbo.pl8calcul8.data.FakeBodyweightDao
import com.scubbo.pl8calcul8.data.FakeLiftDao
import com.scubbo.pl8calcul8.data.FakeWorkoutDao
import com.scubbo.pl8calcul8.data.Lift
import com.scubbo.pl8calcul8.data.Workout
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupUploaderTest {
    private val liftDao = FakeLiftDao()
    private val workoutDao = FakeWorkoutDao()
    private val api = FakeBackupApi()
    private val configStore = FakeConfigStore()
    private val uploader = BackupUploader(
        liftDao = liftDao,
        workoutDao = workoutDao,
        configStore = configStore,
        bodyweightDao = FakeBodyweightDao(),
        apiFactory = { _, _ -> api },
    )

    @Test
    fun `uploads the database contents and reports counts`() = runTest {
        configStore.serverUrl = "https://backup.test"
        configStore.token = "tok"
        liftDao.insert(Lift(name = "Squat"))
        workoutDao.insert(Workout(date = 1_000L))

        val status = uploader.backup()

        assertEquals(1, api.uploaded!!.workouts.size)
        assertTrue(status!!.contains("Backed up 1 workouts"))
    }

    @Test
    fun `reports failure without throwing`() = runTest {
        configStore.serverUrl = "https://backup.test"
        configStore.token = "tok"
        api.failWith = BackupException("server said 503")

        val status = uploader.backup()

        assertTrue(status!!.contains("503"))
    }

    @Test
    fun `returns null when not configured`() = runTest {
        assertNull(uploader.backup())
        assertNull(api.uploaded)
    }
}
