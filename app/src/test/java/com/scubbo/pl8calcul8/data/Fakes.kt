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

    override suspend fun updateIncrement(liftId: Long, incrementLb: Double) {
        lifts.value = lifts.value.map {
            if (it.id == liftId) it.copy(incrementLb = incrementLb) else it
        }
    }

    override suspend fun dump(): List<Lift> = lifts.value

    override suspend fun byId(id: Long): Lift? = lifts.value.find { it.id == id }

    override suspend fun deleteAll() {
        lifts.value = emptyList()
    }
}

class FakeDraftDao : DraftDao {
    val drafts = mutableListOf<DraftExercise>()
    private var nextId = 1L

    override suspend fun load(): List<DraftExercise> = drafts.sortedBy { it.position }

    override suspend fun insertAll(drafts: List<DraftExercise>) {
        this.drafts += drafts.map { it.copy(id = nextId++) }
    }

    override suspend fun clear() = drafts.clear()
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

    var liftNames: Map<Long, String> = emptyMap()

    override suspend fun workoutLog(): List<WorkoutLogRow> =
        exercises
            .sortedWith(compareByDescending<Exercise> { workoutDate(it.workoutId) }.thenBy { it.id })
            .map {
                WorkoutLogRow(
                    workoutId = it.workoutId, date = workoutDate(it.workoutId),
                    liftName = liftNames[it.liftId] ?: "lift-${it.liftId}",
                    weightLb = it.weightLb, assignedReps = it.assignedReps,
                    assignedRpe = it.assignedRpe, sets = it.sets, rpe = it.rpe,
                    notes = it.notes,
                )
            }

    override suspend fun dumpWorkouts(): List<Workout> = workouts.toList()

    override suspend fun dumpExercises(): List<Exercise> = exercises.toList()

    override suspend fun deleteAllWorkouts() = workouts.clear()

    override suspend fun deleteAllExercises() = exercises.clear()

    private fun workoutDate(workoutId: Long) = workouts.first { it.id == workoutId }.date
}
