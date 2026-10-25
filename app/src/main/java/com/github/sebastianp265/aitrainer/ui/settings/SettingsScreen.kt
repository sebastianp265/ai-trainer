package com.github.sebastianp265.aitrainer.ui.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.BuildConfig
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.domain.model.Equipment
import com.github.sebastianp265.aitrainer.domain.model.Goal
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup
import com.github.sebastianp265.aitrainer.ui.components.Stepper
import com.github.sebastianp265.aitrainer.ui.labelRes
import com.github.sebastianp265.aitrainer.ui.theme.AITrainerTheme
import org.koin.compose.viewmodel.koinViewModel

object SettingsTags {
    const val SESSIONS_PER_WEEK = "settings_sessions_per_week"
    const val SESSION_LENGTH = "settings_session_length"
    const val NOTES = "settings_notes"
    const val CLAUDE_API_KEY = "settings_claude_api_key"
    const val CLAUDE_MODEL = "settings_claude_model"
}

interface SettingsActions {
    fun setGoal(goal: Goal)
    fun changeSessionsPerWeek(delta: Int)
    fun changeSessionLength(steps: Int)
    fun toggleWeakMuscleGroup(muscleGroup: MuscleGroup)
    fun toggleEquipment(equipment: Equipment)
    fun setNotes(notes: String)
    fun saveClaudeApiKey(apiKey: String)
    fun removeClaudeApiKey()
    fun setClaudeModel(modelId: String)
    fun openExercisePreferences()
    fun openDebugTools()
}

@Composable
fun SettingsScreen(
    onOpenExercisePreferences: () -> Unit,
    onOpenDebugTools: () -> Unit,
    viewModel: SettingsViewModel = koinViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel, onOpenExercisePreferences, onOpenDebugTools) {
        object : SettingsActions {
            override fun setGoal(goal: Goal) = viewModel.setGoal(goal)
            override fun changeSessionsPerWeek(delta: Int) = viewModel.changeSessionsPerWeek(delta)
            override fun changeSessionLength(steps: Int) = viewModel.changeSessionLength(steps)
            override fun toggleWeakMuscleGroup(muscleGroup: MuscleGroup) = viewModel.toggleWeakMuscleGroup(muscleGroup)
            override fun toggleEquipment(equipment: Equipment) = viewModel.toggleEquipment(equipment)
            override fun setNotes(notes: String) = viewModel.setNotes(notes)
            override fun saveClaudeApiKey(apiKey: String) = viewModel.saveClaudeApiKey(apiKey)
            override fun removeClaudeApiKey() = viewModel.removeClaudeApiKey()
            override fun setClaudeModel(modelId: String) = viewModel.setClaudeModel(modelId)
            override fun openExercisePreferences() = onOpenExercisePreferences()
            override fun openDebugTools() = onOpenDebugTools()
        }
    }
    SettingsContent(state, actions, showDebugTools = BuildConfig.DEBUG)
}

@Composable
fun SettingsContent(state: SettingsUiState, actions: SettingsActions, showDebugTools: Boolean) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { SectionTitle(R.string.settings_training_profile) }
        item { GoalChips(state.profile.goal, actions::setGoal) }
        item { TrainingVolume(state, actions) }
        item {
            ChipGroup(
                title = R.string.settings_weak_muscle_groups,
                options = MuscleGroup.entries,
                selected = state.profile.weakMuscleGroups,
                label = { it.labelRes() },
                onToggle = actions::toggleWeakMuscleGroup,
            )
        }
        item {
            ChipGroup(
                title = R.string.settings_available_equipment,
                options = Equipment.entries,
                selected = state.profile.availableEquipment,
                label = { it.labelRes() },
                onToggle = actions::toggleEquipment,
            )
        }
        item { NotesField(state.profile.notes, actions::setNotes) }
        item {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_exercise_preferences)) },
                supportingContent = { Text(stringResource(R.string.settings_exercise_preferences_hint)) },
                modifier = Modifier.clickable(onClick = actions::openExercisePreferences),
            )
        }
        item { HorizontalDivider() }
        item { SectionTitle(R.string.settings_ai_trainer) }
        item { ClaudeApiKey(state.isClaudeApiKeyStored, actions::saveClaudeApiKey, actions::removeClaudeApiKey) }
        item { ClaudeModelField(state.claudeModel, actions::setClaudeModel) }
        if (showDebugTools) {
            item { HorizontalDivider() }
            item {
                ListItem(
                    headlineContent = { Text(stringResource(R.string.settings_debug_tools)) },
                    modifier = Modifier.clickable(onClick = actions::openDebugTools),
                )
            }
        }
    }
}

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun GoalChips(goal: Goal, onSelect: (Goal) -> Unit) {
    Column {
        Text(stringResource(R.string.settings_goal), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Goal.entries.forEach {
                FilterChip(selected = it == goal, onClick = { onSelect(it) }, label = { Text(stringResource(it.labelRes())) })
            }
        }
    }
}

