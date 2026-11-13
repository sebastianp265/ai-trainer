package com.github.sebastianp265.aitrainer.shared.e2e

import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick

class BottomBarScreen(compose: ComposeTestRule) : Screen(compose) {

    fun open(tab: String) {
        waitForText(tab)
        tabNode(tab).performClick()
    }

    fun assertTabHidden(tab: String) =
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodes(hasText(tab) and hasClickAction()).fetchSemanticsNodes().isEmpty() }

    fun assertSelected(tab: String) = tabNode(tab).assertIsSelected()

    private fun tabNode(tab: String) = compose.onAllNodes(hasText(tab) and hasClickAction()).onFirst()
}
