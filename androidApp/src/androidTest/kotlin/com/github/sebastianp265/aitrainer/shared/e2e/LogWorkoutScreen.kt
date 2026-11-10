package com.github.sebastianp265.aitrainer.shared.e2e

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.github.sebastianp265.aitrainer.feature.workout.impl.ActiveWorkoutTags

class LogWorkoutScreen(
    compose: ComposeTestRule,
    private val startWorkoutText: String = "Start workout",
    private val addExerciseText: String = "Add exercise",
) : Screen(compose) {

    fun startWorkout() {
        waitForText(startWorkoutText)
        clickText(startWorkoutText)
        waitForText(addExerciseText)
    }

    fun openExercisePicker() = clickText(addExerciseText)

    fun typeWeight(exercise: String, setIndex: Int, kg: String) {
        compose.onAllNodes(hasTestTag(ActiveWorkoutTags.WEIGHT) and inExerciseCard(exercise), useUnmergedTree = true)[setIndex].performClick()
        typeIntoNumberDialog(kg)
    }

    fun typeReps(exercise: String, setIndex: Int, reps: String) {
        compose.onAllNodes(hasTestTag(ActiveWorkoutTags.REPS) and inExerciseCard(exercise), useUnmergedTree = true)[setIndex].performClick()
        typeIntoNumberDialog(reps)
    }

    fun decreaseReps(exercise: String) =
        compose.onAllNodes(hasContentDescription("Decrease reps") and inExerciseCard(exercise))[0].performClick()

    fun chooseRpe(exercise: String, setIndex: Int, rpe: String) {
        compose.onAllNodes(hasTestTag(ActiveWorkoutTags.RPE) and inExerciseCard(exercise), useUnmergedTree = true)[setIndex].performClick()
        compose.onNode(hasText(rpe) and hasAnyAncestor(hasContentDescription("RPE of set ${setIndex + 1}"))).performClick()
    }

    fun toggleWarmup(exercise: String, setIndex: Int) {
        compose.onAllNodes(hasTestTag(ActiveWorkoutTags.WARMUP_TOGGLE) and inExerciseCard(exercise))[setIndex].performClick()
    }

    fun addSet() = clickText("+ Set")

    fun clickSameAsLastTime() = clickText("Same as last time")

    fun finish() = clickText("Finish")

    fun askToDiscard() = clickText("Discard")

    fun confirmDiscard() = compose.onNode(hasText("Discard") and hasAnyAncestor(isDialog())).performClick()

    fun keepWorkout() = clickText("Back to workout")

    fun assertSets(exercise: String, vararg expected: Pair<String, String>) {
        try {
            compose.waitUntil(TEST_TIMEOUT_MS) { weightAndRepsPerSet(exercise) == expected.toList() }
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("$exercise: expected sets ${expected.toList()}, shown ${weightAndRepsPerSet(exercise)}", e)
        }
    }

    fun assertRpe(exercise: String, setIndex: Int, expected: String) {
        val matcher = hasTestTag(ActiveWorkoutTags.RPE) and inExerciseCard(exercise)
        compose.waitUntil(TEST_TIMEOUT_MS) {
            val nodes = compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes()
            nodes.getOrNull(setIndex)?.config?.get(SemanticsProperties.Text)?.joinToString("") { it.text } == expected
        }
    }

    fun assertLastTime(exercise: String, expected: String) {
        val matcher = hasTestTag(ActiveWorkoutTags.LAST_TIME) and inExerciseCard(exercise)
        try {
            compose.waitUntil(TEST_TIMEOUT_MS) { lastTimeText(matcher) == expected }
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("$exercise: expected last time \"$expected\", shown \"${lastTimeText(matcher)}\"", e)
        }
    }

    fun assertNoLastTime(exercise: String) {
        compose.waitUntil(TEST_TIMEOUT_MS) {
            compose.onAllNodes(hasTestTag(ActiveWorkoutTags.LAST_TIME) and inExerciseCard(exercise), useUnmergedTree = true)
                .fetchSemanticsNodes().isEmpty()
        }
    }

    fun assertHistoryEntry(summary: String) {
        waitForText(startWorkoutText)
        waitForTextShownOnce(summary)
    }

    fun assertNoWorkoutsYet() {
        waitForText(startWorkoutText)
        waitForText("No workouts logged yet.")
    }

    private fun typeIntoNumberDialog(text: String) {
        val field = compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog()))
        field.performTextClearance()
        field.performTextInput(text)
        compose.onNode(hasText("OK") and hasAnyAncestor(isDialog())).performClick()
    }

    private fun lastTimeText(matcher: SemanticsMatcher): String? =
        compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().firstOrNull()
            ?.config?.get(SemanticsProperties.Text)?.joinToString("") { it.text }
            ?.replace(' ', ' ')

    private fun inExerciseCard(exercise: String): SemanticsMatcher =
        hasAnyAncestor(hasTestTag(ActiveWorkoutTags.exerciseCard(exercise)))

    private fun weightAndRepsPerSet(exercise: String): List<Pair<String, String>> {
        val weightOrReps = hasTestTag(ActiveWorkoutTags.WEIGHT) or hasTestTag(ActiveWorkoutTags.REPS)
        return compose.onAllNodes(weightOrReps and inExerciseCard(exercise), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { node -> node.config[SemanticsProperties.Text].joinToString("") { it.text } }
            .chunked(2) { (weight, reps) -> weight to reps }
    }
}
