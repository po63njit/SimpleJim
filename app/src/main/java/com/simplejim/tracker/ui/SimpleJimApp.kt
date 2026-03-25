package com.simplejim.tracker.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.simplejim.tracker.DraftExercise
import com.simplejim.tracker.SimpleJimTab
import com.simplejim.tracker.SimpleJimUiState
import com.simplejim.tracker.SimpleJimViewModel
import com.simplejim.tracker.data.WorkoutExercise
import com.simplejim.tracker.data.WorkoutSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SimpleJimApp(viewModel: SimpleJimViewModel) {
    val state = viewModel.uiState

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            HeaderBanner(lastWorkout = state.history.firstOrNull())
            TabRow(selectedTabIndex = state.selectedTab.ordinal) {
                SimpleJimTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(text = tab.name) },
                    )
                }
            }

            when (state.selectedTab) {
                SimpleJimTab.Track -> TrackScreen(
                    modifier = Modifier.weight(1f),
                    state = state,
                    onNotesChange = viewModel::updateNotes,
                    onExerciseNameChange = viewModel::updateExerciseName,
                    onSetWeightChange = viewModel::updateSetWeight,
                    onSetRepsChange = viewModel::updateSetReps,
                    onAddExercise = viewModel::addExercise,
                    onAddPresetExercise = viewModel::addPresetExercise,
                    onRemoveExercise = viewModel::removeExercise,
                    onAddSet = viewModel::addSet,
                    onRemoveLastSet = viewModel::removeLastSet,
                    onResetDraft = viewModel::resetDraft,
                    onRepeatLastWorkout = viewModel::repeatLastWorkout,
                    onSaveWorkout = viewModel::saveWorkout,
                )

                SimpleJimTab.History -> HistoryScreen(
                    modifier = Modifier.weight(1f),
                    history = state.history,
                    lastSavedAt = state.lastSavedAt,
                )
            }
        }
    }
}

@Composable
private fun HeaderBanner(lastWorkout: WorkoutSession?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp)
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.secondary,
                    ),
                ),
            )
            .padding(24.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = "SimpleJim",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = "Bare-bones lifting logs for a phone-first routine.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary,
            )
            Text(
                text = lastWorkout?.let { "Last session: ${formatTimestamp(it.performedAt)}" }
                    ?: "No sessions logged yet. Start with one exercise and one set.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimary,
            )
        }
    }
}

@Composable
private fun TrackScreen(
    modifier: Modifier,
    state: SimpleJimUiState,
    onNotesChange: (String) -> Unit,
    onExerciseNameChange: (Long, String) -> Unit,
    onSetWeightChange: (Long, Int, String) -> Unit,
    onSetRepsChange: (Long, Int, String) -> Unit,
    onAddExercise: () -> Unit,
    onAddPresetExercise: (String) -> Unit,
    onRemoveExercise: (Long) -> Unit,
    onAddSet: (Long) -> Unit,
    onRemoveLastSet: (Long) -> Unit,
    onResetDraft: () -> Unit,
    onRepeatLastWorkout: () -> Unit,
    onSaveWorkout: () -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .imePadding(),
        contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item {
            SummaryCard(
                title = "Today's Log",
                body = "Use pounds, tap in reps, and save when you finish the workout.",
                supporting = "${state.history.size} total logged sessions",
            )
        }

        item {
            QuickAddRow(
                presets = state.quickPresets,
                onAddPresetExercise = onAddPresetExercise,
            )
        }

        if (state.history.isNotEmpty()) {
            item {
                OutlinedButton(
                    onClick = onRepeatLastWorkout,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Repeat Last Workout")
                }
            }
        }

        state.validationMessage?.let { message ->
            item {
                StatusCard(
                    title = "Can't save yet",
                    body = message,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }

        item {
            OutlinedTextField(
                value = state.draftNotes,
                onValueChange = onNotesChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes") },
                placeholder = { Text("Optional: push day, low energy, PR attempt...") },
                minLines = 2,
                maxLines = 4,
            )
        }

        itemsIndexed(
            items = state.draftExercises,
            key = { _, exercise -> exercise.id },
        ) { index, exercise ->
            ExerciseEditorCard(
                index = index,
                exercise = exercise,
                canRemoveExercise = state.draftExercises.size > 1,
                onNameChange = { onExerciseNameChange(exercise.id, it) },
                onWeightChange = { setIdx, weight -> onSetWeightChange(exercise.id, setIdx, weight) },
                onRepsChange = { setIdx, reps -> onSetRepsChange(exercise.id, setIdx, reps) },
                onAddSet = { onAddSet(exercise.id) },
                onRemoveLastSet = { onRemoveLastSet(exercise.id) },
                onRemoveExercise = { onRemoveExercise(exercise.id) },
            )
        }

        item {
            OutlinedButton(
                onClick = onAddExercise,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Add New Exercise")
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onSaveWorkout,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Save Workout")
                }
                OutlinedButton(
                    onClick = onResetDraft,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Start Fresh")
                }
            }
        }
    }
}

