package com.scubbo.pl8calcul8.backup

import kotlinx.serialization.Serializable

/**
 * The full contents of the app's database, as uploaded to and restored from
 * the backup server. Shared between the Android app and the server so the
 * two cannot drift apart.
 */
@Serializable
data class BackupPayload(
    /** Payload schema version, for forward compatibility. */
    val version: Int = 1,
    val lifts: List<BackupLift>,
    val workouts: List<BackupWorkout>,
    val exercises: List<BackupExercise>,
    val bodyweights: List<BackupBodyweight> = emptyList(),
)

@Serializable
data class BackupLift(
    val id: Long,
    val name: String,
    val incrementLb: Double,
    val scoringCategory: String? = null,
)

@Serializable
data class BackupBodyweight(
    val id: Long,
    val date: Long,
    val weightLb: Double,
)

@Serializable
data class BackupWorkout(
    val id: Long,
    val date: Long,
)

@Serializable
data class BackupExercise(
    val id: Long,
    val workoutId: Long,
    val liftId: Long,
    val assignedReps: Int,
    val assignedRpe: Double,
    val sets: Int,
    val weightLb: Double,
    val rpe: Double,
    val notes: String?,
)
