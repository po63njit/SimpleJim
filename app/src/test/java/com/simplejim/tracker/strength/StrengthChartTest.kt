package com.simplejim.tracker.strength

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthChartTest {
    private fun set(id: Int, reps: Int, amount: Double = 100.0, unit: LoadUnit = LoadUnit.Kilograms) =
        SavedStrengthSet(id.toLong(), id.toLong(), "Squat", EnteredLoad(amount, unit), reps)

    private fun chart(vararg sets: SavedStrengthSet) = strengthChart(
        calculateStrengthHistory(sets.toList()).getValue("squat"),
    )

    @Test fun preservesMissingEstimateSlotAndOriginalMixedUnitSource() {
        val pounds = set(3, 5, 200.0, LoadUnit.Pounds)
        val result = chart(pounds, set(1, 5), set(2, 1))
        assertEquals(1..3, result.ordinals)
        assertEquals(listOf(1, 2, 3), result.observed.map { it.ordinal })
        assertEquals(listOf(1, 3), result.estimated.map { it.ordinal })
        assertEquals(.5f, result.xFraction(2))
        assertSame(pounds, result.estimated.last().sourceSet)
        assertEquals(90.718474, result.observed.last().kilograms, 1e-9)
        assertEquals(0f, result.yFraction(result.maximumKg))
    }

    @Test fun usesRecentWindowWithStableOrdinalsAndSharedZeroScale() {
        val result = chart(*(1..14).map { set(it, 3, 0.0) }.toTypedArray())
        assertEquals(3..14, result.ordinals)
        assertEquals(12, result.observed.size)
        assertTrue(result.estimated.isEmpty())
        assertEquals(0f, result.xFraction(3))
        assertEquals(1f, result.xFraction(14))
        assertEquals(1f, result.yFraction(0.0))
    }

    @Test fun equalTimestampsKeepDistinctSlotsOrderedBySessionId() {
        val earlierId = set(1, 1).copy(performedAt = 50)
        val laterId = set(2, 5).copy(performedAt = 50)
        val result = chart(laterId, earlierId)
        assertEquals(listOf(earlierId, laterId), result.observed.map { it.sourceSet })
        assertEquals(listOf(2), result.estimated.map { it.ordinal })
        assertEquals(0f, result.xFraction(1))
        assertEquals(1f, result.xFraction(2))
    }

    @Test fun centersSingleZeroOnTheBaselineWithoutInventingAnEstimate() {
        val result = chart(set(1, 5, 0.0))
        assertEquals(.5f, result.xFraction(result.observed.single().ordinal))
        assertEquals(0.0, result.maximumKg, 0.0)
        assertEquals(1f, result.yFraction(result.observed.single().kilograms))
        assertTrue(result.estimated.isEmpty())
    }

    @Test fun centersOneSessionAndKeepsDifferentObservedAndEstimateSources() {
        val heavy = set(1, 1, 100.0)
        val estimate = set(1, 8, 90.0)
        val result = chart(heavy, estimate)
        assertEquals(.5f, result.xFraction(1))
        assertSame(heavy, result.observed.single().sourceSet)
        assertSame(estimate, result.estimated.single().sourceSet)
        assertTrue(result.maximumKg > 100.0)
    }
}
