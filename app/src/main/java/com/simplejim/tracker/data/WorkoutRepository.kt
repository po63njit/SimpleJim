package com.simplejim.tracker.data

import android.content.Context

class WorkoutRepository(context: Context) {
    private val workoutDao = WorkoutDatabase.getInstance(context).workoutDao()

    suspend fun loadWorkouts(): List<WorkoutSession> {
        return workoutDao.getAllSessions().map { entity -> entity.toDomainModel() }
    }

    suspend fun saveWorkout(workout: WorkoutSession) {
        workoutDao.insertSession(workout.toEntity())
    }
}
