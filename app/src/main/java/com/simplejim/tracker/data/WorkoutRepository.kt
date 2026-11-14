package com.simplejim.tracker.data

import android.content.Context
import androidx.room.withTransaction

internal class DraftRevisionConflict : IllegalStateException("Workout draft revision changed")

internal class WorkoutRepository(private val database: WorkoutDatabase) {
    constructor(context: Context) : this(WorkoutDatabase.getInstance(context))

    private val workoutDao = database.workoutDao()

    suspend fun loadWorkouts(): List<WorkoutSession> =
        workoutDao.getAllSessions().map { entity -> entity.toDomainModel() }

    suspend fun readDraft(): WorkoutDraftEntity = database.withTransaction {
        workoutDao.ensureDraftRow(WorkoutDraftEntity())
        workoutDao.readDraft()
    }

    suspend fun writeDraft(expectedRevision: Long, payload: String?): Long {
        if (workoutDao.updateDraft(expectedRevision, payload) != 1) throw DraftRevisionConflict()
        return expectedRevision + 1
    }

    suspend fun commitWorkout(workout: WorkoutSession, expectedRevision: Long): Long =
        database.withTransaction {
            workoutDao.insertSession(workout.toEntity())
            writeDraft(expectedRevision, null)
        }
}
