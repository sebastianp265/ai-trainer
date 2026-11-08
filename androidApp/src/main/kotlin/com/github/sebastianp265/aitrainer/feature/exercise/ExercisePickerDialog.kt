package com.github.sebastianp265.aitrainer.feature.exercise

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.feature.exercise.impl.ExercisePickerTags
import com.github.sebastianp265.aitrainer.feature.exercise.impl.englishName
import com.github.sebastianp265.aitrainer.feature.exercise.impl.searchByDisplayThenEnglishName
import com.github.sebastianp265.aitrainer.feature.exercise.impl.sortedExerciseListItems
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import org.koin.compose.koinInject

@Composable
internal fun ExercisePickerDialog(onPick: (Exercise) -> Unit, onDismiss: () -> Unit) {
    val exerciseService = koinInject<ExerciseService>()
    val exercises by produceState(emptyList()) { value = exerciseService.exercises() }
    var query by rememberSaveable { mutableStateOf("") }
    val locale = LocalConfiguration.current.locales[0]
    val items = remember(exercises, locale) {
        sortedExerciseListItems(exercises, { it.displayName(locale) }, { it.englishName() }, locale)
    }
    val filtered = items.searchByDisplayThenEnglishName(query)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_exercise)) },
        text = {
            Column {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    label = { Text(stringResource(R.string.search)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                LazyColumn(Modifier.height(360.dp)) {
                    items(filtered, key = { it.exercise.key }) { item ->
                        ListItem(
                            headlineContent = { Text(item.displayName, Modifier.testTag(ExercisePickerTags.EXERCISE_NAME)) },
                            supportingContent = {
                                Text(item.exercise.primaryMuscles.joinToString { it.displayName(locale) })
                            },
                            modifier = Modifier.clickable { onPick(item.exercise) },
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
