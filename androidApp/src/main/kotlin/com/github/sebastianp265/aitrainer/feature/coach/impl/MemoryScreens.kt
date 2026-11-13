package com.github.sebastianp265.aitrainer.feature.coach.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.coach.CoachNote
import com.github.sebastianp265.aitrainer.integration.coach.CoachMemoryService
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup
import com.github.sebastianp265.aitrainer.integration.volume.TargetSource
import com.github.sebastianp265.aitrainer.integration.volume.VolumeService
import com.github.sebastianp265.aitrainer.integration.volume.WeeklyTarget
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.shared.localization.formatDayMonth
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons
import com.github.sebastianp265.aitrainer.shared.design.GroupCard
import com.github.sebastianp265.aitrainer.shared.design.GroupRow
import com.github.sebastianp265.aitrainer.shared.design.SectionHeader
import androidx.compose.foundation.layout.Box

internal data class MemoryUiState(
    val isLoading: Boolean = true,
    val profile: Map<String, String> = emptyMap(),
    val notes: List<CoachNote> = emptyList(),
    val targets: List<WeeklyTarget> = emptyList(),
    val groups: List<MuscleGroup> = emptyList(),
)

internal class MemoryViewModel(
    private val memoryService: CoachMemoryService,
    volumeService: VolumeService,
    exerciseService: ExerciseService,
) : ViewModel() {

    val uiState: StateFlow<MemoryUiState> = combine(
        memoryService.observeProfile(),
        memoryService.observeNotes(),
        volumeService.observeTargets(),
        exerciseService.observeMuscleGroups(),
    ) { profile, notes, targets, groups -> MemoryUiState(false, profile, notes, targets, groups) }
        .stateIn(viewModelScope, WhileUiSubscribed, MemoryUiState())

    fun setProfile(field: String, value: String) {
        viewModelScope.launch { memoryService.setProfileField(field, value) }
    }

    fun updateNote(id: Long, text: String) {
        viewModelScope.launch { memoryService.updateNote(id, text) }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { memoryService.deleteNote(id) }
    }
}

@Composable
internal fun MemoryContent(state: MemoryUiState, actions: MemoryActions) {
    var editingField by rememberSaveable { mutableStateOf<String?>(null) }
    var editingNote by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingNote by rememberSaveable { mutableStateOf<Long?>(null) }
    LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(
                stringResource(R.string.memory_intro),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        item { SectionHeader(stringResource(R.string.memory_profile)) }
        item {
            GroupCard {
                CoachMemoryService.PROFILE_FIELDS.forEachIndexed { index, field ->
                    GroupRow(divider = index < CoachMemoryService.PROFILE_FIELDS.lastIndex) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(profileLabel(field), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                state.profile[field] ?: stringResource(R.string.memory_unknown),
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.testTag(CoachMemoryTags.profile(field) + ":value"),
                            )
                        }
                        EditIcon(stringResource(R.string.memory_edit, profileLabel(field)), Modifier.testTag(CoachMemoryTags.profile(field))) { editingField = field }
                    }
                }
            }
        }
        item { SectionHeader(stringResource(R.string.memory_targets)) }
        item {
            GroupCard {
                GroupRow(onClick = actions::openTargets, divider = false) {
                    Text(targetsSummary(state), Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        stringResource(R.string.memory_change),
                        Modifier.padding(end = 12.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
        item { SectionHeader(stringResource(R.string.memory_notes)) }
        if (state.notes.isEmpty()) {
            item { Text(stringResource(R.string.memory_no_notes), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            item {
                GroupCard {
                    state.notes.forEachIndexed { index, note ->
                        GroupRow(Modifier.testTag(CoachMemoryTags.NOTE), divider = index < state.notes.lastIndex) {
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    formatDayMonth(note.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(note.text, style = MaterialTheme.typography.bodyLarge)
                            }
                            EditIcon(stringResource(R.string.memory_edit_note)) { editingNote = note.id }
                            IconButton(onClick = { deletingNote = note.id }, Modifier.testTag(CoachMemoryTags.DELETE_NOTE)) {
                                Icon(DesignIcons.Delete, contentDescription = stringResource(R.string.memory_delete_note), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
        item { Box(Modifier.padding(bottom = 16.dp)) }
    }
    editingField?.let { field ->
        TextEditDialog(
            title = stringResource(R.string.memory_edit, profileLabel(field)),
            initial = state.profile[field].orEmpty(),
            onSave = { actions.setProfile(field, it); editingField = null },
            onDismiss = { editingField = null },
        )
    }
    editingNote?.let { id ->
        TextEditDialog(
            title = stringResource(R.string.memory_edit_note),
            initial = state.notes.firstOrNull { it.id == id }?.text.orEmpty(),
            onSave = { actions.updateNote(id, it); editingNote = null },
            onDismiss = { editingNote = null },
        )
    }
    deletingNote?.let { id ->
        AlertDialog(
            onDismissRequest = { deletingNote = null },
            title = { Text(stringResource(R.string.memory_delete_note_title)) },
            confirmButton = { TextButton(onClick = { actions.deleteNote(id); deletingNote = null }, Modifier.testTag(CoachMemoryTags.CONFIRM_DELETE)) { Text(stringResource(R.string.remove)) } },
            dismissButton = { TextButton(onClick = { deletingNote = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

internal interface MemoryActions {
    fun setProfile(field: String, value: String)
    fun updateNote(id: Long, text: String)
    fun deleteNote(id: Long)
    fun openTargets()
}

@Composable
private fun EditIcon(description: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    IconButton(onClick = onClick, modifier) { Icon(DesignIcons.Edit, contentDescription = description, tint = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun profileLabel(field: String): String = stringResource(
    when (field) {
        "goal" -> R.string.memory_field_goal
        "time" -> R.string.memory_field_time
        "equipment" -> R.string.memory_field_equipment
        "limitations" -> R.string.memory_field_limitations
        else -> R.string.memory_field_preferences
    },
)

@Composable
private fun targetsSummary(state: MemoryUiState): String {
    val locale = LocalConfiguration.current.locales[0]
    val groups = state.groups.associateBy { it.key }
    val custom = state.targets.filter { it.source != TargetSource.DEFAULT }
    if (custom.isEmpty()) return stringResource(R.string.memory_targets_default)
    val parts = custom.map { stringResource(R.string.memory_target_item, groups[it.groupKey]?.displayName(locale) ?: it.groupKey, it.minSets, it.maxSets) }
    return parts.joinToString(" · ") + if (custom.size < state.targets.size) "\n" + stringResource(R.string.memory_targets_rest_default) else ""
}

@Composable
private fun TextEditDialog(title: String, initial: String, onSave: (String) -> Unit, onDismiss: () -> Unit) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.fillMaxWidth().testTag(CoachMemoryTags.EDIT_FIELD)) },
        confirmButton = { TextButton(onClick = { onSave(text) }, Modifier.testTag(CoachMemoryTags.SAVE)) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

internal object CoachMemoryTags {
    const val NOTE = "memory_note"
    const val DELETE_NOTE = "memory_delete_note"
    const val CONFIRM_DELETE = "memory_confirm_delete"
    const val EDIT_FIELD = "memory_edit_field"
    const val SAVE = "memory_save"

    fun profile(field: String) = "memory_profile:$field"
}
