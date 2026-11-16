package com.simplejim.tracker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.simplejim.tracker.StrengthHistoryState
import com.simplejim.tracker.strength.StrengthSessionRow
import com.simplejim.tracker.strength.estimateLabel
import com.simplejim.tracker.strength.exerciseDisplayName
import com.simplejim.tracker.strength.sourceSetLabel
import com.simplejim.tracker.strength.strengthChart
import com.simplejim.tracker.strength.strengthDateTime
import com.simplejim.tracker.strength.strengthSessionRows

@Composable
internal fun StrengthHistoryScreen(
    modifier: Modifier,
    state: StrengthHistoryState,
    onSelectExercise: (String) -> Unit,
    onRetry: () -> Unit,
    onBack: () -> Unit,
) {
    val selected = state.selectedKey?.let(state.exercises::get)
    var newestFirst by rememberSaveable(state.selectedKey) { mutableStateOf(true) }
    val scroll = key(state.selectedKey, newestFirst) { rememberLazyListState() }
    LazyColumn(
        state = scroll,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            TextButton(onClick = onBack) {
                Text(if (selected == null) "Back to workout history" else "Back to exercises")
            }
            StrengthHeading(selected?.let(::exerciseDisplayName) ?: "Strength history")
        }
        when {
            state.isLoading -> item { StrengthStatus("Loading saved strength sets…") }
            state.failedToLoad -> item {
                StrengthStatus("Strength history could not be loaded. Your saved workouts were not changed.")
                OutlinedButton(onClick = onRetry) { Text("Try again") }
            }
            state.exercises.isEmpty() -> item {
                Text("No complete saved strength sets to show. Drafts and unreadable sets do not appear here.")
            }
            selected == null -> {
                item { Text("Choose an exercise to see observed loads and provisional estimates.") }
                items(state.exercises.keys.sorted(), key = { it }) { key ->
                    OutlinedButton(onClick = { onSelectExercise(key) }, modifier = Modifier.fillMaxWidth()) {
                        Text(exerciseDisplayName(state.exercises.getValue(key)))
                    }
                }
            }
            else -> {
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StrengthHeading("Heaviest saved loads")
                        Text("Heaviest logged set: ${sourceSetLabel(selected.heaviestActual)}")
                        Text(
                            "Heaviest logged single: " +
                                (selected.heaviestSingle?.let { sourceSetLabel(it) } ?: "none saved"),
                        )
                        Text("A logged single is not necessarily a tested max.")
                    }
                }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StrengthHeading("Session trend")
                        Text(
                            (if (newestFirst) "Newest first. " else "Oldest first. ") +
                                "Estimated 1RM is provisional (Epley, 2–10 reps), not a weight actually lifted.",
                        )
                        if (selected.sessions.size > 1) {
                            TextButton(onClick = { newestFirst = !newestFirst }) {
                                Text(if (newestFirst) "Show oldest first" else "Show newest first")
                            }
                        }
                    }
                }
                item { StrengthChartPanel(strengthChart(selected)) }
                items(strengthSessionRows(selected, newestFirst), key = { "${it.point.sessionId}:${it.point.performedAt}" }) { row ->
                    StrengthSessionItem(row)
                }
            }
        }
    }
}

@Composable
private fun StrengthStatus(label: String) {
    Text(label, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
}

@Composable
private fun StrengthHeading(label: String) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium,
        modifier = Modifier.semantics { heading() },
    )
}

@Composable
private fun StrengthSessionItem(row: StrengthSessionRow) {
    val point = row.point
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            StrengthHeading("Session ${row.ordinal} · ${strengthDateTime(point.performedAt)}")
            Text("Observed heaviest: ${sourceSetLabel(point.heaviestActual)}")
            point.heaviestSingle?.let { Text("Logged single: ${sourceSetLabel(it)}") }
            val estimate = point.bestEstimate
            if (estimate == null) {
                Text("Estimated 1RM: no qualifying set")
            } else {
                Text("Estimated 1RM: ${estimateLabel(estimate)}")
                Text("Estimate from: ${sourceSetLabel(estimate.sourceSet)}")
            }
        }
    }
}
