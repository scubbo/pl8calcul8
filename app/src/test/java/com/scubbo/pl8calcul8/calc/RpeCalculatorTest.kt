package com.scubbo.pl8calcul8.calc

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class RpeChartTest {
    @Test
    fun `single rep at RPE 10 is 100 percent of 1RM`() {
        assertEquals(1.000, RpeChart.percentage(reps = 1, rpe = 10.0), 1e-9)
    }

    @Test
    fun `known chart values`() {
        assertEquals(0.955, RpeChart.percentage(reps = 2, rpe = 10.0), 1e-9)
        assertEquals(0.811, RpeChart.percentage(reps = 5, rpe = 8.0), 1e-9)
        assertEquals(0.586, RpeChart.percentage(reps = 12, rpe = 6.5), 1e-9)
    }

    @Test
    fun `half-integer RPEs are supported`() {
        assertEquals(0.850, RpeChart.percentage(reps = 4, rpe = 8.5), 1e-9)
    }

    @Test
    fun `reps outside 1-12 are rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            RpeChart.percentage(reps = 0, rpe = 8.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RpeChart.percentage(reps = 13, rpe = 8.0)
        }
    }

    @Test
    fun `RPE outside 6_5-10 or off the half-step grid is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            RpeChart.percentage(reps = 5, rpe = 6.0)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RpeChart.percentage(reps = 5, rpe = 10.5)
        }
        assertThrows(IllegalArgumentException::class.java) {
            RpeChart.percentage(reps = 5, rpe = 7.3)
        }
    }
}

class RpeCalculatorTest {
    @Test
    fun `estimated 1RM from a completed exercise`() {
        // 200lb x5 @ RPE 8 -> 81.1% of 1RM -> e1RM = 200 / 0.811
        assertEquals(246.61, RpeCalculator.estimateOneRepMax(weightLb = 200.0, reps = 5, rpe = 8.0), 0.01)
    }

    @Test
    fun `target weight for an assigned reps and RPE`() {
        // e1RM 246.61, assigned 4@7 -> 81.1% -> back to ~200
        assertEquals(200.0, RpeCalculator.targetWeight(oneRepMaxLb = 246.61, reps = 4, rpe = 7.0), 0.01)
    }

    @Test
    fun `rounds to nearest 5lb`() {
        assertEquals(215.0, roundToNearest5TiesDown(213.0), 1e-9)
        assertEquals(210.0, roundToNearest5TiesDown(211.0), 1e-9)
        assertEquals(210.0, roundToNearest5TiesDown(212.0), 1e-9)
        assertEquals(210.0, roundToNearest5TiesDown(210.0), 1e-9)
    }

    @Test
    fun `exact midpoints round down`() {
        assertEquals(210.0, roundToNearest5TiesDown(212.5), 1e-9)
        assertEquals(205.0, roundToNearest5TiesDown(207.5), 1e-9)
    }

    @Test
    fun `advised weight applies ratio then increment then rounding`() {
        // Last exercise: 200lb 5@8 -> e1RM 246.61
        // New assignment: 4@7 -> target 200.0; +5 increment -> 205; round -> 205
        val advised = RpeCalculator.adviseWeight(
            previousWeightLb = 200.0,
            previousReps = 5,
            previousRpe = 8.0,
            assignedReps = 4,
            assignedRpe = 7.0,
            incrementLb = 5.0,
        )
        assertEquals(205.0, advised, 1e-9)
    }

    @Test
    fun `advised weight matches spec example`() {
        // Spec: ratio calculation comes out to 202.5lb, increment 10lb -> 212.5 -> 210
        // Construct: previous 202.5lb at 4@7, new assignment 4@7 (same) -> target 202.5
        val advised = RpeCalculator.adviseWeight(
            previousWeightLb = 202.5,
            previousReps = 4,
            previousRpe = 7.0,
            assignedReps = 4,
            assignedRpe = 7.0,
            incrementLb = 10.0,
        )
        assertEquals(210.0, advised, 1e-9)
    }
}
