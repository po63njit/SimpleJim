package com.simplejim.tracker.strength

import com.simplejim.tracker.StrengthHistoryState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrengthSnapshotGateTest {
    @Test
    fun saveDiscardsStaleReadAndWaitsUntilCompletionBeforeLoading() {
        val gate = StrengthSnapshotGate()
        val beforeSave = requireNotNull(gate.beginLoad())
        assertTrue(gate.isCurrent(beforeSave))

        gate.beginWorkoutSave()
        assertFalse(gate.isCurrent(beforeSave))
        assertNull(gate.beginLoad())
        gate.finishWorkoutSave()

        val afterSave = requireNotNull(gate.beginLoad())
        assertFalse(gate.isCurrent(beforeSave))
        assertTrue(gate.isCurrent(afterSave))
    }

    @Test
    fun reselectionOrCloseInvalidatesAnAbandonedRead() {
        val gate = StrengthSnapshotGate()
        val beforeRetry = requireNotNull(gate.beginLoad())
        val afterRetry = requireNotNull(gate.beginLoad())
        assertFalse(gate.isCurrent(beforeRetry))
        assertTrue(gate.isCurrent(afterRetry))

        gate.invalidate()
        assertFalse(gate.isCurrent(afterRetry))
    }

    @Test
    fun refreshingKeepsAnExerciseSelectionOnlyWhileItStillExists() {
        val squat = SavedStrengthSet(1, 100, "Squat", EnteredLoad(135.0, LoadUnit.Pounds), 8)
        val prior = StrengthHistoryState(
            isOpen = true,
            isLoading = true,
            exercises = calculateStrengthHistory(listOf(squat)),
            selectedKey = "squat",
        )
        val fresh = calculateStrengthHistory(listOf(squat, squat.copy(sessionId = 2, performedAt = 200)))
        val reloaded = prior.withLoadedExercises(fresh)
        assertEquals("squat", reloaded.selectedKey)
        assertEquals(2, reloaded.exercises.getValue("squat").sessions.size)
        assertFalse(reloaded.isLoading)
        assertNull(prior.withLoadedExercises(emptyMap()).selectedKey)
    }
}
