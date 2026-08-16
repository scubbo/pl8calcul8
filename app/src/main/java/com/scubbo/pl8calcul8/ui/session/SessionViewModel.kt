package com.scubbo.pl8calcul8.ui.session

import androidx.lifecycle.ViewModel
import com.scubbo.pl8calcul8.calc.RpeCalculator
import com.scubbo.pl8calcul8.data.Exercise
import com.scubbo.pl8calcul8.data.Lift
import com.scubbo.pl8calcul8.data.LiftDao
import com.scubbo.pl8calcul8.data.Workout
import com.scubbo.pl8calcul8.data.WorkoutDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the lifter actually did, entered once the exercise is complete. */
data class ExerciseResult(
    val weightLb: Double,
    val rpe: Double,
    val notes: String?,
)

/** An exercise planned for this session, with its advice and eventual result. */
data class PlannedExercise(
    val lift: Lift,
    val reps: Int,
    val rpe: Double,
    val sets: Int,
    /** null when the lift has no history: the lifter picks a starting weight. */
    val advisedWeightLb: Double?,
    val result: ExerciseResult? = null,
)

class SessionViewModel(
    liftDao: LiftDao,
    private val workoutDao: WorkoutDao,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val lifts: Flow<List<Lift>> = liftDao.all()

    private val _planned = MutableStateFlow<List<PlannedExercise>>(emptyList())
    val planned: StateFlow<List<PlannedExercise>> = _planned.asStateFlow()

    suspend fun addExercise(lift: Lift, reps: Int, rpe: Double, sets: Int) {
        val previous = workoutDao.mostRecentExerciseForLift(lift.id)
        val advised = previous?.let {
            RpeCalculator.adviseWeight(
                previousWeightLb = it.weightLb,
                previousReps = it.assignedReps,
                previousRpe = it.rpe,
                assignedReps = reps,
                assignedRpe = rpe,
                incrementLb = lift.incrementLb,
            )
        }
        _planned.value += PlannedExercise(
            lift = lift, reps = reps, rpe = rpe, sets = sets,
            advisedWeightLb = advised,
        )
    }

    fun recordResult(index: Int, weightLb: Double, rpe: Double, notes: String?) {
        _planned.value = _planned.value.mapIndexed { i, exercise ->
            if (i == index) {
                exercise.copy(result = ExerciseResult(weightLb, rpe, notes?.takeIf { it.isNotBlank() }))
            } else {
                exercise
            }
        }
    }

    /** Persists all recorded exercises as a workout. No-op if nothing was recorded. */
    suspend fun finishSession() {
        val recorded = _planned.value.filter { it.result != null }
        if (recorded.isEmpty()) return
        val workoutId = workoutDao.insert(Workout(date = clock()))
        recorded.forEach { exercise ->
            val result = exercise.result!!
            workoutDao.insert(
                Exercise(
                    workoutId = workoutId,
                    liftId = exercise.lift.id,
                    assignedReps = exercise.reps,
                    assignedRpe = exercise.rpe,
                    sets = exercise.sets,
                    weightLb = result.weightLb,
                    rpe = result.rpe,
                    notes = result.notes,
                )
            )
        }
        _planned.value = emptyList()
    }
}
