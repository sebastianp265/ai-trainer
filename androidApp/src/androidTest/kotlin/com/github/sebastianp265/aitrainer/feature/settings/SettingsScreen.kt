package com.github.sebastianp265.aitrainer.feature.settings

import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import com.github.sebastianp265.aitrainer.feature.settings.impl.SettingsTags
import com.github.sebastianp265.aitrainer.shared.e2e.Screen
import com.github.sebastianp265.aitrainer.shared.e2e.TEST_TIMEOUT_MS

class SettingsScreen(compose: ComposeTestRule) : Screen(compose) {

    fun open() {
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Settings").performClick()
        waitForText("AI trainer")
    }

    fun goBack() {
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodesWithContentDescription("Settings").fetchSemanticsNodes().isNotEmpty() }
    }

    fun switchCoach() {
        waitFor(hasTestTag(SettingsTags.COACH_SWITCH))
        compose.onNode(hasTestTag(SettingsTags.COACH_SWITCH)).performClick()
    }

    fun assertCoachOff() {
        waitForText("Off")
        waitForText("Without the coach the app works in full", substring = true)
    }

    fun assertCoachOn() = waitForText("Chat, plan proposals and weekly review")

    fun typeClaudeApiKey(key: String) = scrollTo(hasTestTag(SettingsTags.CLAUDE_API_KEY)).performTextInput(key)

    fun save() = clickText("Save")

    fun removeClaudeApiKey() = scrollTo(hasText("Remove")).performClick()

    fun assertClaudeApiKeySaved() = assertStatusShown("API key saved")

    fun assertNoClaudeApiKeySaved() = assertStatusShown("No API key saved")

    fun assertNotShownAnywhere(text: String) = assertTextNotShown(text)

    private fun assertStatusShown(status: String) =
        compose.waitUntil(TEST_TIMEOUT_MS) { runCatching { scrollTo(hasText(status)) }.isSuccess }

    private fun scrollTo(matcher: SemanticsMatcher): SemanticsNodeInteraction {
        compose.onNode(hasScrollAction()).performScrollToNode(matcher)
        return compose.onNode(matcher)
    }
}
