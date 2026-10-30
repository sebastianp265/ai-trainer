package com.github.sebastianp265.aitrainer.ui.settings

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performScrollToNode
import com.github.sebastianp265.aitrainer.testing.openTab
import com.github.sebastianp265.aitrainer.testing.waitForText

fun ComposeTestRule.openSettings() {
    waitForText("Settings")
    openTab("Settings")
    waitForText("Training profile")
}

fun ComposeTestRule.scrollTo(matcher: SemanticsMatcher): SemanticsNodeInteraction {
    onNode(hasScrollAction()).performScrollToNode(matcher)
    return onNode(matcher)
}
