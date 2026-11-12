package com.github.sebastianp265.aitrainer.feature.settings

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.sebastianp265.aitrainer.feature.coach.impl.CoachApiKeyTags
import com.github.sebastianp265.aitrainer.feature.coach.impl.CoachModelTags
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
    }

    fun goBackToTab() {
        goBack()
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

    fun assertCoachRowsHidden() = assertTextNotShown("Claude API key")

    fun openApiKey() {
        waitFor(hasTestTag(SettingsTags.API_KEY_ROW))
        compose.onNode(hasTestTag(SettingsTags.API_KEY_ROW)).performClick()
        waitFor(hasTestTag(CoachApiKeyTags.FIELD))
    }

    fun openModel() {
        waitFor(hasTestTag(SettingsTags.MODEL_ROW))
        compose.onNode(hasTestTag(SettingsTags.MODEL_ROW)).performClick()
    }

    fun openBackup() {
        scrollTo(hasTestTag(SettingsTags.BACKUP_ROW))
        compose.onNode(hasTestTag(SettingsTags.BACKUP_ROW)).performClick()
    }

    fun assertBackupShown() {
        waitForText("Export to a file")
        waitForText("Import from a file")
    }

    fun typeClaudeApiKey(key: String) {
        waitFor(hasTestTag(CoachApiKeyTags.FIELD))
        compose.onNode(hasTestTag(CoachApiKeyTags.FIELD)).performTextInput(key)
    }

    fun saveAndCheck() = compose.onNode(hasTestTag(CoachApiKeyTags.SAVE)).performClick()

    fun removeClaudeApiKey() {
        scrollTo(hasTestTag(CoachApiKeyTags.REMOVE))
        compose.onNode(hasTestTag(CoachApiKeyTags.REMOVE)).performClick()
    }

    fun assertKeyWorks() = waitForText("The key works", substring = true)

    fun assertKeyRejected() = waitForText("The key was rejected", substring = true)

    fun assertApiKeyScreenShown() = waitFor(hasTestTag(CoachApiKeyTags.FIELD))

    fun assertNoClaudeApiKeySaved() = waitForText("No API key saved")

    fun assertClaudeApiKeySaved() = waitForText("Saved, encrypted on this phone")

    fun assertNotShownAnywhere(text: String) = assertTextNotShown(text)

    fun chooseModel(model: String) {
        waitFor(hasTestTag(CoachModelTags.option(model)))
        compose.onNode(hasTestTag(CoachModelTags.option(model))).performClick()
    }

    fun assertModelShown(name: String) = waitForText(name)
}
