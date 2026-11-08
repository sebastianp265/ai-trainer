package com.github.sebastianp265.aitrainer.shared.e2e

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.github.sebastianp265.aitrainer.feature.workout.impl.LogWorkoutTags

class LogWorkoutScreen(
    compose: ComposeTestRule,
    private val startWorkoutText: String = "Start workout",
    private val addExerciseDescription: String = "Add exercise",
) : Screen(compose) {

    fun startWorkout() {
        waitForText(startWorkoutText)
        clickText(startWorkoutText)
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodesWithContentDescription(addExerciseDescription).fetchSemanticsNodes().isNotEmpty() }
    }

    fun openExercisePicker() = compose.onNodeWithContentDescription(addExerciseDescription).performClick()

    fun increaseWeight(exercise: String, setIndex: Int = 0, taps: Int = 1) {
        val button = compose.onAllNodes(hasContentDescription("Increase weight") and inExerciseCard(exercise))[setIndex]
        button.performTouchInput { repeat(taps) { click(); advanceEventTime(TAP_INTERVAL_MS) } }
    }

    fun decreaseReps(exercise: String) =
        compose.onNode(hasContentDescription("Decrease reps") and inExerciseCard(exercise)).performClick()

    fun addSet() = clickText("+ Set")

    fun clickSameAsLastTime() = clickText("Same as last time")

    fun finish() = clickText("Finish")

    fun discard() {
        clickText("Discard")
        compose.onNode(hasText("Discard") and hasAnyAncestor(isDialog())).performClick()
    }

    fun assertSets(exercise: String, vararg expected: Pair<String, String>) {
        try {
            compose.waitUntil(TEST_TIMEOUT_MS) { weightAndRepsPerSet(exercise) == expected.toList() }
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("$exercise: expected sets ${expected.toList()}, shown ${weightAndRepsPerSet(exercise)}", e)
        }
    }

    fun assertWorkoutSaved() = waitForText("Workout saved")

    fun assertHistoryEntry(summary: String) {
        waitForText(startWorkoutText)
        waitForCount(summary, 1)
    }

    private fun inExerciseCard(exercise: String): SemanticsMatcher =
        hasAnyAncestor(hasTestTag(LogWorkoutTags.exerciseCard(exercise)))

    private fun weightAndRepsPerSet(exercise: String): List<Pair<String, String>> {
        val weightOrReps = hasTestTag(LogWorkoutTags.WEIGHT) or hasTestTag(LogWorkoutTags.REPS)
        return compose.onAllNodes(weightOrReps and inExerciseCard(exercise), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { node -> node.config[SemanticsProperties.Text].joinToString("") { it.text } }
            .chunked(2) { (weight, reps) -> weight to reps }
    }

    private companion object {
        const val TAP_INTERVAL_MS = 200L
    }
}
