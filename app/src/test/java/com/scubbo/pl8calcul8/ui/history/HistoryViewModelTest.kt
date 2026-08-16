package com.scubbo.pl8calcul8.ui.history

import com.scubbo.pl8calcul8.data.Exercise
import com.scubbo.pl8calcul8.data.FakeLiftDao
import com.scubbo.pl8calcul8.data.FakeWorkoutDao
import com.scubbo.pl8calcul8.data.Lift
import com.scubbo.pl8calcul8.data.Workout
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HistoryViewModelTest {
    private val liftDao = FakeLiftDao()
    private val workoutDao = FakeWorkoutDao()
    private val vm = HistoryViewModel(liftDao, workoutDao)

    private suspend fun record(liftId: Long, date: Long, weightLb: Double, reps: Int, rpe: Double) {
        val workoutId = workoutDao.insert(Workout(date = date))
        workoutDao.insert(
            Exercise(
                workoutId = workoutId, liftId = liftId,
                assignedReps = reps, assignedRpe = rpe, sets = 3,
                weightLb = weightLb, rpe = rpe,
            )
        )
    }

    @Test
    fun `history starts empty with no lift selected`() = runTest {
        assertTrue(vm.entries.value.isEmpty())
    }

    @Test
    fun `selecting a lift loads its history newest first`() = runTest {
        val bench = Lift(id = 1, name = "Bench Press")
        record(bench.id, date = 1_000L, weightLb = 185.0, reps = 5, rpe = 8.0)
        record(bench.id, date = 2_000L, weightLb = 190.0, reps = 5, rpe = 8.0)
        record(liftId = 99, date = 3_000L, weightLb = 315.0, reps = 5, rpe = 8.0)

        vm.selectLift(bench)

        assertEquals(bench, vm.selectedLift.value)
        val entries = vm.entries.value
        assertEquals(2, entries.size)
        assertEquals(2_000L, entries[0].date)
        assertEquals(190.0, entries[0].weightLb, 1e-9)
        assertEquals(1_000L, entries[1].date)
    }

    @Test
    fun `chart points are date-ascending with computed e1RM`() = runTest {
        val bench = Lift(id = 1, name = "Bench Press")
        // 200 x5 @8 -> 200 / 0.811 = 246.61
        record(bench.id, date = 2_000L, weightLb = 200.0, reps = 5, rpe = 8.0)
        // 100 x1 @10 -> e1RM 100
        record(bench.id, date = 1_000L, weightLb = 100.0, reps = 1, rpe = 10.0)

        vm.selectLift(bench)

        val oneRepMax = vm.oneRepMaxPoints.value
        assertEquals(listOf(1_000L, 2_000L), oneRepMax.map { it.first })
        assertEquals(100.0, oneRepMax[0].second, 0.01)
        assertEquals(246.61, oneRepMax[1].second, 0.01)

        val weights = vm.weightPoints.value
        assertEquals(listOf(1_000L, 2_000L), weights.map { it.first })
        assertEquals(100.0, weights[0].second, 1e-9)
        assertEquals(200.0, weights[1].second, 1e-9)
    }
}
