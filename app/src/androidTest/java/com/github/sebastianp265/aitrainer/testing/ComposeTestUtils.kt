package com.github.sebastianp265.aitrainer.testing

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.sebastianp265.aitrainer.ui.log.LogWorkoutTags

const val TEST_TIMEOUT_MS = 5_000L

fun ComposeTestRule.waitForText(text: String) =
    waitUntil(TEST_TIMEOUT_MS) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

fun ComposeTestRule.waitFor(matcher: SemanticsMatcher) =
    waitUntil(TEST_TIMEOUT_MS) { onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

fun ComposeTestRule.openTab(label: String) = onNodeWithText(label).performClick()

fun ComposeTestRule.startWorkout() {
    waitForText("Start workout")
    onNodeWithText("Start workout").performClick()
    waitUntil(TEST_TIMEOUT_MS) { onAllNodesWithContentDescription("Add exercise").fetchSemanticsNodes().isNotEmpty() }
}

fun ComposeTestRule.openExercisePicker(searchQuery: String) {
    onNodeWithContentDescription("Add exercise").performClick()
    onNode(hasSetTextAction()).performTextInput(searchQuery)
}

fun ComposeTestRule.exerciseNamesInPicker(): List<String> =
    onAllNodes(hasTestTag(LogWorkoutTags.PICKER_EXERCISE_NAME), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .map { node -> node.config[SemanticsProperties.Text].joinToString("") { it.text } }
