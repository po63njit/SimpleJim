package com.simplejim.tracker.cardio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OutdoorCardioTest {
    @Test fun runAndWalkHoldDistinctKindsAndDoNotInheritMachineData() {
        val draft = CardioDraft(
            duration = "38:45", distance = "5.000", unit = DistanceUnit.KILOMETER,
            incline = "8", floors = "50", level = "9",
        )
        val run = (outdoorRecord(CardioKind.OUTDOOR_RUN, draft) as CardioInputResult.Valid).entry
        val walk = (outdoorRecord(CardioKind.OUTDOOR_WALK, draft) as CardioInputResult.Valid).entry

        assertEquals("OUTDOOR_RUN", run.kind)
        assertEquals(run.copy(kind = "OUTDOOR_WALK"), walk)
        assertEquals(2325L, run.durationSeconds)
        assertEquals(5000L, run.distanceThousandths)
        assertEquals("KILOMETER", run.distanceUnit)
        assertNull(run.inclineThousandths)
        assertNull(run.floorThousandths)
        assertNull(run.levelThousandths)
    }

    @Test fun wholeActivityPaceUsesEnteredUnitAndLongDuration() {
        val walk = (CardioInput.outdoor("1:02:30", "3.75", DistanceUnit.MILE)
            as CardioInputResult.Valid).entry
        val run = (CardioInput.outdoor("28:12", "4,20", DistanceUnit.KILOMETER)
            as CardioInputResult.Valid).entry

        assertEquals(1000L, walk.averagePaceSecondsPerEnteredUnit())
        assertEquals(DistanceUnit.MILE, walk.distance.unit)
        assertEquals(403L, run.averagePaceSecondsPerEnteredUnit())
        assertEquals(DistanceUnit.KILOMETER, run.distance.unit)
    }

    @Test fun invalidOutdoorDataYieldsFieldErrorsInsteadOfARecord() {
        val result = outdoorRecord(CardioKind.OUTDOOR_RUN, CardioDraft(duration = "2:76", distance = "0"))
        assertEquals(
            CardioInputResult.Invalid(listOf(
                CardioValidationIssue(CardioField.DURATION, CardioError.OUT_OF_RANGE),
                CardioValidationIssue(CardioField.DISTANCE, CardioError.OUT_OF_RANGE),
            )),
            result,
        )
    }
}
