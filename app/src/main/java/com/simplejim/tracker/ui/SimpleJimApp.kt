package com.simplejim.tracker.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.simplejim.tracker.DraftExercise
import com.simplejim.tracker.DraftReplacementAction
import com.simplejim.tracker.SimpleJimTab
import com.simplejim.tracker.SimpleJimUiState
import com.simplejim.tracker.SimpleJimViewModel
import com.simplejim.tracker.toWorkoutSetOrNull
import com.simplejim.tracker.cardio.CardioViewModel
import com.simplejim.tracker.data.WorkoutExercise
import com.simplejim.tracker.data.WorkoutSession
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun SimpleJimApp(viewModel: SimpleJimViewModel, cardioViewModel: CardioViewModel) {
    val state = viewModel.uiState
    val keyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    state.pendingDraftReplacement?.let { action ->
        val repeat = action == DraftReplacementAction.Repeat
        AlertDialog(
            onDismissRequest = viewModel::cancelDraftReplacement,
            title = { Text(if (repeat) "Replace this draft?" else "Clear this draft?") },
            text = {
                Text(
                    if (repeat) "The last saved workout will replace the current draft. Saved history will not change."
                    else "Your current draft will be discarded. Saved history will not change.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDraftReplacement) {
                    Text(if (repeat) "Replace draft" else "Clear draft")
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDraftReplacement) { Text("Keep draft") }
            },
        )
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .consumeWindowInsets(innerPadding)
                .imePadding(),
        ) {
            CompactHeader()
            TabRow(
                selectedTabIndex = state.selectedTab.ordinal,
                containerColor = MaterialTheme.colorScheme.background,
            ) {
                SimpleJimTab.entries.forEach { tab ->
                    Tab(
                        selected = state.selectedTab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(text = tab.name) },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    onCopyLastSet = viewModel::copyLastSet,
                    onRemoveLastSet = viewModel::removeLastSet,
                    onResetDraft = viewModel::requestDraftReset,
                    onRetryDraftStorage = viewModel::retryDraftStorage,
                    onRepeatLastWorkout = viewModel::requestRepeatLastWorkout,
                )

                SimpleJimTab.Cardio -> CardioScreen(Modifier.weight(1f), cardioViewModel)

                SimpleJimTab.History -> HistoryScreen(
                    modifier = Modifier.weight(1f),
                    history = state.history,
                    lastSavedAt = state.lastSavedAt,
                )
            }
            if (state.selectedTab == SimpleJimTab.Track && state.draftReady && !keyboardVisible) {
                SaveBar(onSaveWorkout = viewModel::saveWorkout, enabled = !state.draftBusy)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CompactHeader() {
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.Start),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = "SimpleJim",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        LiftLogStamp()
    }
}

@Composable
private fun LiftLogStamp() {
    val accent = MaterialTheme.colorScheme.tertiary
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = MaterialTheme.colorScheme.background,
        contentColor = accent,
        border = BorderStroke(1.dp, accent),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Surface(modifier = Modifier.size(5.dp), shape = CircleShape, color = accent) {}
            Text(
                text = "LIFT LOG",
                style = MaterialTheme.typography.labelSmall,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.7.sp,
            )
        }
    }
}

