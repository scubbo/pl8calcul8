package com.scubbo.pl8calcul8.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface LiftDao {
    @Insert
    suspend fun insert(lift: Lift): Long

    @Query("SELECT * FROM Lift ORDER BY name")
    fun all(): Flow<List<Lift>>

    @Query("UPDATE Lift SET incrementLb = :incrementLb WHERE id = :liftId")
    suspend fun updateIncrement(liftId: Long, incrementLb: Double)

    @Query("SELECT * FROM Lift")
    suspend fun dump(): List<Lift>

    @Query("SELECT * FROM Lift WHERE id = :id")
    suspend fun byId(id: Long): Lift?

    @Query("DELETE FROM Lift")
    suspend fun deleteAll()
}

@Dao
interface DraftDao {
    @Query("SELECT * FROM DraftExercise ORDER BY position")
    suspend fun load(): List<DraftExercise>

    @Insert
    suspend fun insertAll(drafts: List<DraftExercise>)

    @Query("DELETE FROM DraftExercise")
    suspend fun clear()

    @Transaction
    suspend fun replaceAll(drafts: List<DraftExercise>) {
        clear()
        insertAll(drafts)
    }
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

    @Query("SELECT * FROM Workout")
    suspend fun dumpWorkouts(): List<Workout>

    @Query("SELECT * FROM Exercise")
    suspend fun dumpExercises(): List<Exercise>

    @Query("DELETE FROM Workout")
    suspend fun deleteAllWorkouts()

    @Query("DELETE FROM Exercise")
    suspend fun deleteAllExercises()
}
