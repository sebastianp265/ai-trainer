package com.github.sebastianp265.aitrainer.testing

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick

const val TEST_TIMEOUT_MS = 5_000L

fun ComposeTestRule.waitForText(text: String) =
    waitUntil(TEST_TIMEOUT_MS) { onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

fun ComposeTestRule.waitFor(matcher: SemanticsMatcher) =
    waitUntil(TEST_TIMEOUT_MS) { onAllNodes(matcher, useUnmergedTree = true).fetchSemanticsNodes().isNotEmpty() }

fun ComposeTestRule.openTab(label: String) = onNodeWithText(label).performClick()
