package com.scubbo.pl8calcul8.data.backup

import com.scubbo.pl8calcul8.backup.BackupExercise
import com.scubbo.pl8calcul8.backup.BackupLift
import com.scubbo.pl8calcul8.backup.BackupPayload
import com.scubbo.pl8calcul8.backup.BackupWorkout
import com.scubbo.pl8calcul8.data.Exercise
import com.scubbo.pl8calcul8.data.Lift
import com.scubbo.pl8calcul8.data.Workout

fun buildPayload(
    lifts: List<Lift>,
    workouts: List<Workout>,
    exercises: List<Exercise>,
): BackupPayload = BackupPayload(
    lifts = lifts.map { BackupLift(id = it.id, name = it.name, incrementLb = it.incrementLb) },
    workouts = workouts.map { BackupWorkout(id = it.id, date = it.date) },
    exercises = exercises.map {
        BackupExercise(
            id = it.id, workoutId = it.workoutId, liftId = it.liftId,
            assignedReps = it.assignedReps, assignedRpe = it.assignedRpe,
            sets = it.sets, weightLb = it.weightLb, rpe = it.rpe, notes = it.notes,
        )
    },
)

fun BackupPayload.toLifts(): List<Lift> =
    lifts.map { Lift(id = it.id, name = it.name, incrementLb = it.incrementLb) }

fun BackupPayload.toWorkouts(): List<Workout> =
    workouts.map { Workout(id = it.id, date = it.date) }

fun BackupPayload.toExercises(): List<Exercise> =
    exercises.map {
        Exercise(
            id = it.id, workoutId = it.workoutId, liftId = it.liftId,
            assignedReps = it.assignedReps, assignedRpe = it.assignedRpe,
            sets = it.sets, weightLb = it.weightLb, rpe = it.rpe, notes = it.notes,
        )
    }
