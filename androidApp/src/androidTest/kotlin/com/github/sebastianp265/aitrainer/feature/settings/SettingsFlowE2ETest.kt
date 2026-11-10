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
    fun `Claude API key is saved but never shown`() {
        settings.open()
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