@Composable
private fun QuickAddRow(
    presets: List<String>,
    onAddPresetExercise: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Quick add",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(presets) { preset ->
                OutlinedButton(onClick = { onAddPresetExercise(preset) }) {
                    Text(preset)
                }
            }
        }
    }
}

@Composable
private fun ExerciseEditorCard(
    index: Int,
    exercise: DraftExercise,
    canRemoveExercise: Boolean,
    onNameChange: (String) -> Unit,
    onWeightChange: (Int, String) -> Unit,
    onRepsChange: (Int, String) -> Unit,
    onAddSet: () -> Unit,
    onRemoveLastSet: () -> Unit,
    onRemoveExercise: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        ),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Exercise #${index + 1}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                )
                if (canRemoveExercise) {
                    TextButton(onClick = onRemoveExercise) {
                        Text("Remove")
                    }
                }
            }

            OutlinedTextField(
                value = exercise.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name (e.g. Squat)") },
                singleLine = true,
            )

            exercise.sets.forEachIndexed { setIndex, set ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "${setIndex + 1}",
                        modifier = Modifier.padding(horizontal = 4.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    OutlinedTextField(
                        value = set.weight,
                        onValueChange = { onWeightChange(setIndex, it) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Weight") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                    )
                    OutlinedTextField(
                        value = set.reps,
                        onValueChange = { onRepsChange(setIndex, it) },
                        modifier = Modifier.weight(1f),
                        label = { Text("Reps") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                TextButton(
                    onClick = onAddSet,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("+ Add Set")
                }
                if (exercise.sets.size > 1) {
                    TextButton(
                        onClick = onRemoveLastSet,
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("- Remove Set")
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryScreen(
    modifier: Modifier,
    history: List<WorkoutSession>,
    lastSavedAt: Long?,
) {
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        if (history.isEmpty()) {
            item {
                StatusCard(
                    title = "Nothing here yet",
                    body = "Your logged workouts will appear here in reverse chronological order.",
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (lastSavedAt != null) {
            item {
                StatusCard(
                    title = "Workout Saved!",
                    body = "Last session logged at ${formatTimestamp(lastSavedAt)}.",
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                )
            }
        }

        items(history) { session ->
            HistorySessionCard(session)
        }
    }
}

@Composable
private fun HistorySessionCard(session: WorkoutSession) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = formatTimestamp(session.performedAt),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            if (session.notes.isNotEmpty()) {
                Text(
                    text = session.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Column(
                modifier = Modifier.padding(top = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                session.exercises.forEach { exercise ->
                    ExerciseSummaryRow(exercise)
                }
            }
        }
    }
}

@Composable
private fun ExerciseSummaryRow(exercise: WorkoutExercise) {
    Column {
        Text(
            text = exercise.name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = exercise.sets.joinToString(" • ") { "${it.weight} x ${it.reps}" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SummaryCard(title: String, body: String, supporting: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
        ),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
            )
            Text(
                text = supporting,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    body: String,
    containerColor: Color,
    contentColor: Color,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor,
        ),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
            )
            Text(text = body, style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
