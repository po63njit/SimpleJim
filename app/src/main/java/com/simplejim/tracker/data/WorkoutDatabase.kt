package com.simplejim.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WorkoutSessionEntity::class, WorkoutDraftEntity::class, CardioEntryEntity::class],
    version = 3,
    exportSchema = false,
)
internal abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao
    abstract fun cardioDao(): CardioDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS workout_draft (" +
                        "id INTEGER NOT NULL PRIMARY KEY, revision INTEGER NOT NULL, payload_json TEXT)",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS cardio_entries (" +
                        "id TEXT NOT NULL PRIMARY KEY, recorded_at INTEGER NOT NULL, kind TEXT NOT NULL, " +
                        "duration_seconds INTEGER NOT NULL, distance_thousandths INTEGER, distance_unit TEXT, " +
                        "incline_thousandths INTEGER, floor_thousandths INTEGER, level_thousandths INTEGER)",
                )
            }
        }

        @Volatile
        private var instance: WorkoutDatabase? = null

        fun getInstance(context: Context): WorkoutDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "simple_jim.db",
                ).addMigrations(MIGRATION_1_2, MIGRATION_2_3).build().also { instance = it }
            }
        }
    }
}
