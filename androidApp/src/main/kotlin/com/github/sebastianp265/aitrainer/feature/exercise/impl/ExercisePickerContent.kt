package com.github.sebastianp265.aitrainer.feature.exercise.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.integration.exercise.Equipment
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup
import com.github.sebastianp265.aitrainer.integration.workout.LastTimeGroup
import com.github.sebastianp265.aitrainer.integration.workout.RecentExercise
import com.github.sebastianp265.aitrainer.shared.workout.LastTimeLabels
import com.github.sebastianp265.aitrainer.shared.workout.rememberLastTimeLabels
import com.github.sebastianp265.aitrainer.shared.workout.text
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ExercisePickerContent(
    state: ExercisePickerUiState,
    onClose: () -> Unit,
    onPick: (String) -> Unit,
    onCreate: (name: String, primaryGroupKey: String, secondaryGroupKeys: List<String>) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var groupKey by rememberSaveable { mutableStateOf<String?>(null) }
    var creating by rememberSaveable { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val labels = rememberLastTimeLabels()
    val items = remember(state.exercises, locale) {
        sortedExerciseListItems(state.exercises, { it.displayName(locale) }, { it.englishName() }, locale)
    }
    val visible = items.searchByDisplayThenEnglishName(query).inPrimaryGroup(groupKey)
    val recentByKey = state.recent.associateBy { it.exerciseKey }
    val showRecent = query.isBlank()
    val recentItems = if (showRecent) visible.filter { it.exercise.key in recentByKey }.sortedBy { state.recent.indexOfFirst { r -> r.exerciseKey == it.exercise.key } } else emptyList()
    val otherItems = visible - recentItems.toSet()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_exercise)) },
                navigationIcon = {
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close)) }
                },
            )
        },
    ) { padding ->
        if (state.isLoading) {
            CircularProgressIndicator(Modifier.padding(padding).padding(16.dp))
            return@Scaffold
        }
        LazyColumn(Modifier.padding(padding).fillMaxSize(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text(stringResource(R.string.search_exercise)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            item {
                LazyRow(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(selected = groupKey == null, onClick = { groupKey = null }, label = { Text(stringResource(R.string.filter_all)) })
                    }
                    items(state.groups, key = { it.key }) { group ->
                        FilterChip(
                            selected = groupKey == group.key,
                            onClick = { groupKey = group.key },
                            label = { Text(group.displayName(locale)) },
                        )
                    }
                }
            }
            if (recentItems.isNotEmpty()) {
                item { SectionTitle(stringResource(R.string.recently_used)) }
                items(recentItems, key = { "recent:" + it.exercise.key }) { item ->
                    ExerciseRow(item, locale, recentByKey.getValue(item.exercise.key).lastTime, labels) { onPick(item.exercise.key) }
                }
            }
            item { SectionTitle(stringResource(R.string.all_exercises)) }
            items(otherItems, key = { it.exercise.key }) { item ->
                ExerciseRow(item, locale, null, labels) { onPick(item.exercise.key) }
            }
            item {
                OutlinedButton(onClick = { creating = true }, Modifier.fillMaxWidth().padding(top = 16.dp)) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Text(stringResource(R.string.new_exercise))
                }
            }
        }
    }
    if (creating) {
        NewExerciseDialog(
            groups = state.groups,
            onCreate = { name, primary, secondary ->
                creating = false
                onCreate(name, primary, secondary)
            },
            onDismiss = { creating = false },
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, Modifier.padding(top = 12.dp, bottom = 4.dp), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun ExerciseRow(item: ExerciseListItem, locale: Locale, lastTime: List<LastTimeGroup>?, labels: LastTimeLabels, onClick: () -> Unit) {
    val exercise = item.exercise
    val groups = (exercise.primaryMuscles + exercise.secondaryMuscles).joinToString(", ") { it.displayName(locale) }
    val subtitle = if (lastTime.isNullOrEmpty()) groups
    else stringResource(R.string.exercise_with_last_time, groups, lastTime.joinToString(" · ") { it.text(labels) })
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClick = onClick).padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(item.displayName, Modifier.testTag(ExercisePickerTags.EXERCISE_NAME), style = MaterialTheme.typography.bodyLarge)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.Default.Add, contentDescription = null)
    }
}

@Composable
private fun NewExerciseDialog(
    groups: List<MuscleGroup>,
    onCreate: (name: String, primaryGroupKey: String, secondaryGroupKeys: List<String>) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf("") }
    var primary by rememberSaveable { mutableStateOf<String?>(null) }
    var secondary by rememberSaveable { mutableStateOf(emptyList<String>()) }
    val locale = LocalConfiguration.current.locales[0]
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.new_exercise)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.exercise_name)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(stringResource(R.string.primary_group), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    groups.forEach { group ->
                        FilterChip(
                            selected = primary == group.key,
                            onClick = { primary = group.key; secondary = secondary - group.key },
                            label = { Text(group.displayName(locale)) },
                        )
                    }
                }
                Text(stringResource(R.string.secondary_groups), style = MaterialTheme.typography.labelLarge)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    groups.filter { it.key != primary }.forEach { group ->
                        FilterChip(
                            selected = group.key in secondary,
                            onClick = { secondary = if (group.key in secondary) secondary - group.key else secondary + group.key },
                            label = { Text(group.displayName(locale)) },
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { primary?.let { onCreate(name, it, secondary) } },
                enabled = name.isNotBlank() && primary != null,
            ) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@PreviewLightDark
@Composable
private fun ExercisePickerPreview() {
    val chest = MuscleGroup("CHEST", mapOf("en" to "Chest"))
    val arms = MuscleGroup("ARMS", mapOf("en" to "Arms"))
    val dip = Exercise("DIP", mapOf("en" to "Dip"), Equipment("BODYWEIGHT", mapOf("en" to "Bodyweight")), listOf(chest), listOf(arms))
    MaterialTheme {
        ExercisePickerContent(
            state = ExercisePickerUiState(
                isLoading = false,
                exercises = listOf(dip),
                groups = listOf(chest, arms),
                recent = listOf(RecentExercise("DIP", listOf(LastTimeGroup(3, 10, 0.0, null, null)))),
            ),
            onClose = {},
            onPick = {},
            onCreate = { _, _, _ -> },
        )
    }
}
