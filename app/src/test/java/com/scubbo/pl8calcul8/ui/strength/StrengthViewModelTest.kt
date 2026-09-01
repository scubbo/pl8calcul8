package com.scubbo.pl8calcul8.ui.strength

import com.scubbo.pl8calcul8.calc.ScoringCategory
import com.scubbo.pl8calcul8.calc.Sex
import com.scubbo.pl8calcul8.calc.StrengthLevel
import com.scubbo.pl8calcul8.data.BodyweightEntry
import com.scubbo.pl8calcul8.data.Exercise
import com.scubbo.pl8calcul8.data.FakeBodyweightDao
import com.scubbo.pl8calcul8.data.FakeLiftDao
import com.scubbo.pl8calcul8.data.FakeProfileStore
import com.scubbo.pl8calcul8.data.FakeWorkoutDao
import com.scubbo.pl8calcul8.data.Lift
import com.scubbo.pl8calcul8.data.Workout
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StrengthViewModelTest {
    private val liftDao = FakeLiftDao()
    private val workoutDao = FakeWorkoutDao()
    private val bodyweightDao = FakeBodyweightDao()
    private val profileStore = FakeProfileStore()
    private val vm = StrengthViewModel(liftDao, workoutDao, bodyweightDao, profileStore)

    private suspend fun recordExercise(liftId: Long, weightLb: Double, reps: Int, rpe: Double) {
        val workoutId = workoutDao.insert(Workout(date = 1_000L))
        workoutDao.insert(
            Exercise(
                workoutId = workoutId, liftId = liftId,
                assignedReps = reps, assignedRpe = rpe, sets = 3,
                weightLb = weightLb, rpe = rpe,
            )
        )
    }

    @Test
    fun `profile completeness requires bodyweight and birth year`() = runTest {
        vm.refresh()
        assertEquals(false, vm.state.value.profileComplete)

        profileStore.birthYear = 1990
        bodyweightDao.insert(BodyweightEntry(date = 1_000L, weightLb = 220.0))
        vm.refresh()

        assertEquals(true, vm.state.value.profileComplete)
    }

    @Test
    fun `scores designated lifts from their best recent e1RM`() = runTest {
        profileStore.birthYear = LocalDate.now().year - 36
        profileStore.sex = Sex.MALE
        bodyweightDao.insert(BodyweightEntry(date = 1_000L, weightLb = 220.0))
        val squatId = liftDao.insert(
            Lift(name = "Squat", scoringCategory = ScoringCategory.BACK_SQUAT.name)
        )
        liftDao.insert(Lift(name = "Pin Squat")) // not designated: ignored
        // 260 x5 @8 -> e1RM 320.6 -> between Intermediate (300) and Proficient (375)
        recordExercise(squatId, weightLb = 260.0, reps = 5, rpe = 8.0)

        vm.refresh()

        val scores = vm.state.value.scores
        val squat = scores.single()
        assertEquals(ScoringCategory.BACK_SQUAT, squat.category)
        assertEquals(StrengthLevel.INTERMEDIATE, squat.score.level)
        assertEquals(320.6, squat.oneRepMaxLb, 0.1)
    }

    @Test
    fun `uses the best e1RM among recent exercises not just the latest`() = runTest {
        profileStore.birthYear = LocalDate.now().year - 36
        bodyweightDao.insert(BodyweightEntry(date = 1_000L, weightLb = 220.0))
        val benchId = liftDao.insert(
            Lift(name = "Bench Press", scoringCategory = ScoringCategory.BENCH_PRESS.name)
        )
        recordExercise(benchId, weightLb = 225.0, reps = 5, rpe = 8.0) // e1RM 277.4
        recordExercise(benchId, weightLb = 205.0, reps = 5, rpe = 7.0) // e1RM 260.8 (later, lighter)

        vm.refresh()

        assertEquals(277.4, vm.state.value.scores.single().oneRepMaxLb, 0.1)
    }

    @Test
    fun `logging a bodyweight stores it`() = runTest {
        vm.logBodyweight(217.5)

        val entries = bodyweightDao.entries
        assertEquals(217.5, entries.single().weightLb, 1e-9)
        assertTrue(entries.single().date > 0)
    }
}
