package com.simplejim.tracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkoutDraftJsonTest {
    @Test
    fun rawInputsAndIdsSurviveTwoWayRestore() {
        val draft = WorkoutDraft(
            notes = "Light day – warm up \n on the bike",
            exercises = listOf(
                DraftExercise(19, "Squat", listOf(DraftSet("102.", ""), DraftSet("0", "08"))),
                DraftExercise(24, "", listOf(DraftSet())),
            ),
        )
        val encoded = WorkoutDraftJson.encode(draft)
        assertEquals(draft, WorkoutDraftJson.decode(encoded))
        assertEquals(encoded, WorkoutDraftJson.encode(WorkoutDraftJson.decode(encoded)))
    }

    @Test
    fun unreadableFutureOrMalformedPayloadsAreRejected() {
        val valid = WorkoutDraftJson.encode(WorkoutDraft("", listOf(DraftExercise(19))))
        for (invalid in listOf("not json", valid.replace("\"version\":1", "\"version\":2"), valid.replace("\"id\":19", "\"id\":0"))) {
            assertTrue(runCatching { WorkoutDraftJson.decode(invalid) }.isFailure)
        }
    }
}
