package com.github.sebastianp265.aitrainer.feature.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.integration.ai.AiFailureKind
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BottomBarScreen
import com.github.sebastianp265.aitrainer.shared.e2e.FakeAiClient
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class SettingsFlowE2ETest : AbstractE2ETest() {

    private val settings = SettingsScreen(compose)
    private val tabs = BottomBarScreen(compose)
    private val claude = FakeAiClient()

    @BeforeTest
    fun start() = launchApp(aiClient = claude)

    @Test
    fun `the coach is on at first and switching it off hides its settings and its tab`() {
        settings.open()
        settings.assertCoachOn()

        settings.switchCoach()

        settings.assertCoachOff()
        settings.assertCoachRowsHidden()
        tabs.assertTabHidden("Coach")
    }

    @Test
    fun `Claude API key is checked and saved and never shown`() {
        settings.open()
        settings.openApiKey()
        settings.assertNoClaudeApiKeySaved()
        settings.typeClaudeApiKey("test-key")
        settings.saveAndCheck()
        settings.assertKeyWorks()
        settings.assertNotShownAnywhere("test-key")

        settings.goBack()
        settings.assertClaudeApiKeySaved()
        settings.assertNotShownAnywhere("test-key")
        settings.openApiKey()
        settings.removeClaudeApiKey()
        settings.assertNoClaudeApiKeySaved()
    }

    @Test
    fun `a rejected key is reported and not saved`() {
        claude.keyCheckResult = AiFailureKind.KEY_REJECTED
        settings.open()
        settings.openApiKey()

        settings.typeClaudeApiKey("bad-key")
        settings.saveAndCheck()

        settings.assertKeyRejected()
        settings.goBack()
        settings.assertNoClaudeApiKeySaved()
    }

    @Test
    fun `a key that cannot be checked is not saved`() {
        claude.keyCheckResult = AiFailureKind.NO_NETWORK
        settings.open()
        settings.openApiKey()

        settings.typeClaudeApiKey("test-key")
        settings.saveAndCheck()

        settings.assertKeyNotChecked()
        settings.goBack()
        settings.assertNoClaudeApiKeySaved()
    }

    @Test
    fun `the backup screen opens from Settings and offers export and import`() {
        settings.open()

        settings.openBackup()

        settings.assertBackupShown()
    }

    @Test
    fun `the chosen model is shown in Settings`() {
        settings.open()
        settings.openModel()

        settings.chooseModel("claude-haiku-5-5")
        settings.goBack()

        settings.assertModelShown("Claude Haiku 5.5")
    }

    @Test
    fun `a typed answer limit is kept and one above the highest is not saved`() {
        settings.open()
        settings.openModel()
        settings.openTuning()

        settings.typeMaxTokens("3000")
        settings.goBack()
        settings.openModel()
        settings.openTuning()
        settings.assertMaxTokens("3000")
        settings.typeMaxTokens("99999")
        settings.goBack()
        settings.openModel()
        settings.openTuning()

        settings.assertMaxTokens("3000")
    }

    @Test
    fun `the chosen tuning is kept and thinking cannot be turned off on Opus`() {
        settings.open()
        settings.openModel()
        settings.openTuning()

        settings.chooseTuning("effort", "LOW")
        settings.chooseModel("claude-opus-5-5")
        settings.goBack()
        settings.openModel()
        settings.openTuning()

        settings.assertTuningChosen("effort", "LOW")
        settings.assertMaxTokens("16000")
        settings.assertTuningUnavailable("thinking", "off")
        settings.waitForTextOnScreen("This model at this effort always thinks.")
    }
}
