package com.simplejim.tracker

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.simplejim.tracker.data.DraftRevisionConflict
import com.simplejim.tracker.data.WorkoutExercise
import com.simplejim.tracker.data.WorkoutRepository
import com.simplejim.tracker.data.WorkoutSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch

private val DEFAULT_PRESET_EXERCISES = listOf(
    "Bench Press",
    "Squat",
    "Deadlift",
    "Overhead Press",
    "Barbell Row",
)

enum class SimpleJimTab {
    Track,
    History,
}

enum class DraftReplacementAction {
    Reset,
    Repeat,
}

data class SimpleJimUiState(
    val selectedTab: SimpleJimTab = SimpleJimTab.Track,
    val draftNotes: String = "",
    val draftExercises: List<DraftExercise> = emptyList(),
    val history: List<WorkoutSession> = emptyList(),
    val validationMessage: String? = null,
    val lastSavedAt: Long? = null,
    val draftReady: Boolean = false,
    val draftBusy: Boolean = true,
    val pendingDraftWrites: Int = 0,
    val draftStorageError: String? = null,
    val canDiscardUnreadableDraft: Boolean = false,
    val pendingDraftReplacement: DraftReplacementAction? = null,
    val quickPresets: List<String> = DEFAULT_PRESET_EXERCISES,
)

class SimpleJimViewModel(application: Application) : AndroidViewModel(application) {
    private var idCounter = 1L
    private val repository = WorkoutRepository(application)
    private var draftRevision: Long? = null
    private val draftCommands = Channel<DraftCommand>(Channel.UNLIMITED)

    private sealed interface DraftCommand {
        data object Restore : DraftCommand
        data class Persist(val draft: WorkoutDraft) : DraftCommand
        data object Reset : DraftCommand
        data class Save(val workout: WorkoutSession) : DraftCommand
    }

    var uiState by mutableStateOf(
        SimpleJimUiState(
            draftExercises = listOf(newDraftExercise()),
        ),
    )
        private set

    init {
        viewModelScope.launch {
            for (command in draftCommands) {
                try {
                    when (command) {
                        DraftCommand.Restore -> restoreDraft()
                        is DraftCommand.Persist -> {
                            draftRevision = repository.writeDraft(
                                requireNotNull(draftRevision),
                                WorkoutDraftJson.encode(command.draft),
                            )
                            uiState = uiState.copy(draftStorageError = null)
                        }
                        DraftCommand.Reset -> {
                            draftRevision = repository.writeDraft(requireNotNull(draftRevision), null)
                            uiState = uiState.copy(
                                draftReady = true,
                                draftBusy = false,
                                draftStorageError = null,
                                canDiscardUnreadableDraft = false,
                                draftNotes = "",
                                draftExercises = listOf(newDraftExercise()),
                                validationMessage = null,
                            )
                        }
                        is DraftCommand.Save -> {
                            draftRevision = repository.commitWorkout(command.workout, requireNotNull(draftRevision))
                            uiState = uiState.copy(
                                draftBusy = false,
                                draftStorageError = null,
                                selectedTab = SimpleJimTab.History,
                                draftNotes = "",
                                draftExercises = listOf(newDraftExercise()),
                                history = (listOf(command.workout) + uiState.history).sortedByDescending { it.performedAt },
                                validationMessage = null,
                                lastSavedAt = command.workout.performedAt,
                            )
                        }
                    }
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    // A pending save/reset owns the busy flag even when an earlier draft edit fails.
                    val failedPersist = command is DraftCommand.Persist
                    uiState = uiState.copy(
                        draftBusy = if (failedPersist) uiState.draftBusy else false,
                        draftStorageError = if (error is DraftRevisionConflict) {
                            "Another editor changed this draft. These edits were not stored."
                        } else when (command) {
                            DraftCommand.Restore -> "Could not load the draft. Nothing was overwritten."
                            DraftCommand.Reset -> "Could not clear the draft. No workout history was changed."
                            is DraftCommand.Save -> "Could not save this workout. Your draft is still here."
                            is DraftCommand.Persist -> "Could not store this draft yet. Keep the app open and retry."
                        },
                    )
                } finally {
                    if (command is DraftCommand.Persist) {
                        uiState = uiState.copy(pendingDraftWrites = uiState.pendingDraftWrites - 1)
                    }
                }
            }
        }
        draftCommands.trySend(DraftCommand.Restore)
    }

