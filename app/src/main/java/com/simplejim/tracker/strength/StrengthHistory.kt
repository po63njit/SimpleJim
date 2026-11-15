package com.simplejim.tracker.strength

import java.util.Locale
import org.json.JSONArray
import org.json.JSONException

internal enum class LoadUnit {
    Pounds,
    Kilograms,
}

internal data class EnteredLoad(val value: Double, val unit: LoadUnit) {
    fun toKilograms(): Double = when (unit) {
        LoadUnit.Pounds -> value * KILOGRAMS_PER_POUND
        LoadUnit.Kilograms -> value
    }
}

/** Observed set data, retained with its entered load and unit. Never reconstructed from a draft. */
internal data class SavedStrengthSet(
    val sessionId: Long,
    val performedAt: Long,
    val exerciseName: String,
    val load: EnteredLoad,
    val reps: Int,
)

internal data class EstimatedOneRepMax(
    val kilograms: Double,
    val sourceSet: SavedStrengthSet,
)

internal data class StrengthSessionPoint(
    val sessionId: Long,
    val performedAt: Long,
    val heaviestActual: SavedStrengthSet,
    val heaviestSingle: SavedStrengthSet?,
    val bestEstimate: EstimatedOneRepMax?,
)

internal data class ExerciseStrengthHistory(
    val sessions: List<StrengthSessionPoint>,
    val heaviestActual: SavedStrengthSet,
    val heaviestSingle: SavedStrengthSet?,
)

private const val KILOGRAMS_PER_POUND = 0.45359237
private val repeatedWhitespace = Regex("\\s+")

/** Only casing and formatting whitespace are ignored. Different variant names remain distinct. */
internal fun strengthExerciseKey(name: String): String =
    name.trim().replace(repeatedWhitespace, " ").lowercase(Locale.ROOT)

internal fun calculateStrengthHistory(
    savedSets: Iterable<SavedStrengthSet>,
): Map<String, ExerciseStrengthHistory> = savedSets
    .filter(SavedStrengthSet::isValid)
    .sortedWith(compareBy<SavedStrengthSet>({ it.performedAt }, { it.sessionId }))
    .groupBy { strengthExerciseKey(it.exerciseName) }
    .mapValues { (_, exerciseSets) ->
        val sessions = exerciseSets.groupBy { it.sessionId to it.performedAt }.map { (key, sessionSets) ->
            StrengthSessionPoint(
                sessionId = key.first,
                performedAt = key.second,
                heaviestActual = requireNotNull(sessionSets.heaviest()),
                heaviestSingle = sessionSets.filter { it.reps == 1 }.heaviest(),
                bestEstimate = sessionSets.mapNotNull(SavedStrengthSet::estimate).maxByOrNull { it.kilograms },
            )
        }
        ExerciseStrengthHistory(
            sessions = sessions,
            heaviestActual = requireNotNull(exerciseSets.heaviest()),
            heaviestSingle = exerciseSets.filter { it.reps == 1 }.heaviest(),
        )
    }

/**
 * Adapter for the v1/v2 completed strength-session JSON. All legacy weights were entered in lb.
 * Read numeric fields directly rather than using the History decoder's display defaults for
 * missing data. This avoids treating an absent weight as a legitimate unweighted set.
 */
internal fun legacyStrengthSets(
    sessionId: Long,
    performedAt: Long,
    exercisesJson: String?,
): List<SavedStrengthSet> {
    if (exercisesJson == null || sessionId <= 0 || performedAt <= 0) return emptyList()
    val exercises = try {
        JSONArray(exercisesJson)
    } catch (_: JSONException) {
        return emptyList()
    }
    return buildList {
        for (exerciseIndex in 0 until exercises.length()) {
            val row = exercises.optJSONObject(exerciseIndex) ?: continue
            val name = row.opt("name") as? String ?: continue
            val sets = row.optJSONArray("sets") ?: continue
            for (setIndex in 0 until sets.length()) {
                val set = sets.optJSONObject(setIndex) ?: continue
                val weight = (set.opt("weight") as? Number)?.toDouble() ?: continue
                val reps = (set.opt("reps") as? Number)?.toDouble() ?: continue
                if (reps !in 1.0..Int.MAX_VALUE.toDouble() || reps % 1 != 0.0) continue
                val observation = SavedStrengthSet(
                    sessionId, performedAt, name, EnteredLoad(weight, LoadUnit.Pounds), reps.toInt(),
                )
                if (observation.isValid()) add(observation)
            }
        }
    }
}

private fun SavedStrengthSet.isValid(): Boolean = sessionId > 0 &&
    performedAt > 0 &&
    strengthExerciseKey(exerciseName).isNotEmpty() &&
    reps > 0 &&
    load.value.isFinite() &&
    load.value >= 0.0

private fun List<SavedStrengthSet>.heaviest(): SavedStrengthSet? = maxByOrNull { it.load.toKilograms() }

private fun SavedStrengthSet.estimate(): EstimatedOneRepMax? {
    // A logged single is measured, not estimated. Zero work and 11+ reps give no estimate.
    if (reps !in 2..10 || load.value <= 0.0) return null
    val kilograms = load.toKilograms() * (1.0 + reps / 30.0)
    return kilograms.takeIf { it.isFinite() && it > 0.0 }?.let { EstimatedOneRepMax(it, this) }
}
