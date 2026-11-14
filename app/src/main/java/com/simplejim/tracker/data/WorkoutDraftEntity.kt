package com.simplejim.tracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_draft")
internal data class WorkoutDraftEntity(
    @PrimaryKey val id: Int = 1,
    val revision: Long = 0,
    @ColumnInfo(name = "payload_json") val payloadJson: String? = null,
)
