package com.github.sebastianp265.aitrainer.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.domain.model.Equipment
import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.ExercisePreference
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup
import com.github.sebastianp265.aitrainer.ui.components.BackTopBar
import com.github.sebastianp265.aitrainer.ui.displayName
import com.github.sebastianp265.aitrainer.ui.labelRes
import com.github.sebastianp265.aitrainer.ui.log.searchByDisplayOrEnglishName
import com.github.sebastianp265.aitrainer.ui.log.sortedPickerItems
import com.github.sebastianp265.aitrainer.ui.theme.AITrainerTheme
import org.koin.compose.viewmodel.koinViewModel

object ExercisePreferencesTags {
    fun row(exerciseName: String) = "exercise_preference:$exerciseName"
}

@Composable
fun ExercisePreferencesScreen(onBack: () -> Unit, viewModel: ExercisePreferencesViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ExercisePreferencesContent(state, onSetPreference = viewModel::setPreference, onBack = onBack)
}

@Composable
fun ExercisePreferencesContent(
    state: ExercisePreferencesUiState,
    onSetPreference: (Exercise, ExercisePreference) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(topBar = { BackTopBar(R.string.settings_exercise_preferences, onBack) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            if (state.isLoading) {
                CircularProgressIndicator(Modifier.align(Alignment.Center))
            } else {
                ExercisePreferenceList(state.exercises, onSetPreference)
            }
        }
    }
}

@Composable
private fun ExercisePreferenceList(exercises: List<Exercise>, onSetPreference: (Exercise, ExercisePreference) -> Unit) {
    var query by rememberSaveable { mutableStateOf("") }
    val resources = LocalResources.current
    val locale = LocalConfiguration.current.locales[0]
    val items = remember(exercises, resources, locale) { sortedPickerItems(exercises, { it.displayName(resources) }, locale) }
    Column {
        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )
        LazyColumn {
            items(items.searchByDisplayOrEnglishName(query), key = { it.exercise.id }) { item ->
                ListItem(
                    headlineContent = { Text(item.displayName) },
                    supportingContent = {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ExercisePreference.entries.forEach { preference ->
                                FilterChip(
                                    selected = item.exercise.preference == preference,
                                    onClick = { onSetPreference(item.exercise, preference) },
                                    label = { Text(stringResource(preference.labelRes())) },
                                )
                            }
                        }
                    },
                    modifier = Modifier.testTag(ExercisePreferencesTags.row(item.exercise.name)),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ExercisePreferencesPreview() {
    val exercises = listOf(
        Exercise(1, "Barbell Bench Press", listOf(MuscleGroup.CHEST), emptyList(), Equipment.BARBELL, ExercisePreference.FAVOURITE),
        Exercise(2, "Back Squat", listOf(MuscleGroup.QUADS), emptyList(), Equipment.BARBELL, ExercisePreference.AVOID),
    )
    AITrainerTheme {
        ExercisePreferencesContent(ExercisePreferencesUiState(isLoading = false, exercises = exercises), { _, _ -> }, {})
    }
}
