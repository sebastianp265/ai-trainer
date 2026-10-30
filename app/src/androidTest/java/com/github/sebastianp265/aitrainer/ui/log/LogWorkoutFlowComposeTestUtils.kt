package com.github.sebastianp265.aitrainer.ui.log

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.github.sebastianp265.aitrainer.domain.model.ExerciseKey
import com.github.sebastianp265.aitrainer.testing.TEST_TIMEOUT_MS
import com.github.sebastianp265.aitrainer.testing.openExercisePicker
import com.github.sebastianp265.aitrainer.testing.startWorkout
import com.github.sebastianp265.aitrainer.testing.waitForText

fun ComposeTestRule.logFinishedBenchWorkout() {
    startWorkout()
    addExercise(searchQuery = "Bench Press", exerciseToPick = "Barbell Bench Press")
    assertSetsInCard(ExerciseKey.BARBELL_BENCH_PRESS, "0" to "10")

    val increaseWeight = hasContentDescription("Increase weight") and inExerciseCard(ExerciseKey.BARBELL_BENCH_PRESS)
    onNode(increaseWeight).tapRapidly(times = 3)
    onNode(hasContentDescription("Decrease reps") and inExerciseCard(ExerciseKey.BARBELL_BENCH_PRESS)).performClick()
    assertSetsInCard(ExerciseKey.BARBELL_BENCH_PRESS, "7.5" to "9")

    onNodeWithText("+ Set").performClick()
    assertSetsInCard(ExerciseKey.BARBELL_BENCH_PRESS, "7.5" to "9", "7.5" to "9")
    onAllNodes(increaseWeight)[1].performClick()
    assertSetsInCard(ExerciseKey.BARBELL_BENCH_PRESS, "7.5" to "9", "10" to "9")

    onNodeWithText("Finish").performClick()
}

fun ComposeTestRule.waitForCount(text: String, count: Int) =
    waitUntil(TEST_TIMEOUT_MS) { onAllNodesWithText(text).fetchSemanticsNodes().size == count }

fun ComposeTestRule.addExercise(searchQuery: String, exerciseToPick: String) {
    openExercisePicker(searchQuery)
    waitForText(exerciseToPick)
    onNode(hasText(exerciseToPick) and hasAnyAncestor(isDialog())).performClick()
}

fun inExerciseCard(exercise: ExerciseKey): SemanticsMatcher = hasAnyAncestor(hasTestTag(LogWorkoutTags.exerciseCard(exercise)))

fun ComposeTestRule.assertSetsInCard(exercise: ExerciseKey, vararg expected: Pair<String, String>) {
    try {
        waitUntil(TEST_TIMEOUT_MS) { weightAndRepsPerSetInCard(exercise) == expected.toList() }
    } catch (e: ComposeTimeoutException) {
        throw AssertionError("$exercise: expected sets ${expected.toList()}, shown ${weightAndRepsPerSetInCard(exercise)}", e)
    }
}

fun SemanticsNodeInteraction.tapRapidly(times: Int) = performTouchInput {
    repeat(times) { click(); advanceEventTime(200) }
}

private fun ComposeTestRule.weightAndRepsPerSetInCard(exercise: ExerciseKey): List<Pair<String, String>> {
    fun values(tag: String) = onAllNodes(hasTestTag(tag) and inExerciseCard(exercise), useUnmergedTree = true)
        .fetchSemanticsNodes()
        .map { node -> node.config[SemanticsProperties.Text].joinToString("") { it.text } }
    val weights = values(LogWorkoutTags.WEIGHT)
    val reps = values(LogWorkoutTags.REPS)
    check(weights.size == reps.size) { "weights $weights vs reps $reps" }
    return weights.zip(reps)
}
