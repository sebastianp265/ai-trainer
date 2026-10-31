package com.github.sebastianp265.aitrainer.feature.settings.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.testing.AbstractITest
import com.github.sebastianp265.aitrainer.testing.waitForText
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class SettingsFlowITest : AbstractITest() {

    @BeforeTest
    fun start() = launchApp()

    @Test
    fun claudeApiKeyIsSavedButNeverShown() {
        compose.openSettings()
        compose.scrollTo(hasText("No API key saved"))
        compose.scrollTo(hasTestTag(SettingsTags.CLAUDE_API_KEY)).performTextInput("test-key")
        compose.onNodeWithText("Save").performClick()
        compose.waitForText("API key saved")
        assertEquals(0, compose.onAllNodesWithText("test-key", substring = true).fetchSemanticsNodes().size)

        restartApp()
        compose.openSettings()

        compose.scrollTo(hasText("API key saved"))
        assertEquals(0, compose.onAllNodesWithText("test-key", substring = true).fetchSemanticsNodes().size)
        compose.scrollTo(hasText("Remove")).performClick()
        compose.waitForText("No API key saved")
    }
}
