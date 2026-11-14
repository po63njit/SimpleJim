package com.simplejim.tracker

import com.simplejim.tracker.data.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Test

class DraftSetTest {
    @Test
    fun copyAppendsTheImmediateLastSetAndPreservesOriginal() {
        val previous = DraftSet("95", "10")
        val last = DraftSet("102.50", "8")
        val original = listOf(previous, last)

        val copied = original.withCopiedLastSet()

        assertEquals(listOf(previous, last, last), copied)
        assertNotSame(last, copied.last())
        assertEquals(listOf(previous, last), original)
    }

    @Test
    fun emptyOrIncompleteLastSetIsNotCopied() {
        val empty = emptyList<DraftSet>()
        val incomplete = listOf(DraftSet("95", "10"), DraftSet("100", ""))

        assertEquals(empty, empty.withCopiedLastSet())
        assertEquals(incomplete, incomplete.withCopiedLastSet())
    }

    @Test
    fun zeroWeightIsValidAndCanBeCopied() {
        val bodyweight = DraftSet("0", "12")

        assertEquals(WorkoutSet(0.0, 12), bodyweight.toWorkoutSetOrNull())
        assertEquals(listOf(bodyweight, bodyweight), listOf(bodyweight).withCopiedLastSet())
    }

    @Test
    fun invalidValuesCannotBeUsedAsARepeatableSet() {
        val invalidSets = listOf(
            DraftSet(".", "10"),
            DraftSet("95", "0"),
            DraftSet("-1", "10"),
            DraftSet("Infinity", "10"),
            DraftSet("NaN", "10"),
            DraftSet("95", "2147483648"),
        )
        for (set in invalidSets) {
            assertNull(set.toWorkoutSetOrNull())
            assertEquals(listOf(set), listOf(set).withCopiedLastSet())
        }
    }
}
