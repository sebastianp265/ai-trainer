package com.github.sebastianp265.aitrainer.feature.settings

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.github.sebastianp265.aitrainer.feature.ai.impl.AiSettingsTags
import com.github.sebastianp265.aitrainer.feature.ai.impl.ApiKeyTags
import com.github.sebastianp265.aitrainer.feature.ai.impl.ModelTags
import com.github.sebastianp265.aitrainer.feature.ai.impl.ModelTuningTags
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.isSelected
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import com.github.sebastianp265.aitrainer.feature.settings.impl.SettingsTags
import com.github.sebastianp265.aitrainer.shared.e2e.Screen
import com.github.sebastianp265.aitrainer.shared.e2e.TEST_TIMEOUT_MS

class SettingsScreen(compose: ComposeTestRule) : Screen(compose) {

    fun open() {
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodes(hasText("Settings") and hasClickAction()).fetchSemanticsNodes().isNotEmpty() }
        compose.onAllNodes(hasText("Settings") and hasClickAction()).onFirst().performClick()
        waitForText("AI trainer")
    }

    fun goBack() {
        compose.onNodeWithContentDescription("Back").performClick()
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

    fun assertCoachRowsHidden() =
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodes(hasTestTag(SettingsTags.AI_ROW)).fetchSemanticsNodes().isEmpty() }

    fun openApiKey() {
        openAiSettings()
        compose.onNode(hasTestTag(AiSettingsTags.API_KEY_ROW)).performClick()
        waitFor(hasTestTag(ApiKeyTags.FIELD))
    }

    fun openModel() {
        openAiSettings()
        compose.onNode(hasTestTag(AiSettingsTags.MODEL_ROW)).performClick()
    }

    private fun openAiSettings() {
        waitFor(hasTestTag(SettingsTags.AI_ROW) or hasTestTag(AiSettingsTags.API_KEY_ROW))
        if (compose.onAllNodes(hasTestTag(SettingsTags.AI_ROW)).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNode(hasTestTag(SettingsTags.AI_ROW)).performClick()
        }
        waitFor(hasTestTag(AiSettingsTags.API_KEY_ROW))
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
        waitFor(hasTestTag(ApiKeyTags.FIELD))
        compose.onNode(hasTestTag(ApiKeyTags.FIELD)).performTextInput(key)
    }

    fun saveAndCheck() = compose.onNode(hasTestTag(ApiKeyTags.SAVE)).performClick()

    fun removeClaudeApiKey() {
        scrollTo(hasTestTag(ApiKeyTags.REMOVE))
        compose.onNode(hasTestTag(ApiKeyTags.REMOVE)).performClick()
    }

    fun assertKeyWorks() = waitForText("The key works", substring = true)

    fun assertKeyRejected() = waitForText("The key was rejected", substring = true)

    fun assertKeyNotChecked() = waitForText("no connection to check it", substring = true)

    fun assertApiKeyScreenShown() = waitFor(hasTestTag(ApiKeyTags.FIELD))

    fun assertNoClaudeApiKeySaved() = waitForText("No API key saved")

    fun assertClaudeApiKeySaved() = waitForText("Saved, encrypted on this phone")

    fun assertNotShownAnywhere(text: String) = assertTextNotShown(text)

    fun chooseModel(model: String) {
        waitFor(hasTestTag(ModelTags.option(model)))
        compose.onNode(hasTestTag(ModelTags.option(model))).performClick()
    }

    fun assertModelShown(name: String) = waitForText(name)

    fun openTuning() {
        waitFor(hasTestTag(ModelTuningTags.TOGGLE))
        compose.onNode(hasTestTag(ModelTuningTags.TOGGLE)).performClick()
    }

    fun chooseTuning(setting: String, value: String) {
        waitFor(hasTestTag(ModelTuningTags.option(setting, value)))
        compose.onNode(hasTestTag(ModelTuningTags.option(setting, value))).performScrollTo().performClick()
    }

    fun assertTuningChosen(setting: String, value: String) {
        waitFor(hasTestTag(ModelTuningTags.option(setting, value)) and isSelected())
    }

    fun assertTuningUnavailable(setting: String, value: String) {
        waitFor(hasTestTag(ModelTuningTags.option(setting, value)) and isNotEnabled())
    }

    fun waitForTextOnScreen(text: String) = waitForText(text)

    fun typeMaxTokens(value: String) {
        waitFor(hasTestTag(ModelTuningTags.MAX_TOKENS))
        compose.onNode(hasTestTag(ModelTuningTags.MAX_TOKENS)).performScrollTo().performTextReplacement(value)
    }

    fun assertMaxTokens(value: String) {
        waitFor(hasTestTag(ModelTuningTags.MAX_TOKENS) and hasText(value))
    }
}
