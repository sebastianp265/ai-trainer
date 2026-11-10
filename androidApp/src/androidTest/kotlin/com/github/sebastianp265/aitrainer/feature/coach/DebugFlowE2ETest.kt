package com.github.sebastianp265.aitrainer.feature.coach

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.feature.settings.SettingsScreen
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BottomBarScreen
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class DebugFlowE2ETest : AbstractE2ETest() {

    private val tabs = BottomBarScreen(compose)
    private val coach = CoachScreen(compose)
    private val settings = SettingsScreen(compose)

    @BeforeTest
    fun start() = launchApp()

    @Test
    fun `the debug screen switches to a fake coach that answers without a network`() {
        settings.open()
        settings.openDebug()
        settings.switchFakeClaude()
        settings.goBack()
        settings.openApiKey()
        settings.typeClaudeApiKey("test-key")
        settings.saveAndCheck()
        settings.assertKeyWorks()
        settings.goBack()
        settings.goBackToTab()
        tabs.open("Coach")

        coach.send("Hello coach")

        coach.assertCoachSaid("Debug answer: the fake coach is on.")
        settings.open()
        settings.openDebug()
        settings.assertDebugLogShown()
    }
}
