package com.scubbo.pl8calcul8.calc

import kotlin.math.ceil

/**
 * The RTS/Tuchscherer RPE chart: percentage of estimated 1RM implied by
 * completing a given number of reps at a given RPE.
 *
 * Structure: for a set of n reps at RPE r, the lifter had (10 - r) reps in
 * reserve, so the chart is diagonal-consistent: n@r == (n+1)@(r+1).
 */
object RpeChart {
    const val MIN_REPS = 1
    const val MAX_REPS = 12
    const val MIN_RPE = 6.5
    const val MAX_RPE = 10.0

    // Rows are reps 1..12; columns are RPE 10, 9.5, 9, ... 6.5.
    private val percentages = arrayOf(
        doubleArrayOf(100.0, 97.8, 95.5, 93.9, 92.2, 90.7, 89.2, 87.8),
        doubleArrayOf(95.5, 93.9, 92.2, 90.7, 89.2, 87.8, 86.3, 85.0),
        doubleArrayOf(92.2, 90.7, 89.2, 87.8, 86.3, 85.0, 83.7, 82.4),
        doubleArrayOf(89.2, 87.8, 86.3, 85.0, 83.7, 82.4, 81.1, 79.9),
        doubleArrayOf(86.3, 85.0, 83.7, 82.4, 81.1, 79.9, 78.6, 77.4),
        doubleArrayOf(83.7, 82.4, 81.1, 79.9, 78.6, 77.4, 76.2, 75.1),
        doubleArrayOf(81.1, 79.9, 78.6, 77.4, 76.2, 75.1, 73.9, 72.3),
        doubleArrayOf(78.6, 77.4, 76.2, 75.1, 73.9, 72.3, 70.7, 69.4),
        doubleArrayOf(76.2, 75.1, 73.9, 72.3, 70.7, 69.4, 68.0, 66.7),
        doubleArrayOf(73.9, 72.3, 70.7, 69.4, 68.0, 66.7, 65.3, 64.0),
        doubleArrayOf(70.7, 69.4, 68.0, 66.7, 65.3, 64.0, 62.6, 61.3),
        doubleArrayOf(68.0, 66.7, 65.3, 64.0, 62.6, 61.3, 59.9, 58.6),
    )

    /** Fraction of 1RM (e.g. 0.811) for the given reps and RPE. */
    fun percentage(reps: Int, rpe: Double): Double {
        require(reps in MIN_REPS..MAX_REPS) { "reps must be in $MIN_REPS..$MAX_REPS, was $reps" }
        val halfSteps = rpe * 2
        require(halfSteps == halfSteps.toInt().toDouble() && rpe in MIN_RPE..MAX_RPE) {
            "rpe must be in $MIN_RPE..$MAX_RPE in 0.5 steps, was $rpe"
        }
        val column = ((MAX_RPE - rpe) * 2).toInt()
        return percentages[reps - 1][column] / 100.0
    }
}

/** Rounds to the nearest multiple of 5, with exact midpoints rounding down. */
fun roundToNearest5TiesDown(weightLb: Double): Double = ceil(weightLb / 5.0 - 0.5) * 5.0

object RpeCalculator {
    /** The 1RM implied by lifting [weightLb] for [reps] reps at [rpe]. */
    fun estimateOneRepMax(weightLb: Double, reps: Int, rpe: Double): Double =
        weightLb / RpeChart.percentage(reps, rpe)

    /** The weight implied by [oneRepMaxLb] for an assigned [reps] at [rpe]. */
    fun targetWeight(oneRepMaxLb: Double, reps: Int, rpe: Double): Double =
        oneRepMaxLb * RpeChart.percentage(reps, rpe)

    /**
     * Advised weight for a new assignment, based on the most recent completed
     * exercise for the same lift: ratio math from the previous result, plus
     * the lift's progression increment, rounded to the nearest 5lb.
     */
    fun adviseWeight(
        previousWeightLb: Double,
        previousReps: Int,
        previousRpe: Double,
        assignedReps: Int,
        assignedRpe: Double,
        incrementLb: Double,
    ): Double {
        val oneRepMax = estimateOneRepMax(previousWeightLb, previousReps, previousRpe)
        val target = targetWeight(oneRepMax, assignedReps, assignedRpe)
        return roundToNearest5TiesDown(target + incrementLb)
    }
}
