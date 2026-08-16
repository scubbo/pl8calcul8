package com.scubbo.pl8calcul8.ui.session

import com.scubbo.pl8calcul8.data.Exercise
import com.scubbo.pl8calcul8.data.ExerciseHistoryEntry
import com.scubbo.pl8calcul8.data.Lift
import com.scubbo.pl8calcul8.data.LiftDao
import com.scubbo.pl8calcul8.data.Workout
import com.scubbo.pl8calcul8.data.WorkoutDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

private class FakeLiftDao : LiftDao {
    val lifts = MutableStateFlow<List<Lift>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(lift: Lift): Long {
        val id = nextId++
        lifts.value += lift.copy(id = id)
        return id
    }

    override fun all(): Flow<List<Lift>> = lifts
}

private class FakeWorkoutDao : WorkoutDao {
    val workouts = mutableListOf<Workout>()
    val exercises = mutableListOf<Exercise>()
    private var nextWorkoutId = 1L
    private var nextExerciseId = 1L

    override suspend fun insert(workout: Workout): Long {
        val id = nextWorkoutId++
        workouts += workout.copy(id = id)
        return id
    }

    override suspend fun insert(exercise: Exercise): Long {
        val id = nextExerciseId++
        exercises += exercise.copy(id = id)
        return id
    }

    override suspend fun mostRecentExerciseForLift(liftId: Long): Exercise? =
        exercises.filter { it.liftId == liftId }.maxByOrNull { workoutDate(it.workoutId) }

    override suspend fun historyForLift(liftId: Long): List<ExerciseHistoryEntry> =
        exercises.filter { it.liftId == liftId }
            .sortedByDescending { workoutDate(it.workoutId) }
            .map {
                ExerciseHistoryEntry(
                    date = workoutDate(it.workoutId), weightLb = it.weightLb,
                    assignedReps = it.assignedReps, assignedRpe = it.assignedRpe,
                    sets = it.sets, rpe = it.rpe, notes = it.notes,
                )
            }

    private fun workoutDate(workoutId: Long) = workouts.first { it.id == workoutId }.date
}

class SessionViewModelTest {
    private val liftDao = FakeLiftDao()
    private val workoutDao = FakeWorkoutDao()
    private val clock = { 42_000L }
    private val vm = SessionViewModel(liftDao, workoutDao, clock)

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
}
