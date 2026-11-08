package com.github.sebastianp265.aitrainer.feature.settings

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class SettingsFlowE2ETest : AbstractE2ETest() {

    private val settings = SettingsScreen(compose)

    @BeforeTest
    fun start() = launchApp()

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
