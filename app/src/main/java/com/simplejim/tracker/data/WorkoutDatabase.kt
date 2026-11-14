package com.simplejim.tracker.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [WorkoutSessionEntity::class, WorkoutDraftEntity::class],
    version = 2,
    exportSchema = false,
)
internal abstract class WorkoutDatabase : RoomDatabase() {
    abstract fun workoutDao(): WorkoutDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS workout_draft (" +
                        "id INTEGER NOT NULL PRIMARY KEY, revision INTEGER NOT NULL, payload_json TEXT)",
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
                ).addMigrations(MIGRATION_1_2).build().also { instance = it }
            }
        }
    }
}
