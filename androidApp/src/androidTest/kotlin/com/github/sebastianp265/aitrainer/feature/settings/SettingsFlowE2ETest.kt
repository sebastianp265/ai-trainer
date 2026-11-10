package com.github.sebastianp265.aitrainer.feature.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeFailureKind
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BottomBarScreen
import com.github.sebastianp265.aitrainer.shared.e2e.FakeClaudeClient
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class SettingsFlowE2ETest : AbstractE2ETest() {

    private val settings = SettingsScreen(compose)
    private val tabs = BottomBarScreen(compose)
    private val claude = FakeClaudeClient()

    @BeforeTest
    fun start() = launchApp(claudeClient = claude)

    @Test
    fun `Settings open from the gear on every tab and return`() {
        listOf("Plan", "Coach", "Progress", "Training").forEach { tab ->
            tabs.open(tab)
            settings.open()
            settings.goBackToTab()
            tabs.assertSelected(tab)
        }
    }

    @Test
    fun `the coach is on at first and switching it off hides its settings and its tab`() {
        settings.open()
        settings.assertCoachOn()

        settings.switchCoach()

        settings.assertCoachOff()
        settings.assertCoachRowsHidden()
        settings.goBackToTab()
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
    fun `a rejected key is reported`() {
        claude.keyCheckResult = ClaudeFailureKind.KEY_REJECTED
        settings.open()
        settings.openApiKey()

        settings.typeClaudeApiKey("bad-key")
        settings.saveAndCheck()

        settings.assertKeyRejected()
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
}
