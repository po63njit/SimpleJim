package com.simplejim.tracker.cardio

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simplejim.tracker.data.CardioEntryEntity
import com.simplejim.tracker.data.WorkoutDatabase
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

internal enum class CardioKind { TREADMILL, STAIRS, OUTDOOR_RUN, OUTDOOR_WALK }

internal data class CardioDraft(
    val duration: String = "",
    val distance: String = "",
    val unit: DistanceUnit = DistanceUnit.MILE,
    val incline: String = "",
    val floors: String = "",
    val level: String = "",
)

internal data class CardioState(
    val kind: CardioKind = CardioKind.TREADMILL,
    val fieldValues: Map<CardioKind, CardioDraft> = emptyMap(),
    val issues: List<CardioValidationIssue> = emptyList(),
    val entries: List<CardioEntryEntity> = emptyList(),
    val saving: Boolean = false,
    val message: String? = null,
    val historyUnavailable: Boolean = false,
) {
    val draft: CardioDraft get() = fieldValues[kind] ?: CardioDraft()
}

internal class CardioViewModel(application: Application) : AndroidViewModel(application) {
    private val dao by lazy { WorkoutDatabase.getInstance(application).cardioDao() }
    var state by mutableStateOf(CardioState())
        private set
    // A retry of unchanged input reuses the same record ID.
    private var pendingEntry: CardioEntryEntity? = null
    private var historyRequest = 0L

    fun select(kind: CardioKind) {
        if (state.saving || state.kind == kind) return
        state = state.copy(kind = kind, issues = emptyList(), message = null)
    }

    fun edit(change: (CardioDraft) -> CardioDraft) {
        if (state.saving) return
        state = state.copy(
            fieldValues = state.fieldValues + (state.kind to change(state.draft)),
            issues = emptyList(),
            message = null,
        )
    }

    fun refresh() {
        viewModelScope.launch { loadEntries() }
    }

    fun save() {
        if (state.saving) return
        val kind = state.kind
        val draft = state.draft
        val entry = when (kind) {
            CardioKind.TREADMILL -> when (
                val parsed = CardioInput.treadmill(draft.duration, draft.distance, draft.unit, draft.incline)
            ) {
                is CardioInputResult.Invalid -> return showIssues(parsed.issues)
                is CardioInputResult.Valid -> parsed.entry.let {
                    CardioEntryEntity(
                        "", 0, kind.name, it.durationSeconds,
                        distanceThousandths = it.distance.thousandths,
                        distanceUnit = it.distance.unit.name,
                        inclineThousandths = it.inclineThousandthsOfPercent,
                    )
                }
            }
            CardioKind.OUTDOOR_RUN, CardioKind.OUTDOOR_WALK -> when (
                val parsed = outdoorRecord(kind, draft)
            ) {
                is CardioInputResult.Invalid -> return showIssues(parsed.issues)
                is CardioInputResult.Valid -> parsed.entry
            }
            CardioKind.STAIRS -> when (val parsed = CardioInput.stairs(draft.duration, draft.floors, draft.level)) {
                is CardioInputResult.Invalid -> return showIssues(parsed.issues)
                is CardioInputResult.Valid -> parsed.entry.let {
                    CardioEntryEntity(
                        "", 0, kind.name, it.durationSeconds,
                        floorThousandths = it.floorThousandths,
                        levelThousandths = it.displayedLevelThousandths,
                    )
                }
            }
        }
        val pending = pendingEntry?.takeIf { it.copy(id = "", recordedAt = 0) == entry }
            ?: entry.copy(id = UUID.randomUUID().toString(), recordedAt = System.currentTimeMillis())
        pendingEntry = pending
        state = state.copy(saving = true, issues = emptyList(), message = null)
        viewModelScope.launch {
            try {
                if (dao.insert(pending) == -1L) check(dao.entry(pending.id) == pending)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Exception) {
                state = state.copy(saving = false, message = "Could not save cardio. Your entries are still here.")
                return@launch
            }
            pendingEntry = null
            state = state.copy(fieldValues = state.fieldValues - kind, saving = false, message = "Cardio saved.")
            loadEntries()
        }
    }

    private fun showIssues(issues: List<CardioValidationIssue>) {
        state = state.copy(issues = issues, message = null)
    }

    private suspend fun loadEntries() {
        val request = ++historyRequest
        try {
            val entries = dao.entries()
            if (request == historyRequest) state = state.copy(entries = entries, historyUnavailable = false)
        } catch (error: CancellationException) {
            throw error
        } catch (_: Exception) {
            if (request == historyRequest) state = state.copy(historyUnavailable = true)
        }
    }
}

internal fun outdoorRecord(kind: CardioKind, draft: CardioDraft): CardioInputResult<CardioEntryEntity> {
    require(kind == CardioKind.OUTDOOR_RUN || kind == CardioKind.OUTDOOR_WALK)
    return when (val parsed = CardioInput.outdoor(draft.duration, draft.distance, draft.unit)) {
        is CardioInputResult.Invalid -> parsed
        is CardioInputResult.Valid -> parsed.entry.let {
            CardioInputResult.Valid(
                CardioEntryEntity(
                    "", 0, kind.name, it.durationSeconds,
                    distanceThousandths = it.distance.thousandths,
                    distanceUnit = it.distance.unit.name,
                ),
            )
        }
    }
}
