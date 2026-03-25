package com.simplejim.tracker.data

import org.json.JSONArray
import org.json.JSONObject

data class WorkoutSet(
    val weight: Double,
    val reps: Int,
)

data class WorkoutExercise(
    val name: String,
    val sets: List<WorkoutSet>,
)

data class WorkoutSession(
    val id: Long,
    val performedAt: Long,
    val notes: String,
    val exercises: List<WorkoutExercise>,
)

fun WorkoutExercise.toJsonObject(): JSONObject {
    val jsonObject = JSONObject()
    jsonObject.put("name", name)
    val setsArray = JSONArray()
    sets.forEach { set ->
        val setObject = JSONObject()
        setObject.put("weight", set.weight)
        setObject.put("reps", set.reps)
        setsArray.put(setObject)
    }
    jsonObject.put("sets", setsArray)
    return jsonObject
}

fun List<WorkoutExercise>.toJsonString(): String {
    val exercises = JSONArray()
    forEach { exercise ->
        exercises.put(exercise.toJsonObject())
    }
    return exercises.toString()
}

fun exercisesFromJson(rawJson: String?): List<WorkoutExercise> {
    if (rawJson == null) return emptyList()
    val exercises = JSONArray(rawJson)
    return buildList {
        for (exerciseIndex in 0 until exercises.length()) {
            val exerciseObject = exercises.optJSONObject(exerciseIndex) ?: continue
            val name = exerciseObject.optString("name", "")
            val setsArray = exerciseObject.optJSONArray("sets") ?: JSONArray()
            val sets = mutableListOf<WorkoutSet>()
            for (setIndex in 0 until setsArray.length()) {
                val setObject = setsArray.optJSONObject(setIndex) ?: continue
                sets.add(
                    WorkoutSet(
                        weight = setObject.optDouble("weight", 0.0),
                        reps = setObject.optInt("reps", 0),
                    ),
                )
            }
            add(WorkoutExercise(name = name, sets = sets))
        }
    }
}
