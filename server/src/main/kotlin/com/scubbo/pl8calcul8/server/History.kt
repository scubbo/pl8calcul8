package com.scubbo.pl8calcul8.server

import com.scubbo.pl8calcul8.backup.BackupPayload
import com.scubbo.pl8calcul8.calc.RpeCalculator
import kotlinx.serialization.Serializable

/** Per-lift history computed from a backup, as consumed by the web UI. */
@Serializable
data class HistoryResponse(val lifts: List<LiftHistory>)

@Serializable
data class LiftHistory(
    val id: Long,
    val name: String,
    /** Date-ascending. */
    val entries: List<HistoryEntry>,
)

@Serializable
data class HistoryEntry(
    val date: Long,
    val weightLb: Double,
    val assignedReps: Int,
    val assignedRpe: Double,
    val sets: Int,
    val rpe: Double,
    val notes: String?,
    /** Null when the recorded values fall outside the RPE chart. */
    val oneRepMax: Double?,
    val tonnage: Double,
)

fun buildHistory(payload: BackupPayload): HistoryResponse {
    val workoutDates = payload.workouts.associate { it.id to it.date }
    val exercisesByLift = payload.exercises.groupBy { it.liftId }
    val lifts = payload.lifts.mapNotNull { lift ->
        val entries = exercisesByLift[lift.id].orEmpty()
            .mapNotNull { exercise ->
                val date = workoutDates[exercise.workoutId] ?: return@mapNotNull null
                HistoryEntry(
                    date = date,
                    weightLb = exercise.weightLb,
                    assignedReps = exercise.assignedReps,
                    assignedRpe = exercise.assignedRpe,
                    sets = exercise.sets,
                    rpe = exercise.rpe,
                    notes = exercise.notes,
                    oneRepMax = runCatching {
                        RpeCalculator.estimateOneRepMax(
                            weightLb = exercise.weightLb,
                            reps = exercise.assignedReps,
                            rpe = exercise.rpe,
                        )
                    }.getOrNull(),
                    tonnage = exercise.weightLb * exercise.assignedReps * exercise.sets,
                )
            }
            .sortedBy { it.date }
        if (entries.isEmpty()) null else LiftHistory(id = lift.id, name = lift.name, entries = entries)
    }.sortedBy { it.name }
    return HistoryResponse(lifts)
}
