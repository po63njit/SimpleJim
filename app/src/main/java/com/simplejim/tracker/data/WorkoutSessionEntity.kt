package com.simplejim.tracker.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "workout_sessions")
data class WorkoutSessionEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "performed_at")
    val performedAt: Long,
    val notes: String,
    @ColumnInfo(name = "exercises_json")
    val exercisesJson: String,
)

fun WorkoutSessionEntity.toDomainModel(): WorkoutSession {
    return WorkoutSession(
        id = id,
        performedAt = performedAt,
        notes = notes,
        exercises = exercisesFromJson(exercisesJson),
    )
}

fun WorkoutSession.toEntity(): WorkoutSessionEntity {
    return WorkoutSessionEntity(
        id = id,
        performedAt = performedAt,
        notes = notes,
        exercisesJson = exercises.toJsonString(),
    )
}