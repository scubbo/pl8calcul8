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
    fun `chart points are date-ascending with computed e1RM and tonnage`() = runTest {
        val bench = Lift(id = 1, name = "Bench Press")
        // 200 x5 @8 -> 200 / 0.811 = 246.61; tonnage 200*5*3 = 3000
        record(bench.id, date = 2_000L, weightLb = 200.0, reps = 5, rpe = 8.0)
        // 100 x1 @10 -> e1RM 100; tonnage 100*1*3 = 300
        record(bench.id, date = 1_000L, weightLb = 100.0, reps = 1, rpe = 10.0)

        vm.selectLift(bench)

        val oneRepMax = vm.oneRepMaxPoints.value
        assertEquals(listOf(1_000L, 2_000L), oneRepMax.map { it.first })
        assertEquals(100.0, oneRepMax[0].second, 0.01)
        assertEquals(246.61, oneRepMax[1].second, 0.01)

        val tonnage = vm.tonnagePoints.value
        assertEquals(listOf(1_000L, 2_000L), tonnage.map { it.first })
        assertEquals(300.0, tonnage[0].second, 1e-9)
        assertEquals(3_000.0, tonnage[1].second, 1e-9)
    }

    @Test
    fun `toggling lifts builds one series per selected lift`() = runTest {
        val bench = Lift(id = 1, name = "Bench Press")
        val squat = Lift(id = 2, name = "Squat")
        record(bench.id, date = 1_000L, weightLb = 200.0, reps = 5, rpe = 8.0)
        record(squat.id, date = 2_000L, weightLb = 300.0, reps = 5, rpe = 8.0)

        vm.toggleLift(bench)
        vm.toggleLift(squat)

        assertEquals(setOf(bench.id, squat.id), vm.multiSelectedIds.value)
        val series = vm.multiSeries.value
        assertEquals(listOf("Bench Press", "Squat"), series.map { it.lift.name })
        // Default metric is e1RM
        assertEquals(246.61, series[0].points.single().second, 0.01)
        assertEquals(369.91, series[1].points.single().second, 0.01)
    }

    @Test
    fun `toggling a selected lift removes its series`() = runTest {
        val bench = Lift(id = 1, name = "Bench Press")
        record(bench.id, date = 1_000L, weightLb = 200.0, reps = 5, rpe = 8.0)

        vm.toggleLift(bench)
        vm.toggleLift(bench)

        assertTrue(vm.multiSelectedIds.value.isEmpty())
        assertTrue(vm.multiSeries.value.isEmpty())
    }

    @Test
    fun `workout log lists workouts newest-first with their exercises`() = runTest {
        val benchId = liftDao.insert(Lift(name = "Bench Press"))
        val squatId = liftDao.insert(Lift(name = "Squat"))
        workoutDao.liftNames = mapOf(benchId to "Bench Press", squatId to "Squat")
        val w1 = workoutDao.insert(Workout(date = 1_000L))
        val w2 = workoutDao.insert(Workout(date = 2_000L))
        workoutDao.insert(
            Exercise(
                workoutId = w1, liftId = benchId,
                assignedReps = 5, assignedRpe = 8.0, sets = 3,
                weightLb = 185.0, rpe = 8.0, notes = "solid",
            )
        )
        workoutDao.insert(
            Exercise(
                workoutId = w2, liftId = benchId,
                assignedReps = 4, assignedRpe = 7.0, sets = 3,
                weightLb = 190.0, rpe = 7.5,
            )
        )
        workoutDao.insert(
            Exercise(
                workoutId = w2, liftId = squatId,
                assignedReps = 5, assignedRpe = 8.0, sets = 3,
                weightLb = 245.0, rpe = 8.0,
            )
        )

        vm.loadWorkoutLog()

        val log = vm.workoutLog.value
        assertEquals(2, log.size)
        assertEquals(2_000L, log[0].date)
        assertEquals(listOf("Bench Press", "Squat"), log[0].exercises.map { it.liftName })
        assertEquals(190.0, log[0].exercises[0].weightLb, 1e-9)
        assertEquals(1_000L, log[1].date)
        assertEquals("solid", log[1].exercises.single().notes)
    }

    @Test
    fun `switching metric recomputes series as tonnage`() = runTest {
        val bench = Lift(id = 1, name = "Bench Press")
        record(bench.id, date = 1_000L, weightLb = 200.0, reps = 5, rpe = 8.0)
        vm.toggleLift(bench)

        vm.setMetric(HistoryMetric.TONNAGE)

        assertEquals(HistoryMetric.TONNAGE, vm.metric.value)
        assertEquals(3_000.0, vm.multiSeries.value.single().points.single().second, 1e-9)
    }
}
