package com.simplejim.tracker.cardio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardioInputTest {
    @Test fun treadmillRetainsUnitAndUsesWholeSessionPace() {
        val entry = (CardioInput.treadmill("26:15", "2.500", DistanceUnit.MILE, "3.5")
            as CardioInputResult.Valid).entry
        assertEquals(1575L, entry.durationSeconds)
        assertEquals(2500L, entry.distance.thousandths)
        assertEquals(4_023_360L, entry.distance.millimeters())
        assertEquals(630L, entry.averagePaceSecondsPerEnteredUnit())
        assertEquals(3500L, entry.inclineThousandthsOfPercent)
    }

    @Test fun kilometersAndDeclineAreNotRelabeledOrClamped() {
        val entry = (CardioInput.treadmill("1:05:00", "6,25", DistanceUnit.KILOMETER, "-1.5")
            as CardioInputResult.Valid).entry
        assertEquals(3900L, entry.durationSeconds)
        assertEquals(DistanceUnit.KILOMETER, entry.distance.unit)
        assertEquals(6_250_000L, entry.distance.millimeters())
        assertEquals(624L, entry.averagePaceSecondsPerEnteredUnit())
        assertEquals(-1500L, entry.inclineThousandthsOfPercent)
    }

    @Test fun stairsKeepDisplayedLevelSeparateFromFloors() {
        val entry = (CardioInput.stairs("15:30", "70.5", "7") as CardioInputResult.Valid).entry
        assertEquals(70_500L, entry.floorThousandths)
        assertEquals(7_000L, entry.displayedLevelThousandths)
        assertEquals("4.5", entry.averageFloorsPerMinute().toPlainString())
    }

    @Test fun omittedMetadataStaysNullAndLongMinuteTimersWork() {
        val stairs = (CardioInput.stairs("75:00", "100") as CardioInputResult.Valid).entry
        val treadmill = (CardioInput.treadmill("30:00", "2", DistanceUnit.MILE)
            as CardioInputResult.Valid).entry
        assertEquals(4500L, stairs.durationSeconds)
        assertNull(stairs.displayedLevelThousandths)
        assertNull(treadmill.inclineThousandthsOfPercent)
    }

    @Test fun rejectionIsFieldSpecificAndReportsMultipleIssues() {
        assertEquals(
            CardioInputResult.Invalid(listOf(
                CardioValidationIssue(CardioField.DURATION, CardioError.OUT_OF_RANGE),
                CardioValidationIssue(CardioField.DISTANCE, CardioError.REQUIRED),
                CardioValidationIssue(CardioField.INCLINE, CardioError.INVALID_FORMAT),
            )),
            CardioInput.treadmill("10:60", "", DistanceUnit.MILE, "NaN"),
        )
        assertEquals(
            CardioInputResult.Invalid(listOf(
                CardioValidationIssue(CardioField.DURATION, CardioError.OUT_OF_RANGE),
                CardioValidationIssue(CardioField.FLOORS, CardioError.OUT_OF_RANGE),
                CardioValidationIssue(CardioField.LEVEL, CardioError.OUT_OF_RANGE),
            )),
            CardioInput.stairs("00:00", "0", "-1"),
        )
    }

    @Test fun rejectsAmbiguousPrecisionGroupingAndMalformedTimes() {
        for (value in listOf("1.0001", "1,000.2", "1e3", "1000000000", "Infinity")) {
            val invalid = CardioInput.stairs("08:00", value) as CardioInputResult.Invalid
            assertEquals(listOf(CardioValidationIssue(CardioField.FLOORS, CardioError.INVALID_FORMAT)), invalid.issues)
        }
        for (value in listOf("20", "1:2", ":30", "1:60:00")) {
            val invalid = CardioInput.stairs(value, "1") as CardioInputResult.Invalid
            assertEquals(CardioField.DURATION, invalid.issues.single().field)
        }
    }
}
