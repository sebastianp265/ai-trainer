package com.github.sebastianp265.aitrainer.testing

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import com.github.sebastianp265.aitrainer.ui.log.LogWorkoutTags

private const val TIMEOUT_MS = 5_000L

fun ComposeTestRule.waitForText(text: String) =
    waitUntil(TIMEOUT_MS) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

fun ComposeTestRule.waitForCount(text: String, count: Int) =
    waitUntil(TIMEOUT_MS) { onAllNodesWithText(text).fetchSemanticsNodes().size == count }

fun ComposeTestRule.waitFor(matcher: SemanticsMatcher) =
    waitUntil(TIMEOUT_MS) { onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

fun ComposeTestRule.openTab(label: String) = onNodeWithText(label).performClick()

fun ComposeTestRule.scrollTo(matcher: SemanticsMatcher): SemanticsNodeInteraction {
    onNode(hasScrollAction()).performScrollToNode(matcher)
    return onNode(matcher)
}

fun ComposeTestRule.startWorkout() {
    waitForText("Start workout")
    onNodeWithText("Start workout").performClick()
    waitUntil(TIMEOUT_MS) { onAllNodesWithContentDescription("Add exercise").fetchSemanticsNodes().isNotEmpty() }
}

fun ComposeTestRule.openExercisePicker(searchQuery: String) {
    onNodeWithContentDescription("Add exercise").performClick()
    onNode(hasSetTextAction()).performTextInput(searchQuery)
}

fun ComposeTestRule.addExercise(searchQuery: String, exerciseToPick: String) {
    openExercisePicker(searchQuery)
    waitForText(exerciseToPick)
    onNode(hasText(exerciseToPick) and hasAnyAncestor(isDialog())).performClick()
}

fun ComposeTestRule.exerciseNamesInPicker(nameSuffix: String): List<String> =
    onAllNodes(hasAnyAncestor(isDialog()), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .flatMap { it.config.getOrElse(SemanticsProperties.Text) { emptyList() } }
        .map { it.text }
        .filter { it.endsWith(nameSuffix) }

fun inExerciseCard(exerciseName: String) = hasAnyAncestor(hasTestTag(LogWorkoutTags.exerciseCard(exerciseName)))

fun ComposeTestRule.weightAndRepsPerSetInCard(exerciseName: String): List<Pair<String, String>> {
    fun values(tag: String) = onAllNodes(hasTestTag(tag) and inExerciseCard(exerciseName), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .map { node -> node.config[SemanticsProperties.Text].joinToString("") { it.text } }
    val weights = values(LogWorkoutTags.WEIGHT)
    val reps = values(LogWorkoutTags.REPS)
    check(weights.size == reps.size) { "weights $weights vs reps $reps" }
    return weights.zip(reps)
}

fun ComposeTestRule.assertSetsInCard(exerciseName: String, vararg expected: Pair<String, String>) {
    try {
        waitUntil(TIMEOUT_MS) { weightAndRepsPerSetInCard(exerciseName) == expected.toList() }
    } catch (e: ComposeTimeoutException) {
        throw AssertionError("$exerciseName: expected sets ${expected.toList()}, shown ${weightAndRepsPerSetInCard(exerciseName)}", e)
    }
}

fun SemanticsNodeInteraction.tapRapidly(times: Int) = performTouchInput {
    repeat(times) { click(); advanceEventTime(200) }
}