    private suspend fun restoreDraft() {
        val history = repository.loadWorkouts()
        uiState = uiState.copy(history = history)
        val stored = repository.readDraft()
        draftRevision = stored.revision
        val draft = try {
            stored.payloadJson?.let(WorkoutDraftJson::decode)
        } catch (_: Exception) {
            uiState = uiState.copy(
                draftBusy = false,
                draftStorageError = "This draft could not be read. It has not been overwritten.",
                canDiscardUnreadableDraft = true,
            )
            return
        }
        if (draft != null) idCounter = draft.exercises.maxOf { it.id } + 1
        uiState = uiState.copy(
            draftReady = true,
            draftBusy = false,
            draftStorageError = null,
            canDiscardUnreadableDraft = false,
            draftNotes = draft?.notes.orEmpty(),
            draftExercises = draft?.exercises ?: listOf(newDraftExercise()),
        )
    }

    fun retryDraftStorage() {
        if (uiState.draftBusy) return
        if (uiState.draftReady) {
            persistDraft(currentDraft())
        } else {
            uiState = uiState.copy(draftBusy = true, draftStorageError = null)
            draftCommands.trySend(DraftCommand.Restore)
        }
    }

    fun selectTab(tab: SimpleJimTab) {
        uiState = uiState.copy(selectedTab = tab)
    }

    fun updateNotes(notes: String) {
        editDraft { copy(draftNotes = notes) }
    }

    fun addExercise() {
        editDraft { copy(draftExercises = draftExercises + newDraftExercise()) }
    }

    fun addPresetExercise(name: String) {
        editDraft { copy(draftExercises = draftExercises + newDraftExercise(name = name)) }
    }

    fun removeExercise(exerciseId: Long) {
        editDraft {
            val remaining = draftExercises.filterNot { it.id == exerciseId }
            copy(draftExercises = remaining.ifEmpty { listOf(newDraftExercise()) })
        }
    }

    fun updateExerciseName(exerciseId: Long, name: String) {
        updateExercise(exerciseId) { exercise ->
            exercise.copy(name = name)
        }
    }

    fun addSet(exerciseId: Long) {
        updateExercise(exerciseId) { exercise ->
            exercise.copy(sets = exercise.sets + DraftSet())
        }
    }

    fun copyLastSet(exerciseId: Long) {
        updateExercise(exerciseId) { exercise ->
            exercise.copy(sets = exercise.sets.withCopiedLastSet())
        }
    }

    fun removeLastSet(exerciseId: Long) {
        updateExercise(exerciseId) { exercise ->
            val trimmedSets = if (exercise.sets.size > 1) exercise.sets.dropLast(1) else exercise.sets
            exercise.copy(sets = trimmedSets)
        }
    }

    fun updateSetWeight(exerciseId: Long, setIndex: Int, weight: String) {
        updateExercise(exerciseId) { exercise ->
            exercise.copy(
                sets = exercise.sets.mapIndexed { index, draftSet ->
                    if (index == setIndex) {
                        draftSet.copy(weight = sanitizeDecimal(weight))
                    } else {
                        draftSet
                    }
                },
            )
        }
    }

    fun updateSetReps(exerciseId: Long, setIndex: Int, reps: String) {
        updateExercise(exerciseId) { exercise ->
            exercise.copy(
                sets = exercise.sets.mapIndexed { index, draftSet ->
                    if (index == setIndex) {
                        draftSet.copy(reps = reps.filter(Char::isDigit))
                    } else {
                        draftSet
                    }
                },
            )
        }
    }

    fun requestDraftReset() = requestDraftReplacement(DraftReplacementAction.Reset)

    fun requestRepeatLastWorkout() = requestDraftReplacement(DraftReplacementAction.Repeat)

    fun cancelDraftReplacement() {
        uiState = uiState.copy(pendingDraftReplacement = null)
    }

    fun confirmDraftReplacement() {
        val action = uiState.pendingDraftReplacement ?: return
        uiState = uiState.copy(pendingDraftReplacement = null)
        if (canReplaceDraft(action)) performDraftReplacement(action)
    }

    private fun requestDraftReplacement(action: DraftReplacementAction) {
        if (uiState.pendingDraftReplacement != null || !canReplaceDraft(action)) return
        // An unreadable payload could contain meaningful input; never discard it silently.
        if (uiState.canDiscardUnreadableDraft || currentDraft().hasMeaningfulInput()) {
            uiState = uiState.copy(pendingDraftReplacement = action)
        } else {
            performDraftReplacement(action)
        }
    }