@Composable
private fun SaveBar(onSaveWorkout: () -> Unit, enabled: Boolean) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Button(
                onClick = onSaveWorkout,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
            ) {
                Text("Save Workout", modifier = Modifier.padding(vertical = 2.dp))
            }
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
    onCopyLastSet: (Long) -> Unit,
    onRemoveLastSet: (Long) -> Unit,
    onResetDraft: () -> Unit,
    onRetryDraftStorage: () -> Unit,
    onRepeatLastWorkout: () -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        state.validationMessage?.let { message ->
            StatusCard(
                title = "Can't save yet",
                body = message,
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp),
            )
        }
        state.draftStorageError?.let { message ->
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                StatusCard(
                    title = "Workout storage",
                    body = message,
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                )
                TextButton(onClick = onRetryDraftStorage, enabled = !state.draftBusy) {
                    Text(if (state.draftReady) "Retry draft save" else "Try loading again")
                }
                if (state.canDiscardUnreadableDraft) {
                    TextButton(onClick = onResetDraft, enabled = !state.draftBusy) {
                        Text("Start fresh without this draft")
                    }
                }
            }
        }
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Workout log",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.semantics { heading() },
                    )
                    Text(
                        text = when {
                            !state.draftReady && state.draftBusy -> "Loading workout draft…"
                            !state.draftReady -> "Workout draft not loaded."
                            state.draftBusy -> "Updating workout…"
                            state.pendingDraftWrites > 0 -> "Saving draft…"
                            state.history.isEmpty() -> "Start with one exercise."
                            else -> "Last session: ${formatTimestamp(state.history.first().performedAt)}"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (!state.draftReady) return@LazyColumn

            item {
                QuickAddRow(
                    presets = state.quickPresets,
                    onAddPresetExercise = onAddPresetExercise,
                )
            }

            if (state.history.isNotEmpty()) {
                item {
                    TextButton(onClick = onRepeatLastWorkout) { Text("Repeat Last Workout") }
                }
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
                    onCopyLastSet = { onCopyLastSet(exercise.id) },
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
                OutlinedTextField(
                    value = state.draftNotes,
                    onValueChange = onNotesChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Workout notes (optional)") },
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                )
            }

            item {
                TextButton(onClick = onResetDraft, enabled = !state.draftBusy) { Text("Start Fresh") }
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseEditorCard(
    index: Int,
    exercise: DraftExercise,
    canRemoveExercise: Boolean,
    onNameChange: (String) -> Unit,
    onWeightChange: (Int, String) -> Unit,
    onRepsChange: (Int, String) -> Unit,
    onAddSet: () -> Unit,
    onCopyLastSet: () -> Unit,
    onRemoveLastSet: () -> Unit,
    onRemoveExercise: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Exercise ${index + 1}",
                    modifier = Modifier.weight(1f).semantics { heading() },
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                )
                if (canRemoveExercise) {
                    TextButton(
                        onClick = onRemoveExercise,
                        modifier = Modifier.semantics { contentDescription = "Remove exercise ${index + 1}" },
                    ) {
                        Text("Remove", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            OutlinedTextField(
                value = exercise.name,
                onValueChange = onNameChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Name (e.g. Squat)") },
                shape = RoundedCornerShape(10.dp),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                singleLine = true,
            )

            HorizontalDivider(
                modifier = Modifier.padding(top = 4.dp),
                color = MaterialTheme.colorScheme.outlineVariant,
            )

            exercise.sets.forEachIndexed { setIndex, set ->
                SetEditorRow(
                    setNumber = setIndex + 1,
                    weight = set.weight,
                    reps = set.reps,
                    onWeightChange = { onWeightChange(setIndex, it) },
                    onRepsChange = { onRepsChange(setIndex, it) },
                )
            }

            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = onAddSet) { Text("Add blank set") }
                TextButton(
                    onClick = onCopyLastSet,
                    enabled = exercise.sets.lastOrNull()?.toWorkoutSetOrNull() != null,
                ) { Text("Copy last set") }

                if (exercise.sets.size > 1) {
                    TextButton(onClick = onRemoveLastSet) {
                        Text("Remove last set", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun SetEditorRow(
    setNumber: Int,
    weight: String,
    reps: String,
    onWeightChange: (String) -> Unit,
    onRepsChange: (String) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        val stackFields = maxWidth < 252.dp || LocalDensity.current.fontScale >= 1.25f
        val shape = RoundedCornerShape(10.dp)
        val number: @Composable () -> Unit = {
            Text(
                text = "Set $setNumber",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
            )
        }
        val weightField: @Composable (Modifier) -> Unit = { modifier ->
            OutlinedTextField(
                value = weight,
                onValueChange = onWeightChange,
                modifier = modifier,
                label = { Text("Weight (lb)") },
                shape = shape,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Decimal,
                    imeAction = ImeAction.Next,
                ),
                singleLine = true,
            )
        }
        val repsField: @Composable (Modifier) -> Unit = { modifier ->
            OutlinedTextField(
                value = reps,
                onValueChange = onRepsChange,
                modifier = modifier,
                label = { Text("Reps") },
                shape = shape,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                singleLine = true,
            )
        }

        if (stackFields) {
            Column(modifier = Modifier.fillMaxWidth().padding(top = 6.dp)) {
                number()
                weightField(Modifier.fillMaxWidth())
                repsField(Modifier.fillMaxWidth())
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                number()
                weightField(Modifier.weight(1.25f))
                repsField(Modifier.weight(1f))
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
                    aside = "Big lifts. Small paperwork.",
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
        shape = RoundedCornerShape(16.dp),
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
            text = exercise.sets.joinToString("  ·  ") { "${it.weight} lb × ${it.reps}" },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun StatusCard(
    title: String,
    body: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier,
    aside: String? = null,
) {
    Card(
        modifier = modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite },
        shape = RoundedCornerShape(12.dp),
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
            if (aside != null) {
                Text(
                    text = aside,
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
