package com.scubbo.pl8calcul8.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LiftDao {
    @Insert
    suspend fun insert(lift: Lift): Long

    @Query("SELECT * FROM Lift ORDER BY name")
    fun all(): Flow<List<Lift>>

    @Query("UPDATE Lift SET incrementLb = :incrementLb WHERE id = :liftId")
    suspend fun updateIncrement(liftId: Long, incrementLb: Double)
}

/** Creates a lift with the default increment, returning it with its new id. */
suspend fun LiftDao.createLift(name: String): Lift {
    val lift = Lift(name = name.trim())
    return lift.copy(id = insert(lift))
}

/** One row of a lift's history: a completed exercise with its workout date. */
data class ExerciseHistoryEntry(
    val date: Long,
    val weightLb: Double,
    val assignedReps: Int,
    val assignedRpe: Double,
    val sets: Int,
    val rpe: Double,
    val notes: String?,
)

@Dao
interface WorkoutDao {
    @Insert
    suspend fun insert(workout: Workout): Long

    @Insert
    suspend fun insert(exercise: Exercise): Long

    @Query(
        """
        SELECT e.* FROM Exercise e
        JOIN Workout w ON e.workoutId = w.id
        WHERE e.liftId = :liftId
        ORDER BY w.date DESC, e.id DESC
        LIMIT 1
        """
    )
    suspend fun mostRecentExerciseForLift(liftId: Long): Exercise?

    @Query(
        """
        SELECT w.date AS date, e.weightLb AS weightLb, e.assignedReps AS assignedReps,
               e.assignedRpe AS assignedRpe, e.sets AS sets, e.rpe AS rpe, e.notes AS notes
        FROM Exercise e
        JOIN Workout w ON e.workoutId = w.id
        WHERE e.liftId = :liftId
        ORDER BY w.date DESC, e.id DESC
        """
    )
    suspend fun historyForLift(liftId: Long): List<ExerciseHistoryEntry>
}
