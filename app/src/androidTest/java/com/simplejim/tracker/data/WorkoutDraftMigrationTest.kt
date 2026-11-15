package com.simplejim.tracker.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.simplejim.tracker.DraftExercise
import com.simplejim.tracker.DraftSet
import com.simplejim.tracker.WorkoutDraft
import com.simplejim.tracker.WorkoutDraftJson
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class WorkoutDraftMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun migrationChainPreservesV1HistoryAndStoresIncompleteDraftAcrossOpen() = runBlocking<Unit> {
        val name = "room-draft-migration-" + UUID.randomUUID()
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        try {
            SQLiteDatabase.openOrCreateDatabase(path, null).use { database ->
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS workout_sessions (" +
                        "id INTEGER NOT NULL, performed_at INTEGER NOT NULL, " +
                        "notes TEXT NOT NULL, exercises_json TEXT NOT NULL, PRIMARY KEY(id))",
                )
                database.execSQL("INSERT INTO workout_sessions VALUES (1, 123, 'old notes', '[]')")
                database.version = 1
            }
            val draft = WorkoutDraft(
                notes = "warm up",
                exercises = listOf(DraftExercise(19, "Squat", listOf(DraftSet("102.", "")))),
            )
            open(name).useDb { database ->
                val repository = WorkoutRepository(database)
                assertEquals(WorkoutSession(1, 123, "old notes", emptyList()), repository.loadWorkouts().single())
                assertEquals(0L, repository.readDraft().revision)
                assertEquals(1L, repository.writeDraft(0, WorkoutDraftJson.encode(draft)))
            }
            open(name).useDb { database ->
                val repository = WorkoutRepository(database)
                assertEquals(draft, WorkoutDraftJson.decode(requireNotNull(repository.readDraft().payloadJson)))
                assertEquals(1, repository.loadWorkouts().size)
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    @Test
    fun saveAndResetNeverLetAStaleWriteRestoreClearedDraft() = runBlocking<Unit> {
        Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build().useDb { database ->
            val repository = WorkoutRepository(database)
            val first = repository.readDraft()
            val persisted = repository.writeDraft(first.revision, "draft")
            val committed = repository.commitWorkout(WorkoutSession(1, 123, "saved", emptyList()), persisted)
            assertNull(repository.readDraft().payloadJson)
            assertTrue(runCatching { repository.writeDraft(persisted, "stale") }.exceptionOrNull() is DraftRevisionConflict)
            assertTrue(runCatching {
                repository.commitWorkout(WorkoutSession(2, 124, "stale", emptyList()), persisted)
            }.exceptionOrNull() is DraftRevisionConflict)
            assertEquals(listOf(1L), repository.loadWorkouts().map { it.id })

            val revised = repository.writeDraft(committed, "new draft")
            val reset = repository.writeDraft(revised, null)
            assertEquals(reset, repository.readDraft().revision)
            assertNull(repository.readDraft().payloadJson)
            assertTrue(runCatching { repository.writeDraft(revised, "late") }.exceptionOrNull() is DraftRevisionConflict)
            assertEquals(1, repository.loadWorkouts().size)
        }
    }

    @Test
    fun failedSessionInsertDoesNotClearDraftOrModifyExistingWorkout() = runBlocking<Unit> {
        Room.inMemoryDatabaseBuilder(context, WorkoutDatabase::class.java).build().useDb { database ->
            val repository = WorkoutRepository(database)
            val original = WorkoutSession(1, 123, "first workout", emptyList())
            val initial = repository.readDraft()
            val saved = repository.commitWorkout(original, initial.revision)
            val revision = repository.writeDraft(saved, "keep this draft")
            assertTrue(runCatching { repository.commitWorkout(original.copy(notes = "replace"), revision) }.isFailure)
            assertEquals("keep this draft", repository.readDraft().payloadJson)
            assertEquals(revision, repository.readDraft().revision)
            assertEquals(original, repository.loadWorkouts().single())
        }
    }

    private inline fun <T> WorkoutDatabase.useDb(block: (WorkoutDatabase) -> T): T =
        try {
            block(this)
        } finally {
            close()
        }

    private fun open(name: String) = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
        .addMigrations(WorkoutDatabase.MIGRATION_1_2, WorkoutDatabase.MIGRATION_2_3)
        .build()
}
