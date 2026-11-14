package com.simplejim.tracker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutDraftReplacementTest {
    @Test
    fun emptyOrWhitespaceOnlyDraftDoesNotNeedConfirmation() {
        val blank = DraftExercise(1)
        val whitespace = DraftExercise(2, "  ", listOf(DraftSet(" ", ""), DraftSet()))
        assertFalse(WorkoutDraft("", emptyList()).hasMeaningfulInput())
        assertFalse(WorkoutDraft("   \n", listOf(blank, whitespace)).hasMeaningfulInput())
    }

    @Test
    fun aNamedExerciseWithoutSetsCountsAsInput() {
        assertTrue(WorkoutDraft("", listOf(DraftExercise(1, "Squat"))).hasMeaningfulInput())
    }

    @Test
    fun notesWithoutExercisesCountAsInput() {
        assertTrue(WorkoutDraft("Keep this note", listOf(DraftExercise(1))).hasMeaningfulInput())
    }

    @Test
    fun partialWeightAndRepsCountEvenIfTheExerciseIsUnnamed() {
        for (partial in listOf(DraftSet(".", ""), DraftSet("0", ""), DraftSet("", "5"))) {
            val draft = WorkoutDraft("", listOf(DraftExercise(1), DraftExercise(2, sets = listOf(partial))))
            assertTrue(draft.hasMeaningfulInput())
        }
    }
}
