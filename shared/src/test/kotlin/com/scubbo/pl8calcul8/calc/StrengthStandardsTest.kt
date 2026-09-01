package com.scubbo.pl8calcul8.calc

import org.junit.Assert.assertEquals
import org.junit.Test

class StrengthStandardsTest {

    @Test
    fun `reference standards match the calibration point exactly`() {
        // Calibration data: 36-year-old 220lb male (symmetricstrength.com)
        val thresholds = StrengthStandards.thresholds(
            category = ScoringCategory.BACK_SQUAT,
            bodyweightLb = 220.0, age = 36, sex = Sex.MALE,
        )
        assertEquals(
            listOf(150.0, 225.0, 300.0, 375.0, 435.0, 500.0, 560.0, 625.0),
            thresholds.map { Math.round(it).toDouble() },
        )
    }

    @Test
    fun `standards scale allometrically with bodyweight`() {
        val at220 = StrengthStandards.thresholds(ScoringCategory.BENCH_PRESS, 220.0, 36, Sex.MALE)
        val at180 = StrengthStandards.thresholds(ScoringCategory.BENCH_PRESS, 180.0, 36, Sex.MALE)

        // (180/220)^0.67 = 0.8742...
        val expectedRatio = Math.pow(180.0 / 220.0, 0.67)
        assertEquals(expectedRatio, at180[2] / at220[2], 1e-9)
        // Sanity: a lighter lifter has lower absolute thresholds
        assertEquals(true, at180[2] < at220[2])
    }

    @Test
    fun `age over 40 lowers thresholds`() {
        val at36 = StrengthStandards.thresholds(ScoringCategory.DEADLIFT, 220.0, 36, Sex.MALE)
        val at50 = StrengthStandards.thresholds(ScoringCategory.DEADLIFT, 220.0, 50, Sex.MALE)
        val at60 = StrengthStandards.thresholds(ScoringCategory.DEADLIFT, 220.0, 60, Sex.MALE)

        assertEquals(true, at50[2] < at36[2])
        assertEquals(true, at60[2] < at50[2])
    }

    @Test
    fun `female thresholds are lower than male`() {
        val male = StrengthStandards.thresholds(ScoringCategory.OVERHEAD_PRESS, 160.0, 30, Sex.MALE)
        val female = StrengthStandards.thresholds(ScoringCategory.OVERHEAD_PRESS, 160.0, 30, Sex.FEMALE)
        assertEquals(true, female[2] < male[2])
    }

    @Test
    fun `level assignment interpolates between thresholds`() {
        // 220lb male, Back Squat: Intermediate=300, Proficient=375
        val score = StrengthStandards.score(
            oneRepMaxLb = 337.5,
            category = ScoringCategory.BACK_SQUAT,
            bodyweightLb = 220.0, age = 36, sex = Sex.MALE,
        )
        assertEquals(StrengthLevel.INTERMEDIATE, score.level)
        assertEquals(0.5, score.progressToNext, 0.01)
    }

    @Test
    fun `below the first threshold is untrained with fractional progress`() {
        val score = StrengthStandards.score(
            oneRepMaxLb = 75.0,
            category = ScoringCategory.BACK_SQUAT,
            bodyweightLb = 220.0, age = 36, sex = Sex.MALE,
        )
        assertEquals(StrengthLevel.UNTRAINED, score.level)
        // 75 of the 150lb Untrained threshold
        assertEquals(0.5, score.progressToNext, 1e-9)
    }

    @Test
    fun `at or above the top threshold is world class and capped`() {
        val score = StrengthStandards.score(
            oneRepMaxLb = 700.0,
            category = ScoringCategory.BACK_SQUAT,
            bodyweightLb = 220.0, age = 36, sex = Sex.MALE,
        )
        assertEquals(StrengthLevel.WORLD_CLASS, score.level)
        assertEquals(1.0, score.progressToNext, 1e-9)
    }
}
