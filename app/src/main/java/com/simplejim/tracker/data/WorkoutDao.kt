package com.simplejim.tracker.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
internal interface WorkoutDao {
    @Query("SELECT * FROM workout_sessions ORDER BY performed_at DESC")
    suspend fun getAllSessions(): List<WorkoutSessionEntity>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertSession(session: WorkoutSessionEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun ensureDraftRow(draft: WorkoutDraftEntity)

    @Query("SELECT * FROM workout_draft WHERE id = 1")
    suspend fun readDraft(): WorkoutDraftEntity

    @Query(
        "UPDATE workout_draft SET revision = revision + 1, payload_json = :payload " +
            "WHERE id = 1 AND revision = :expectedRevision",
    )
    suspend fun updateDraft(expectedRevision: Long, payload: String?): Int
}
