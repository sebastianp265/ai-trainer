package com.github.sebastianp265.aitrainer.feature.coach.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
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
import com.github.sebastianp265.aitrainer.integration.coach.CoachService
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

internal data class MemoryUiState(
    val isLoading: Boolean = true,
    val profile: Map<String, String> = emptyMap(),
    val notes: List<CoachNote> = emptyList(),
    val targets: List<WeeklyTarget> = emptyList(),
    val groups: List<MuscleGroup> = emptyList(),
)

internal class MemoryViewModel(
    private val coachService: CoachService,
    volumeService: VolumeService,
    exerciseService: ExerciseService,
) : ViewModel() {

    val uiState: StateFlow<MemoryUiState> = combine(
        coachService.observeProfile(),
        coachService.observeNotes(),
        volumeService.observeTargets(),
        exerciseService.observeMuscleGroups(),
    ) { profile, notes, targets, groups -> MemoryUiState(false, profile, notes, targets, groups) }
        .stateIn(viewModelScope, WhileUiSubscribed, MemoryUiState())

    fun setProfile(field: String, value: String) {
        viewModelScope.launch { coachService.setProfileField(field, value) }
    }

    fun updateNote(id: Long, text: String) {
        viewModelScope.launch { coachService.updateNote(id, text) }
    }

    fun deleteNote(id: Long) {
        viewModelScope.launch { coachService.deleteNote(id) }
    }
}

@Composable
internal fun MemoryContent(state: MemoryUiState, actions: MemoryActions) {
    var editingField by rememberSaveable { mutableStateOf<String?>(null) }
    var editingNote by rememberSaveable { mutableStateOf<Long?>(null) }
    var deletingNote by rememberSaveable { mutableStateOf<Long?>(null) }
    LazyColumn(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text(stringResource(R.string.memory_intro), style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 8.dp)) }
        item { SectionTitle(R.string.memory_profile) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                    CoachService.PROFILE_FIELDS.forEach { field ->
                        MemoryRow(
                            label = profileLabel(field),
                            value = state.profile[field] ?: stringResource(R.string.memory_unknown),
                            editDescription = stringResource(R.string.memory_edit, profileLabel(field)),
                            icon = Icons.Default.Edit,
                            tag = CoachMemoryTags.profile(field),
                            onClick = { editingField = field },
                        )
                    }
                }
            }
        }
        item { SectionTitle(R.string.memory_targets) }
        item {
            Card(Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(targetsSummary(state), style = MaterialTheme.typography.bodyLarge)
                    }
                    TextButton(onClick = actions::openTargets) { Text(stringResource(R.string.memory_change)) }
                }
            }
        }
        item { SectionTitle(R.string.memory_notes) }
        if (state.notes.isEmpty()) item { Text(stringResource(R.string.memory_no_notes), style = MaterialTheme.typography.bodyMedium) }
        items(state.notes, key = { it.id }) { note ->
            Card(Modifier.fillMaxWidth().testTag(CoachMemoryTags.NOTE)) {
                Row(Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(vertical = 4.dp)) {
                        Text(formatDayMonth(note.createdAt.toLocalDateTime(TimeZone.currentSystemDefault()).date), style = MaterialTheme.typography.labelSmall)
                        Text(note.text, style = MaterialTheme.typography.bodyLarge)
                    }
                    IconButton(onClick = { editingNote = note.id }) { Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.memory_edit_note)) }
                    IconButton(onClick = { deletingNote = note.id }, Modifier.testTag(CoachMemoryTags.DELETE_NOTE)) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.memory_delete_note))
                    }
                }
            }
        }
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
private fun SectionTitle(title: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
}

@Composable
private fun MemoryRow(label: String, value: String, editDescription: String, icon: androidx.compose.ui.graphics.vector.ImageVector, tag: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.testTag("$tag:value"))
        }
        IconButton(onClick = onClick, Modifier.testTag(tag)) { Icon(icon, contentDescription = editDescription) }
    }
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
