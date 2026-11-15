package com.simplejim.tracker.data

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CardioMigrationTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun upgradeRetainsStrengthRecordsAndPersistsBothTypesOfCardio() = runBlocking<Unit> {
        val name = "room-cardio-migration-" + UUID.randomUUID()
        val path = context.getDatabasePath(name)
        path.parentFile?.mkdirs()
        try {
            SQLiteDatabase.openOrCreateDatabase(path, null).use { database ->
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS workout_sessions (id INTEGER NOT NULL PRIMARY KEY, " +
                        "performed_at INTEGER NOT NULL, notes TEXT NOT NULL, exercises_json TEXT NOT NULL)",
                )
                database.execSQL(
                    "CREATE TABLE IF NOT EXISTS workout_draft (id INTEGER NOT NULL PRIMARY KEY, " +
                        "revision INTEGER NOT NULL, payload_json TEXT)",
                )
                database.execSQL("INSERT INTO workout_sessions VALUES (9, 123, 'synthetic', '[]')")
                database.execSQL("INSERT INTO workout_draft VALUES (1, 5, 'synthetic draft')")
                database.version = 2
            }
            val treadmill = CardioEntryEntity(
                "treadmill", 101, "TREADMILL", 1575, distanceThousandths = 2500,
                distanceUnit = "MILE", inclineThousandths = -1000,
            )
            val stairs = CardioEntryEntity("stairs", 102, "STAIRS", 930, floorThousandths = 70500, levelThousandths = 7000)
            open(name).apply {
                try {
                    assertEquals(9L, workoutDao().getAllSessions().single().id)
                    assertEquals(5L, workoutDao().readDraft().revision)
                    assertEquals("synthetic draft", workoutDao().readDraft().payloadJson)
                    assertTrue(cardioDao().entries().isEmpty())
                    assertTrue(cardioDao().insert(treadmill) != -1L)
                    assertEquals(-1L, cardioDao().insert(treadmill))
                    assertEquals(treadmill, cardioDao().entry(treadmill.id))
                    cardioDao().insert(stairs)
                } finally {
                    close()
                }
            }
            val outdoorRun = CardioEntryEntity(
                "run", 103, "OUTDOOR_RUN", 1692, distanceThousandths = 4200, distanceUnit = "KILOMETER",
            )
            val outdoorWalk = CardioEntryEntity(
                "walk", 104, "OUTDOOR_WALK", 3750, distanceThousandths = 3750, distanceUnit = "MILE",
            )
            open(name).apply {
                try {
                    assertEquals(listOf(stairs, treadmill), cardioDao().entries())
                    cardioDao().insert(outdoorRun)
                    cardioDao().insert(outdoorWalk)
                    assertEquals("synthetic draft", workoutDao().readDraft().payloadJson)
                    assertEquals(9L, workoutDao().getAllSessions().single().id)
                } finally {
                    close()
                }
            }
            open(name).apply {
                try {
                    assertEquals(listOf(outdoorWalk, outdoorRun, stairs, treadmill), cardioDao().entries())
                } finally {
                    close()
                }
            }
        } finally {
            context.deleteDatabase(name)
        }
    }

    private fun open(name: String) = Room.databaseBuilder(context, WorkoutDatabase::class.java, name)
        .addMigrations(WorkoutDatabase.MIGRATION_1_2, WorkoutDatabase.MIGRATION_2_3)
        .build()
}
