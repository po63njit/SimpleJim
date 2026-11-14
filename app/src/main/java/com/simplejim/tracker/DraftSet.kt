package com.simplejim.tracker

import com.simplejim.tracker.data.WorkoutSet

data class DraftSet(
    val weight: String = "",
    val reps: String = "",
)

internal fun DraftSet.toWorkoutSetOrNull(): WorkoutSet? {
    val parsedWeight = weight.toDoubleOrNull() ?: return null
    val parsedReps = reps.toIntOrNull() ?: return null
    if (!parsedWeight.isFinite() || parsedWeight < 0 || parsedReps <= 0) return null
    return WorkoutSet(weight = parsedWeight, reps = parsedReps)
}

internal fun List<DraftSet>.withCopiedLastSet(): List<DraftSet> {
    val lastSet = lastOrNull() ?: return this
    return if (lastSet.toWorkoutSetOrNull() == null) this else this + lastSet.copy()
}
