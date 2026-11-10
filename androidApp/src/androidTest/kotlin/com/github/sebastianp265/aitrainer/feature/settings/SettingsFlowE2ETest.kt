package com.github.sebastianp265.aitrainer.feature.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BottomBarScreen
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class SettingsFlowE2ETest : AbstractE2ETest() {

    private val settings = SettingsScreen(compose)
    private val tabs = BottomBarScreen(compose)

    @BeforeTest
    fun start() = launchApp()

    @Test
    fun `Settings open from the gear on every tab and return`() {
        listOf("Plan", "Progress", "Training").forEach { tab ->
            tabs.open(tab)
            settings.open()
            settings.goBack()
            tabs.assertSelected(tab)
        }
    }

    @Test
    fun `the coach is off at first and the coach settings appear when it is switched on`() {
        settings.open()
        settings.assertCoachOff()
        settings.assertNotShownAnywhere("Claude API key")

        settings.switchCoach()

        settings.assertCoachOn()
        settings.assertNoClaudeApiKeySaved()
    }

    @Test
    fun `the coach switch is remembered after restart`() {
        settings.open()
        settings.switchCoach()

        restartApp()
        settings.open()

        settings.assertCoachOn()
    }

    @Test
    fun `Claude API key is saved but never shown`() {
        settings.open()
        settings.switchCoach()
        settings.assertNoClaudeApiKeySaved()
        settings.typeClaudeApiKey("test-key")
        settings.save()
        settings.assertClaudeApiKeySaved()
        settings.assertNotShownAnywhere("test-key")

        restartApp()
        settings.open()
        settings.assertClaudeApiKeySaved()
        settings.assertNotShownAnywhere("test-key")
        settings.removeClaudeApiKey()
        settings.assertNoClaudeApiKeySaved()
    }
}
