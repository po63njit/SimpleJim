package com.simplejim.tracker.data

import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.simplejim.tracker.DraftExercise
import com.simplejim.tracker.DraftSet
import com.simplejim.tracker.WorkoutDraft
import com.simplejim.tracker.WorkoutDraftJson
import com.simplejim.tracker.strength.LoadUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class StrengthHistoryRepositoryTest {
    @Test
    fun strengthReadUsesOnlyCommittedValidSetsAndLeavesDraftAlone() = runBlocking<Unit> {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val database = Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build()
        try {
            val dao = database.workoutDao()
            dao.insertSession(WorkoutSessionEntity(3, 300, "", "not JSON"))
            dao.insertSession(WorkoutSessionEntity(2, 200, "", """[{"name":"Squat","sets":[{"reps":7}]}]"""))
            dao.insertSession(WorkoutSessionEntity(1, 100, "", """[{"name":"Squat","sets":[{"weight":95,"reps":8}]}]"""))
            val repository = WorkoutRepository(database)
            val initial = repository.readDraft()
            val draft = WorkoutDraft("Not history", listOf(DraftExercise(1, "Deadlift", listOf(DraftSet("255", "3")))))
            val payload = WorkoutDraftJson.encode(draft)
            val revision = repository.writeDraft(initial.revision, payload)

            val result = repository.loadStrengthHistory()
            assertEquals(setOf("squat"), result.keys)
            val set = result.getValue("squat").sessions.single().heaviestActual
            assertEquals(1L, set.sessionId)
            assertEquals(LoadUnit.Pounds, set.load.unit)
            assertEquals(95.0, set.load.value, 0.0)
            assertEquals(revision, repository.readDraft().revision)
            assertEquals(payload, repository.readDraft().payloadJson)
            assertEquals(3, dao.getAllSessions().size)
        } finally {
            database.close()
        }
    }
}
