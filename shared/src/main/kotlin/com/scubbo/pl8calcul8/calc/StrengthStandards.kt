package com.scubbo.pl8calcul8.calc

import kotlin.math.pow

enum class Sex { MALE, FEMALE }

enum class StrengthLevel(val label: String) {
    UNTRAINED("Untrained"),
    NOVICE("Novice"),
    INTERMEDIATE("Intermediate"),
    PROFICIENT("Proficient"),
    ADVANCED("Advanced"),
    EXCEPTIONAL("Exceptional"),
    ELITE("Elite"),
    WORLD_CLASS("World class"),
}

/** The lift categories with published strength standards. */
enum class ScoringCategory(val label: String) {
    BACK_SQUAT("Back Squat"),
    FRONT_SQUAT("Front Squat"),
    DEADLIFT("Deadlift"),
    BENCH_PRESS("Bench Press"),
    INCLINE_BENCH_PRESS("Incline Bench Press"),
    OVERHEAD_PRESS("Overhead Press"),
    PUSH_PRESS("Push Press"),
    POWER_CLEAN("Power Clean"),
    PENDLAY_ROW("Pendlay Row"),
}

data class StrengthScore(
    val level: StrengthLevel,
    /** 0..1 fraction of the way from [level]'s threshold to the next level's. */
    val progressToNext: Double,
)

/**
 * Strength standards approximating symmetricstrength.com's: per-lift level
 * thresholds calibrated at a reference lifter (36-year-old 220lb male, from
 * the site's published table), scaled allometrically for bodyweight
 * (Lift ∝ BW^0.67 - Jaric's muscle-force exponent, standard in the
 * literature), discounted ~1%/year past age 40 (masters-lifter convention),
 * and scaled for sex.
 */
object StrengthStandards {
    private const val REFERENCE_BODYWEIGHT_LB = 220.0
    private const val ALLOMETRIC_EXPONENT = 0.67
    private const val AGE_DISCOUNT_PER_YEAR = 0.01
    private const val AGE_DISCOUNT_START = 40
    // Approximate female:male strength ratio for barbell lifts.
    private const val FEMALE_FACTOR = 0.65

    // Thresholds for the reference lifter, one row per level (Untrained ->
    // World class), from symmetricstrength.com/standards.
    private val referenceThresholds: Map<ScoringCategory, List<Double>> = mapOf(
        ScoringCategory.BACK_SQUAT to listOf(150.0, 225.0, 300.0, 375.0, 435.0, 500.0, 560.0, 625.0),
        ScoringCategory.FRONT_SQUAT to listOf(120.0, 180.0, 240.0, 300.0, 350.0, 400.0, 450.0, 500.0),
        ScoringCategory.DEADLIFT to listOf(170.0, 260.0, 345.0, 430.0, 505.0, 575.0, 645.0, 720.0),
        ScoringCategory.BENCH_PRESS to listOf(110.0, 170.0, 225.0, 280.0, 325.0, 375.0, 420.0, 465.0),
        ScoringCategory.INCLINE_BENCH_PRESS to listOf(90.0, 140.0, 185.0, 230.0, 270.0, 305.0, 345.0, 385.0),
        ScoringCategory.OVERHEAD_PRESS to listOf(75.0, 110.0, 145.0, 180.0, 210.0, 245.0, 275.0, 305.0),
        ScoringCategory.PUSH_PRESS to listOf(95.0, 145.0, 195.0, 240.0, 280.0, 325.0, 365.0, 405.0),
        ScoringCategory.POWER_CLEAN to listOf(95.0, 145.0, 195.0, 240.0, 280.0, 320.0, 360.0, 400.0),
        ScoringCategory.PENDLAY_ROW to listOf(90.0, 135.0, 185.0, 230.0, 265.0, 305.0, 345.0, 380.0),
    )

    /** Level thresholds (lb) for the given lifter, Untrained -> World class. */
    fun thresholds(
        category: ScoringCategory,
        bodyweightLb: Double,
        age: Int,
        sex: Sex,
    ): List<Double> {
        val bodyweightFactor = (bodyweightLb / REFERENCE_BODYWEIGHT_LB).pow(ALLOMETRIC_EXPONENT)
        val ageFactor = 1.0 - AGE_DISCOUNT_PER_YEAR * maxOf(0, age - AGE_DISCOUNT_START)
        val sexFactor = if (sex == Sex.FEMALE) FEMALE_FACTOR else 1.0
        return referenceThresholds.getValue(category).map {
            it * bodyweightFactor * ageFactor * sexFactor
        }
    }

    fun score(
        oneRepMaxLb: Double,
        category: ScoringCategory,
        bodyweightLb: Double,
        age: Int,
        sex: Sex,
    ): StrengthScore {
        val levels = StrengthLevel.entries
        val bounds = thresholds(category, bodyweightLb, age, sex)
        val levelIndex = bounds.indexOfLast { oneRepMaxLb >= it }
        return when {
            levelIndex < 0 -> StrengthScore(
                level = StrengthLevel.UNTRAINED,
                progressToNext = (oneRepMaxLb / bounds.first()).coerceIn(0.0, 1.0),
            )
            levelIndex >= bounds.size - 1 -> StrengthScore(StrengthLevel.WORLD_CLASS, 1.0)
            else -> StrengthScore(
                level = levels[levelIndex],
                progressToNext =
                    (oneRepMaxLb - bounds[levelIndex]) / (bounds[levelIndex + 1] - bounds[levelIndex]),
            )
        }
    }
}
