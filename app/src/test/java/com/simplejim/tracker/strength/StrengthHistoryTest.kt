package com.simplejim.tracker.strength

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthHistoryTest {
    @Test
    fun actualLoadSingleAndEstimateRemainSeparate() {
        val single = set(load = 90.0, reps = 1)
        val highRepActual = set(load = 92.5, reps = 12)
        val estimateSource = set(load = 80.0, reps = 8)
        val otherEstimate = set(load = 82.5, reps = 5)
        val squat = calculateStrengthHistory(listOf(single, highRepActual, estimateSource, otherEstimate)).getValue("squat")
        val point = squat.sessions.single()

        assertEquals(highRepActual, squat.heaviestActual)
        assertEquals(single, squat.heaviestSingle)
        assertEquals(highRepActual, point.heaviestActual)
        assertEquals(single, point.heaviestSingle)
        val estimate = requireNotNull(point.bestEstimate)
        assertEquals(estimateSource, estimate.sourceSet)
        assertEquals(101.33333333333333, estimate.kilograms, 1e-9)
    }

    @Test
    fun normalizesLoadsButKeepsEnteredValuesAndUnits() {
        val pounds = set(load = 220.0, reps = 1, unit = LoadUnit.Pounds)
        val kilograms = set(load = 100.0, reps = 1)
        val squat = calculateStrengthHistory(listOf(pounds, kilograms)).getValue("squat")

        assertEquals(220.0, pounds.load.value, 0.0)
        assertEquals(LoadUnit.Pounds, pounds.load.unit)
        assertEquals(99.7903214, pounds.load.toKilograms(), 1e-9)
        assertEquals(kilograms, squat.heaviestActual)
        assertEquals(kilograms, squat.heaviestSingle)
        assertNull(squat.sessions.single().bestEstimate)
    }

    @Test
    fun differentlyNamedVariantsAreNotMerged() {
        val original = set(name = "  Bench  Press  ", load = 72.0)
        val capitalization = set(name = "bench press", load = 75.0, id = 2)
        val pause = set(name = "Paused Bench Press", load = 90.0)
        val machine = set(name = "Machine Bench Press", load = 100.0)
        val history = calculateStrengthHistory(listOf(original, capitalization, pause, machine))

        assertEquals(setOf("bench press", "paused bench press", "machine bench press"), history.keys)
        assertEquals(capitalization, history.getValue("bench press").heaviestActual)
        assertEquals(pause, history.getValue("paused bench press").heaviestActual)
        assertEquals("  Bench  Press  ", history.getValue("bench press").sessions.first().heaviestActual.exerciseName)
    }

    @Test
    fun sessionsAreChronologicalWithStableSameTimeOrder() {
        val entries = listOf(
            set(id = 4, time = 300, load = 90.0),
            set(id = 3, time = 100, load = 80.0),
            set(id = 2, time = 100, load = 80.0),
            set(id = 2, time = 100, load = 50.0),
            set(id = 1, time = 200, load = 70.0),
        )
        val points = calculateStrengthHistory(entries).getValue("squat").sessions
        assertEquals(listOf(2L, 3L, 1L, 4L), points.map { it.sessionId })
        assertEquals(listOf(100L, 100L, 200L, 300L), points.map { it.performedAt })
        assertEquals(80.0, points.first().heaviestActual.load.value, 0.0)
    }

    @Test
    fun exactTiesKeepTheEarliestSessionAndFirstEqualSourceSet() {
        val later = set(id = 5, time = 200, load = 90.0, reps = 1)
        val firstSingle = set(id = 2, time = 100, name = "Squat", load = 90.0, reps = 1)
        val equalSingle = firstSingle.copy(exerciseName = "SQUAT")
        val firstEstimate = set(id = 2, time = 100, name = "Squat", load = 80.0, reps = 8)
        val equalEstimate = firstEstimate.copy(exerciseName = "  squat  ")
        val squat = calculateStrengthHistory(
            listOf(later, firstSingle, equalSingle, firstEstimate, equalEstimate),
        ).getValue("squat")

        assertEquals(firstSingle, squat.heaviestActual)
        assertEquals(firstSingle, squat.heaviestSingle)
        assertEquals(firstSingle, squat.sessions.first().heaviestActual)
        assertEquals(firstSingle, squat.sessions.first().heaviestSingle)
        assertEquals(firstEstimate, requireNotNull(squat.sessions.first().bestEstimate).sourceSet)
    }

    @Test
    fun filteredEstimatesDoNotInventSinglesOrUnweightedProgress() {
        val entries = listOf(
            set(id = 1, load = 0.0, reps = 5),
            set(id = 2, load = 80.0, reps = 11),
            set(id = 3, load = 80.0, reps = 2),
            set(id = 4, load = 80.0, reps = 10),
            set(id = 5, load = Double.MAX_VALUE, reps = 3),
        )
        val squat = calculateStrengthHistory(entries).getValue("squat")
        val points = squat.sessions.associateBy { it.sessionId }
        assertNull(squat.heaviestSingle)
        for (id in listOf(1L, 2L, 5L)) assertNull(points.getValue(id).bestEstimate)
        val zeroLoad = points.getValue(1L).heaviestActual.load.value
        assertEquals(0.0, zeroLoad, 0.0)
        assertEquals(80.0 * (1 + 2 / 30.0), requireNotNull(points.getValue(3).bestEstimate).kilograms, 1e-9)
        assertEquals(80.0 * (1 + 10 / 30.0), requireNotNull(points.getValue(4).bestEstimate).kilograms, 1e-9)
    }

    @Test
    fun emptyAndInvalidTypedHistoryYieldNoChartPoints() {
        assertTrue(calculateStrengthHistory(emptyList()).isEmpty())
        assertTrue(calculateStrengthHistory(listOf(
            set(name = " "),
            set(id = 0),
            set(time = 0),
            set(load = -4.0),
            set(reps = 0),
            set(load = Double.POSITIVE_INFINITY),
            set(load = Double.NaN),
        )).isEmpty())
    }

    @Test
    fun legacyImportSkipsMissingAndCorruptRecordsWithoutReplacingWeightWithZero() {
        val legacy = """
            [
              {"name":"Squat","sets":[
                {"weight":135.5,"reps":7},
                {"reps":4},
                {"weight":55},
                {"weight":"100","reps":4},
                {"weight":100,"reps":3.5},
                {"weight":100,"reps":2147483648},
                {"weight":-4,"reps":4},
                {"weight":0,"reps":10},
                null
              ]},
              {"name":"","sets":[{"weight":123,"reps":5}]},
              {"name":"Bench","sets":null},
              false
            ]
        """.trimIndent()
        val sets = legacyStrengthSets(10, 500, legacy)
        assertEquals(listOf(135.5, 0.0), sets.map { it.load.value })
        assertEquals(listOf(7, 10), sets.map { it.reps })
        assertTrue(sets.all { it.load.unit == LoadUnit.Pounds && it.sessionId == 10L && it.performedAt == 500L })
        assertFalse(calculateStrengthHistory(sets).containsKey("bench"))
    }

    @Test
    fun legacyImportFailsClosedForInvalidContainersAndDates() {
        for (bad in listOf(null, "", "{", "{}", "[]")) assertTrue(legacyStrengthSets(1, 1, bad).isEmpty())
        assertTrue(legacyStrengthSets(0, 1, "[{}]").isEmpty())
        assertTrue(legacyStrengthSets(1, -1, "[{}]").isEmpty())
    }

    private fun set(
        name: String = "Squat",
        load: Double = 80.0,
        reps: Int = 5,
        id: Long = 1,
        time: Long = 100,
        unit: LoadUnit = LoadUnit.Kilograms,
    ) = SavedStrengthSet(id, time, name, EnteredLoad(load, unit), reps)
}
