package com.scubbo.pl8calcul8.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeLiftDao : LiftDao {
    val lifts = MutableStateFlow<List<Lift>>(emptyList())
    private var nextId = 1L

    override suspend fun insert(lift: Lift): Long {
        val id = nextId++
        lifts.value += lift.copy(id = id)
        return id
    }

    override fun all(): Flow<List<Lift>> = lifts
}

class FakeWorkoutDao : WorkoutDao {
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
