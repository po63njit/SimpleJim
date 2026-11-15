package com.simplejim.tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simplejim.tracker.cardio.CardioError
import com.simplejim.tracker.cardio.CardioField
import com.simplejim.tracker.cardio.CardioKind
import com.simplejim.tracker.cardio.CardioState
import com.simplejim.tracker.cardio.CardioViewModel
import com.simplejim.tracker.cardio.DisplayedDistance
import com.simplejim.tracker.cardio.DistanceUnit
import com.simplejim.tracker.cardio.StairMachineEntry
import com.simplejim.tracker.cardio.TreadmillEntry
import com.simplejim.tracker.data.CardioEntryEntity
import java.math.BigDecimal
import java.text.DateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CardioScreen(modifier: Modifier, viewModel: CardioViewModel) {
    val state = viewModel.state
    val draft = state.draft
    val enabled = !state.saving
    LaunchedEffect(viewModel) { viewModel.refresh() }

    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text("Log cardio", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
            Text(
                "Only saved cardio entries survive closing the app.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CardioKind.entries.forEach { kind ->
                    FilterChip(
                        selected = state.kind == kind,
                        onClick = { viewModel.select(kind) },
                        label = { Text(kind.label()) },
                        enabled = enabled,
                    )
                }
            }
        }
        item {
            CardioFieldInput(state, CardioField.DURATION, "Duration", draft.duration, KeyboardType.Text, "mm:ss or hh:mm:ss") {
                viewModel.edit { draft -> draft.copy(duration = it) }
            }
        }
        if (state.kind == CardioKind.TREADMILL) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    DistanceUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = draft.unit == unit,
                            onClick = { viewModel.edit { it.copy(unit = unit) } },
                            label = { Text(unit.shortLabel()) },
                            enabled = enabled,
                        )
                    }
                }
                CardioFieldInput(state, CardioField.DISTANCE, "Distance (${draft.unit.shortLabel()})", draft.distance, KeyboardType.Decimal) {
                    viewModel.edit { draft -> draft.copy(distance = it) }
                }
            }
            item {
                CardioFieldInput(
                    state, CardioField.INCLINE, "Incline (%) (optional)", draft.incline, KeyboardType.Text,
                    "Use a steady grade only. Leave blank if it changed.",
                ) {
                    viewModel.edit { draft -> draft.copy(incline = it) }
                }
            }
        } else {
            item {
                CardioFieldInput(state, CardioField.FLOORS, "Floors", draft.floors, KeyboardType.Decimal) {
                    viewModel.edit { draft -> draft.copy(floors = it) }
                }
            }
            item {
                CardioFieldInput(state, CardioField.LEVEL, "Displayed level (optional)", draft.level, KeyboardType.Decimal) {
                    viewModel.edit { draft -> draft.copy(level = it) }
                }
            }
        }
        item {
            Button(onClick = viewModel::save, enabled = enabled) { Text(if (enabled) "Save cardio" else "Saving cardio…") }
            state.message?.let { Text(it, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
        }
        item {
            Text("Cardio history", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
            if (state.historyUnavailable) {
                Text("Could not load cardio history.")
                TextButton(onClick = viewModel::refresh) { Text("Retry history") }
            } else if (state.entries.isEmpty()) {
                Text("No cardio recorded yet.")
            }
        }
        items(state.entries, key = { it.id }) { entry -> CardioHistoryRecord(entry) }
    }
}

@Composable
private fun CardioFieldInput(
    state: CardioState,
    field: CardioField,
    label: String,
    value: String,
    keyboard: KeyboardType,
    hint: String? = null,
    edit: (String) -> Unit,
) {
    val error = state.issues.firstOrNull { it.field == field }?.error
    val helper = when (error) {
        CardioError.REQUIRED -> "Enter a value."
        CardioError.INVALID_FORMAT -> if (field == CardioField.DURATION) "Use mm:ss or hh:mm:ss." else "Use a number, up to three decimal places."
        CardioError.OUT_OF_RANGE -> when (field) {
            CardioField.DURATION -> "Use a positive duration; seconds and clock minutes must be below 60."
            CardioField.LEVEL -> "Level cannot be negative."
            else -> "Enter a value greater than zero."
        }
        null -> hint
    }
    OutlinedTextField(
        value = value,
        onValueChange = edit,
        label = { Text(label) },
        supportingText = if (helper == null) null else { { Text(helper) } },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        enabled = !state.saving,
        isError = error != null,
        keyboardOptions = KeyboardOptions(keyboardType = keyboard),
    )
}

@Composable
private fun CardioHistoryRecord(entry: CardioEntryEntity) {
    val kind = CardioKind.entries.firstOrNull { it.name == entry.kind } ?: return
    val details = when (kind) {
        CardioKind.TREADMILL -> {
            val unit = DistanceUnit.entries.firstOrNull { it.name == entry.distanceUnit } ?: return
            val distance = DisplayedDistance(entry.distanceThousandths ?: return, unit)
            val treadmill = TreadmillEntry(entry.durationSeconds, distance, entry.inclineThousandths)
            listOf(
                "${decimal(distance.thousandths)} ${unit.shortLabel()}",
                "${clock(treadmill.averagePaceSecondsPerEnteredUnit())}/${unit.shortLabel()} average",
                entry.inclineThousandths?.let { "${decimal(it)}% incline" },
            )
        }
        CardioKind.STAIRS -> {
            val stair = StairMachineEntry(entry.durationSeconds, entry.floorThousandths ?: return, entry.levelThousandths)
            listOf(
                "${decimal(stair.floorThousandths)} floors",
                "${stair.averageFloorsPerMinute().toPlainString()} floors/min average",
                entry.levelThousandths?.let { "Level ${decimal(it)}" },
            )
        }
    }.filterNotNull().joinToString(" · ")
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("${kind.label()} · ${clock(entry.durationSeconds)}", style = MaterialTheme.typography.titleSmall)
            Text(details)
            Text(DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(entry.recordedAt)))
        }
    }
}

private fun CardioKind.label() = if (this == CardioKind.TREADMILL) "Treadmill" else "Stair machine"
private fun DistanceUnit.shortLabel() = if (this == DistanceUnit.MILE) "mi" else "km"
private fun decimal(value: Long) = BigDecimal.valueOf(value, 3).stripTrailingZeros().toPlainString()
private fun clock(seconds: Long): String =
    if (seconds >= 3600) String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
    else String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