@Composable
private fun TrainingVolume(state: SettingsUiState, actions: SettingsActions) {
    Column {
        LabeledRow(R.string.settings_sessions_per_week) {
            Stepper(
                value = state.profile.sessionsPerWeek.toString(),
                valueTag = SettingsTags.SESSIONS_PER_WEEK,
                unit = stringResource(R.string.unit_sessions),
                decreaseDescription = stringResource(R.string.decrease_sessions_per_week),
                increaseDescription = stringResource(R.string.increase_sessions_per_week),
                onMinus = { actions.changeSessionsPerWeek(-1) },
                onPlus = { actions.changeSessionsPerWeek(1) },
            )
        }
        LabeledRow(R.string.settings_session_length) {
            Stepper(
                value = state.profile.sessionLengthMin.toString(),
                valueTag = SettingsTags.SESSION_LENGTH,
                unit = stringResource(R.string.unit_min),
                decreaseDescription = stringResource(R.string.decrease_session_length),
                increaseDescription = stringResource(R.string.increase_session_length),
                onMinus = { actions.changeSessionLength(-1) },
                onPlus = { actions.changeSessionLength(1) },
            )
        }
    }
}

@Composable
private fun LabeledRow(@StringRes label: Int, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(label), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        content()
    }
}

@Composable
private fun <T> ChipGroup(
    @StringRes title: Int,
    options: List<T>,
    selected: Set<T>,
    label: (T) -> Int,
    onToggle: (T) -> Unit,
) {
    Column {
        Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach {
                FilterChip(selected = it in selected, onClick = { onToggle(it) }, label = { Text(stringResource(label(it))) })
            }
        }
    }
}

@Composable
private fun NotesField(savedNotes: String, onNotesChange: (String) -> Unit) {
    var notes by rememberSaveable { mutableStateOf(savedNotes) }
    OutlinedTextField(
        value = notes,
        onValueChange = {
            notes = it
            onNotesChange(it)
        },
        label = { Text(stringResource(R.string.settings_notes)) },
        minLines = 2,
        modifier = Modifier.fillMaxWidth().testTag(SettingsTags.NOTES),
    )
}

@Composable
private fun ClaudeApiKey(isStored: Boolean, onSave: (String) -> Unit, onRemove: () -> Unit) {
    var apiKey by rememberSaveable { mutableStateOf("") }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(if (isStored) R.string.settings_api_key_saved else R.string.settings_api_key_missing),
            style = MaterialTheme.typography.bodyMedium,
        )
        OutlinedTextField(
            value = apiKey,
            onValueChange = { apiKey = it },
            label = { Text(stringResource(R.string.settings_claude_api_key)) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth().testTag(SettingsTags.CLAUDE_API_KEY),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    onSave(apiKey)
                    apiKey = ""
                },
                enabled = apiKey.isNotBlank(),
            ) { Text(stringResource(R.string.save)) }
            if (isStored) {
                OutlinedButton(onClick = onRemove) { Text(stringResource(R.string.remove)) }
            }
        }
    }
}

@Composable
private fun ClaudeModelField(savedModel: String, onModelChange: (String) -> Unit) {
    var model by rememberSaveable { mutableStateOf(savedModel) }
    OutlinedTextField(
        value = model,
        onValueChange = {
            model = it
            onModelChange(it)
        },
        label = { Text(stringResource(R.string.settings_claude_model)) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth().testTag(SettingsTags.CLAUDE_MODEL),
    )
}

private object NoOpSettingsActions : SettingsActions {
    override fun setGoal(goal: Goal) {}
    override fun changeSessionsPerWeek(delta: Int) {}
    override fun changeSessionLength(steps: Int) {}
    override fun toggleWeakMuscleGroup(muscleGroup: MuscleGroup) {}
    override fun toggleEquipment(equipment: Equipment) {}
    override fun setNotes(notes: String) {}
    override fun saveClaudeApiKey(apiKey: String) {}
    override fun removeClaudeApiKey() {}
    override fun setClaudeModel(modelId: String) {}
    override fun openExercisePreferences() {}
    override fun openDebugTools() {}
}

@Preview(showBackground = true)
@Preview(name = "Polski", locale = "pl", showBackground = true)
@Composable
private fun SettingsPreview() {
    AITrainerTheme {
        SettingsContent(SettingsUiState(isLoading = false, isClaudeApiKeyStored = true), NoOpSettingsActions, showDebugTools = true)
    }
}
