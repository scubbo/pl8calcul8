package com.scubbo.pl8calcul8.ui.session

import com.scubbo.pl8calcul8.data.Exercise
import com.scubbo.pl8calcul8.data.FakeDraftDao
import com.scubbo.pl8calcul8.data.FakeLiftDao
import com.scubbo.pl8calcul8.data.FakeWorkoutDao
import com.scubbo.pl8calcul8.data.Lift
import com.scubbo.pl8calcul8.data.Workout
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionViewModelTest {
    private val liftDao = FakeLiftDao()
    private val workoutDao = FakeWorkoutDao()
    private val draftDao = FakeDraftDao()
    private val clock = { 42_000L }
    private val vm = SessionViewModel(liftDao, workoutDao, draftDao, clock)

    private suspend fun seedHistory(lift: Lift, weightLb: Double, reps: Int, rpe: Double) {
        val workoutId = workoutDao.insert(Workout(date = 1_000L))
        workoutDao.insert(
            Exercise(
                workoutId = workoutId, liftId = lift.id,
                assignedReps = reps, assignedRpe = rpe, sets = 3,
                weightLb = weightLb, rpe = rpe,
            )
        )
    }

    @Test
    fun `adding an exercise with history computes advised weight`() = runTest {
        val bench = Lift(id = 1, name = "Bench Press", incrementLb = 5.0)
        // 200lb 5@8 -> e1RM 246.6 -> 4@7 target 200 -> +5 -> 205
        seedHistory(bench, weightLb = 200.0, reps = 5, rpe = 8.0)

        vm.addExercise(bench, reps = 4, rpe = 7.0, sets = 3)

        val planned = vm.planned.value.single()
        assertEquals(205.0, planned.advisedWeightLb!!, 1e-9)
    }

    @Test
    fun `advised weight uses the lift's own increment`() = runTest {
        val squat = Lift(id = 2, name = "Squat", incrementLb = 10.0)
        seedHistory(squat, weightLb = 200.0, reps = 5, rpe = 8.0)

        vm.addExercise(squat, reps = 4, rpe = 7.0, sets = 3)

        assertEquals(210.0, vm.planned.value.single().advisedWeightLb!!, 1e-9)
    }

    @Test
    fun `adding an exercise with no history has no advised weight`() = runTest {
        val newLift = Lift(id = 3, name = "Front Squat", incrementLb = 5.0)

        vm.addExercise(newLift, reps = 5, rpe = 8.0, sets = 3)

        assertNull(vm.planned.value.single().advisedWeightLb)
    }

    @Test
    fun `recording a result attaches it to the right exercise`() = runTest {
        val bench = Lift(id = 1, name = "Bench Press")
        val squat = Lift(id = 2, name = "Squat")
        vm.addExercise(bench, reps = 4, rpe = 7.0, sets = 3)
        vm.addExercise(squat, reps = 5, rpe = 8.0, sets = 3)

        vm.recordResult(index = 1, weightLb = 225.0, rpe = 8.5, notes = "felt heavy")

        assertNull(vm.planned.value[0].result)
        val result = vm.planned.value[1].result!!
        assertEquals(225.0, result.weightLb, 1e-9)
        assertEquals(8.5, result.rpe, 1e-9)
        assertEquals("felt heavy", result.notes)
    }

    @Test
    fun `finishing saves a dated workout with recorded exercises only`() = runTest {
        val bench = Lift(id = 1, name = "Bench Press")
        val squat = Lift(id = 2, name = "Squat")
        vm.addExercise(bench, reps = 4, rpe = 7.0, sets = 3)
        vm.addExercise(squat, reps = 5, rpe = 8.0, sets = 3)
        vm.recordResult(index = 0, weightLb = 205.0, rpe = 7.5, notes = null)

        vm.finishSession()

        val workout = workoutDao.workouts.single()
        assertEquals(42_000L, workout.date)
        val saved = workoutDao.exercises.single()
        assertEquals(workout.id, saved.workoutId)
        assertEquals(bench.id, saved.liftId)
        assertEquals(4, saved.assignedReps)
        assertEquals(7.0, saved.assignedRpe, 1e-9)
        assertEquals(3, saved.sets)
        assertEquals(205.0, saved.weightLb, 1e-9)
        assertEquals(7.5, saved.rpe, 1e-9)
        assertNull(saved.notes)
    }

    @Test
    fun `a new lift can be created mid-session`() = runTest {
        val created = vm.addLift("  Front Squat ")

        assertEquals("Front Squat", created.name)
        assertEquals(5.0, created.incrementLb, 1e-9)
        val stored = liftDao.lifts.value.single()
        assertEquals(created.id, stored.id)
        assertEquals("Front Squat", stored.name)
    }

    @Test
    fun `finishing with nothing recorded saves nothing`() = runTest {
        val bench = Lift(id = 1, name = "Bench Press")
        vm.addExercise(bench, reps = 4, rpe = 7.0, sets = 3)

        vm.finishSession()

        assertTrue(workoutDao.workouts.isEmpty())
        assertTrue(workoutDao.exercises.isEmpty())
    }

    @Test
    fun `session mutations persist a draft`() = runTest {
        val bench = Lift(id = 1, name = "Bench Press")
        liftDao.insert(bench.copy(id = 0))
        vm.addExercise(bench, reps = 4, rpe = 7.0, sets = 3)

        assertEquals(1, draftDao.drafts.size)
        assertNull(draftDao.drafts[0].resultWeightLb)

        vm.recordResult(index = 0, weightLb = 205.0, rpe = 7.5, notes = "ok")

        assertEquals(205.0, draftDao.drafts.single().resultWeightLb!!, 1e-9)
        assertEquals("ok", draftDao.drafts.single().resultNotes)
    }

    @Test
    fun `a persisted draft is reloaded into a fresh session`() = runTest {
        val benchId = liftDao.insert(Lift(name = "Bench Press"))
        val bench = liftDao.lifts.value.single()
        vm.addExercise(bench, reps = 4, rpe = 7.0, sets = 3)
        vm.recordResult(index = 0, weightLb = 205.0, rpe = 7.5, notes = null)

        // Simulates process death: a brand-new ViewModel over the same storage
        val revived = SessionViewModel(liftDao, workoutDao, draftDao, clock)
        revived.loadDraft()

        val planned = revived.planned.value.single()
        assertEquals(benchId, planned.lift.id)
        assertEquals(4, planned.reps)
        assertEquals(205.0, planned.result!!.weightLb, 1e-9)
    }

    @Test
    fun `finishing clears the draft`() = runTest {
        liftDao.insert(Lift(name = "Bench Press"))
        val bench = liftDao.lifts.value.single()
        vm.addExercise(bench, reps = 4, rpe = 7.0, sets = 3)
        vm.recordResult(index = 0, weightLb = 205.0, rpe = 7.5, notes = null)

        vm.finishSession()

        assertTrue(draftDao.drafts.isEmpty())
    }

    @Test
    fun `discarding clears the session and the draft`() = runTest {
        liftDao.insert(Lift(name = "Bench Press"))
        val bench = liftDao.lifts.value.single()
        vm.addExercise(bench, reps = 4, rpe = 7.0, sets = 3)

        vm.discardSession()

        assertTrue(vm.planned.value.isEmpty())
        assertTrue(draftDao.drafts.isEmpty())
        assertTrue(workoutDao.workouts.isEmpty())
    }
}
