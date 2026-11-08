package com.github.sebastianp265.aitrainer.shared.e2e

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.sebastianp265.aitrainer.feature.exercise.impl.ExercisePickerTags

class ExercisePickerScreen(compose: ComposeTestRule) : Screen(compose) {

    fun search(query: String) = compose.onNode(hasSetTextAction()).performTextInput(query)

    fun pick(exercise: String) {
        waitForText(exercise)
        compose.onNode(hasText(exercise) and hasAnyAncestor(isDialog())).performClick()
    }

    fun assertResultsInOrder(vararg expected: String) {
        try {
            compose.waitUntil(TEST_TIMEOUT_MS) { shownNames() == expected.toList() }
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("expected exercises ${expected.toList()}, shown ${shownNames()}", e)
        }
    }

    private fun shownNames(): List<String> =
        compose.onAllNodes(hasTestTag(ExercisePickerTags.EXERCISE_NAME), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { node -> node.config[SemanticsProperties.Text].joinToString("") { it.text } }
}
