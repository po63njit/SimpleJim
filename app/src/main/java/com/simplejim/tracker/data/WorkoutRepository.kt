package com.simplejim.tracker.data

import android.content.Context
import androidx.room.withTransaction
import com.simplejim.tracker.strength.ExerciseStrengthHistory
import com.simplejim.tracker.strength.calculateStrengthHistory
import com.simplejim.tracker.strength.legacyStrengthSets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class DraftRevisionConflict : IllegalStateException("Workout draft revision changed")

internal class WorkoutRepository(private val database: WorkoutDatabase) {
    constructor(context: Context) : this(WorkoutDatabase.getInstance(context))

    private val workoutDao = database.workoutDao()

    suspend fun loadWorkouts(): List<WorkoutSession> =
        workoutDao.getAllSessions().map { entity -> entity.toDomainModel() }

    // Keep this separate from the ordinary workout decoder, which has display defaults.
    suspend fun loadStrengthHistory(): Map<String, ExerciseStrengthHistory> {
        val completedSessions = workoutDao.getAllSessions()
        return withContext(Dispatchers.Default) {
            calculateStrengthHistory(completedSessions.flatMap { session ->
                legacyStrengthSets(session.id, session.performedAt, session.exercisesJson)
            })
        }
    }

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
