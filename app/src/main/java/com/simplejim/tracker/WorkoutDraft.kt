package com.simplejim.tracker

import org.json.JSONArray
import org.json.JSONObject

data class DraftExercise(
    val id: Long,
    val name: String = "",
    val sets: List<DraftSet> = listOf(DraftSet()),
)

internal data class WorkoutDraft(val notes: String, val exercises: List<DraftExercise>) {
    fun hasMeaningfulInput(): Boolean = notes.isNotBlank() || exercises.any { exercise ->
        exercise.name.isNotBlank() || exercise.sets.any { it.weight.isNotBlank() || it.reps.isNotBlank() }
    }
}

internal object WorkoutDraftJson {
    private const val VERSION = 1

    fun encode(draft: WorkoutDraft): String = JSONObject().apply {
        put("version", VERSION)
        put("notes", draft.notes)
        put("exercises", JSONArray().apply {
            draft.exercises.forEach { exercise ->
                put(JSONObject().apply {
                    put("id", exercise.id)
                    put("name", exercise.name)
                    put("sets", JSONArray().apply {
                        exercise.sets.forEach { set ->
                            put(JSONObject().apply {
                                put("weight", set.weight)
                                put("reps", set.reps)
                            })
                        }
                    })
                })
            }
        })
    }.toString()

    fun decode(payload: String): WorkoutDraft {
        val json = JSONObject(payload)
        require(json.getInt("version") == VERSION) { "Unsupported workout draft version" }
        val rows = json.getJSONArray("exercises")
        val ids = mutableSetOf<Long>()
        val exercises = List(rows.length()) { index ->
            val row = rows.getJSONObject(index)
            val id = row.getLong("id")
            require(id in 1L until Long.MAX_VALUE - 1 && ids.add(id)) { "Invalid exercise ID" }
            val sets = row.getJSONArray("sets")
            require(sets.length() > 0) { "Missing workout set" }
            DraftExercise(
                id = id,
                name = readString(row, "name"),
                sets = List(sets.length()) { setIndex ->
                    val set = sets.getJSONObject(setIndex)
                    DraftSet(readString(set, "weight"), readString(set, "reps"))
                },
            )
        }
        require(exercises.isNotEmpty()) { "Missing workout exercise" }
        return WorkoutDraft(readString(json, "notes"), exercises)
    }

    private fun readString(row: JSONObject, key: String): String =
        row.get(key) as? String ?: throw IllegalArgumentException("Invalid draft text")
}
