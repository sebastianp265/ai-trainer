package com.github.sebastianp265.aitrainer.shared.e2e

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.sebastianp265.aitrainer.feature.exercise.impl.ExercisePickerTags

class ExercisePickerScreen(compose: ComposeTestRule) : Screen(compose) {

    fun search(query: String) {
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodes(hasSetTextAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(hasSetTextAction()).performTextInput(query)
    }

    fun pick(exercise: String) {
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodes(exerciseName(exercise), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(exerciseName(exercise), useUnmergedTree = true)[0].performClick()
    }

    fun filterByGroup(group: String) {
        waitForText(group)
        compose.onNode(hasText(group) and isSelectable()).performClick()
    }

    fun createExercise(name: String, primaryGroup: String, vararg secondaryGroups: String) {
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodes(verticalList).fetchSemanticsNodes().isNotEmpty() }
        compose.onNode(verticalList).performScrollToNode(hasText("New exercise"))
        clickText("New exercise")
        compose.onNode(hasSetTextAction() and hasAnyAncestor(isDialog())).performTextInput(name)
        compose.onAllNodes(hasText(primaryGroup) and hasAnyAncestor(isDialog()))[0].performClick()
        secondaryGroups.forEach { group -> compose.onAllNodes(hasText(group) and hasAnyAncestor(isDialog())).onLast().performClick() }
        compose.onNode(hasText("Save") and hasAnyAncestor(isDialog())).performClick()
    }

    fun assertShown(exercise: String) =
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodes(exerciseName(exercise), useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    fun assertNotShown(exercise: String) =
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodes(exerciseName(exercise), useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }

    fun assertRecentlyUsedShown(exercise: String) {
        waitForText("Recently used")
        assertShown(exercise)
    }

    private val verticalList = hasScrollAction() and SemanticsMatcher.keyIsDefined(SemanticsProperties.VerticalScrollAxisRange)

    private fun exerciseName(exercise: String) = hasTestTag(ExercisePickerTags.EXERCISE_NAME) and hasText(exercise)

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
