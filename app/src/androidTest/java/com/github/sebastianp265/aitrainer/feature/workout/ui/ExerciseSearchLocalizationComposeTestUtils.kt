package com.github.sebastianp265.aitrainer.feature.workout.ui

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule

fun ComposeTestRule.exerciseNamesInPicker(): List<String> =
    onAllNodes(hasTestTag(LogWorkoutTags.PICKER_EXERCISE_NAME), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .map { node -> node.config[SemanticsProperties.Text].joinToString("") { it.text } }
