package com.github.sebastianp265.aitrainer.shared.e2e

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlin.test.assertEquals

const val TEST_TIMEOUT_MS = 5_000L

abstract class Screen(protected val compose: ComposeTestRule) {

    protected fun waitForText(text: String, substring: Boolean = false) =
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodesWithText(text, substring = substring).fetchSemanticsNodes().isNotEmpty() }

    protected fun waitForTextShownOnce(text: String) =
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodesWithText(text).fetchSemanticsNodes().size == 1 }

    protected fun waitFor(matcher: SemanticsMatcher) =
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

    protected fun clickText(text: String) = compose.onNodeWithText(text).performClick()

    protected fun assertTextNotShown(text: String) =
        assertEquals(0, compose.onAllNodesWithText(text, substring = true).fetchSemanticsNodes().size, "\"$text\" is shown")
}
