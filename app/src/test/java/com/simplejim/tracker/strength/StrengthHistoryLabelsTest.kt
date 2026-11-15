package com.simplejim.tracker.strength

import java.util.Locale
import java.util.TimeZone
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthHistoryLabelsTest {
    @Test
    fun sourceSetLabelShowsEnteredNumberUnitRepsAndLocalDate() {
        val time = 1_735_689_600_000L
        val entry = SavedStrengthSet(1, time, "Squat", EnteredLoad(135.5, LoadUnit.Pounds), 8)
        val utc = sourceSetLabel(entry, Locale.US, TimeZone.getTimeZone("UTC"))
        assertTrue(utc.startsWith("135.5 lb, 8 reps, Jan 1, 2025") && utc.contains("12:00:00"))
        val pacific = sourceSetLabel(entry.copy(reps = 1), Locale.US, TimeZone.getTimeZone("America/Los_Angeles"))
        assertTrue(pacific.startsWith("135.5 lb, 1 rep, Dec 31, 2024") && pacific.contains("4:00:00"))
    }

    @Test
    fun newestFirstKeepsChronologicalSessionNumbersAndSameDayTimes() {
        val midnight = 1_735_689_600_000L
        val one = SavedStrengthSet(1, midnight, "Squat", EnteredLoad(135.0, LoadUnit.Pounds), 8)
        val two = one.copy(sessionId = 2, performedAt = midnight + 3_600_000)
        val sameSecond = two.copy(sessionId = 3)
        val history = calculateStrengthHistory(listOf(two, sameSecond, one)).getValue("squat")

        assertEquals(listOf(1L, 2L, 3L), history.sessions.map { it.sessionId })
        assertEquals(listOf(1, 2, 3), strengthSessionRows(history, newestFirst = false).map { it.ordinal })
        val newest = strengthSessionRows(history, newestFirst = true)
        assertEquals(listOf(3L, 2L, 1L), newest.map { it.point.sessionId })
        assertEquals(listOf(3, 2, 1), newest.map { it.ordinal })
        val utc = TimeZone.getTimeZone("UTC")
        assertTrue(strengthDateTime(one.performedAt, Locale.US, utc).contains("12:00:00"))
        assertTrue(strengthDateTime(two.performedAt, Locale.US, utc).contains("1:00:00"))
    }

    @Test
    fun estimateUsesSourceUnitAndNeverFormatsPositiveLoadAsZero() {
        val entry = SavedStrengthSet(1, 100, "Squat", EnteredLoad(100.0, LoadUnit.Pounds), 6)
        val estimate = requireNotNull(calculateStrengthHistory(listOf(entry))["squat"]?.sessions?.single()?.bestEstimate)
        assertEquals("120 lb", estimateLabel(estimate, Locale.US))
        val tiny = estimate.copy(kilograms = 1e-10, sourceSet = entry.copy(load = EnteredLoad(1e-10, LoadUnit.Kilograms)))
        assertTrue(estimateLabel(tiny, Locale.US).startsWith("1.0E-10 "))
    }

    @Test
    fun estimateFallsBackToKilogramsIfDisplayConversionOverflows() {
        val source = SavedStrengthSet(1, 100, "Squat", EnteredLoad(Double.MAX_VALUE, LoadUnit.Pounds), 10)
        val estimate = requireNotNull(calculateStrengthHistory(listOf(source))["squat"]?.sessions?.single()?.bestEstimate)
        val label = estimateLabel(estimate, Locale.US)
        assertTrue(label.endsWith(" kg") && !label.contains("Infinity"))
    }
}