    private fun canReplaceDraft(action: DraftReplacementAction): Boolean {
        if (uiState.draftBusy) return false
        return when (action) {
            DraftReplacementAction.Reset -> uiState.draftReady || uiState.canDiscardUnreadableDraft
            DraftReplacementAction.Repeat -> uiState.draftReady && uiState.history.isNotEmpty()
        }
    }

    private fun performDraftReplacement(action: DraftReplacementAction) {
        when (action) {
            DraftReplacementAction.Reset -> {
                uiState = uiState.copy(draftBusy = true, draftStorageError = null)
                draftCommands.trySend(DraftCommand.Reset)
            }
            DraftReplacementAction.Repeat -> repeatLastWorkout()
        }
    }

    private fun repeatLastWorkout() {
        val lastSession = uiState.history.firstOrNull() ?: return
        val repeatedExercises = lastSession.exercises.map { exercise ->
            DraftExercise(
                id = nextId(),
                name = exercise.name,
                sets = exercise.sets.map { set ->
                    DraftSet(
                        weight = set.weight.toString(),
                        reps = set.reps.toString(),
                    )
                },
            )
        }
        editDraft {
            copy(
                selectedTab = SimpleJimTab.Track,
                draftNotes = "",
                draftExercises = repeatedExercises.ifEmpty { listOf(newDraftExercise()) },
            )
        }
    }

    fun saveWorkout() {
        if (!uiState.draftReady || uiState.draftBusy || uiState.pendingDraftReplacement != null) return
        val cleanedExercises = uiState.draftExercises.mapNotNull { draftExercise ->
            val name = draftExercise.name.trim()
            val sets = draftExercise.sets.mapNotNull(DraftSet::toWorkoutSetOrNull)

            if (name.isNotEmpty() && sets.isNotEmpty()) {
                WorkoutExercise(name = name, sets = sets)
            } else {
                null
            }
        }

        if (cleanedExercises.isEmpty()) {
            uiState = uiState.copy(
                validationMessage = "Add at least one named exercise with valid weight and reps before saving.",
            )
            return
        }

        val timestamp = System.currentTimeMillis()
        val session = WorkoutSession(
            id = timestamp,
            performedAt = timestamp,
            notes = uiState.draftNotes.trim(),
            exercises = cleanedExercises,
        )
        uiState = uiState.copy(draftBusy = true, draftStorageError = null, validationMessage = null)
        draftCommands.trySend(DraftCommand.Save(session))
    }

    private fun updateExercise(exerciseId: Long, transform: (DraftExercise) -> DraftExercise) {
        editDraft {
            copy(draftExercises = draftExercises.map { exercise ->
                if (exercise.id == exerciseId) transform(exercise) else exercise
            })
        }
    }

    private fun editDraft(update: SimpleJimUiState.() -> SimpleJimUiState) {
        if (!uiState.draftReady || uiState.draftBusy || uiState.pendingDraftReplacement != null) return
        val previous = currentDraft()
        uiState = uiState.update().copy(validationMessage = null)
        val next = currentDraft()
        if (next != previous) persistDraft(next)
    }

    private fun persistDraft(draft: WorkoutDraft) {
        uiState = uiState.copy(pendingDraftWrites = uiState.pendingDraftWrites + 1)
        draftCommands.trySend(DraftCommand.Persist(draft))
    }

    private fun currentDraft() = WorkoutDraft(uiState.draftNotes, uiState.draftExercises)

    private fun newDraftExercise(name: String = ""): DraftExercise {
        return DraftExercise(id = nextId(), name = name)
    }

    private fun nextId(): Long {
        val currentId = idCounter
        idCounter += 1
        return currentId
    }

    private fun sanitizeDecimal(input: String): String {
        val filtered = input.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) {
            return filtered
        }

        var dotSeen = false
        return buildString {
            filtered.forEach { character ->
                if (character == '.') {
                    if (!dotSeen) {
                        dotSeen = true
                        append(character)
                    }
                } else {
                    append(character)
                }
            }
        }
    }

    companion object {
        fun factory(application: Application): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SimpleJimViewModel(application) as T
            }
        }
    }
}
