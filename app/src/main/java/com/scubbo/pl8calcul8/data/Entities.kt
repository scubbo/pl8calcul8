package com.scubbo.pl8calcul8.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

const val DEFAULT_INCREMENT_LB = 5.0

@Entity(indices = [Index(value = ["name"], unique = true)])
data class Lift(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Overrides the app-wide 5lb progression increment when present. */
    val incrementLb: Double? = null,
)

@Entity
data class Workout(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    /** Epoch milliseconds. */
    val date: Long,
)

/**
 * A completed exercise: one lift performed for [sets] sets of [assignedReps]
 * reps within a workout. A single weight and RPE are recorded for the whole
 * exercise; actual reps are assumed to match the assignment (deviations go
 * in [notes]).
 */
/**
 * One planned exercise of an in-progress workout, persisted so an unfinished
 * session survives the app's process being killed.
 */
@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Lift::class,
            parentColumns = ["id"],
            childColumns = ["liftId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("liftId")],
)
data class DraftExercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val position: Int,
    val liftId: Long,
    val reps: Int,
    val rpe: Double,
    val sets: Int,
    val advisedWeightLb: Double?,
    val resultWeightLb: Double?,
    val resultRpe: Double?,
    val resultNotes: String?,
    /** The workout's date (same on every row); null in pre-v3 drafts. */
    val sessionDate: Long? = null,
)

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Workout::class,
            parentColumns = ["id"],
            childColumns = ["workoutId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Lift::class,
            parentColumns = ["id"],
            childColumns = ["liftId"],
            onDelete = ForeignKey.RESTRICT,
        ),
    ],
    indices = [Index("workoutId"), Index("liftId")],
)
data class Exercise(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workoutId: Long,
    val liftId: Long,
    val assignedReps: Int,
    val assignedRpe: Double,
    val sets: Int,
    val weightLb: Double,
    val rpe: Double,
    val notes: String? = null,
)
