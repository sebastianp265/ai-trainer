package com.github.sebastianp265.aitrainer.ui.settings

import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.testing.AppUnderTest
import com.github.sebastianp265.aitrainer.testing.exerciseNamesInPicker
import com.github.sebastianp265.aitrainer.testing.openExercisePicker
import com.github.sebastianp265.aitrainer.testing.openTab
import com.github.sebastianp265.aitrainer.testing.scrollTo
import com.github.sebastianp265.aitrainer.testing.startWorkout
import com.github.sebastianp265.aitrainer.testing.waitFor
import com.github.sebastianp265.aitrainer.testing.waitForText
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class SettingsFlowITest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val app = AppUnderTest()

    @BeforeTest
    fun start() = app.startInEnglishWithFreshData()

    @AfterTest
    fun close() = app.close()

    @Test
    fun trainingProfileIsKeptAfterRestart() {
        openSettings()
        compose.onNodeWithText("Cut").performClick()
        compose.onNodeWithContentDescription("Increase sessions per week").performClick()
        compose.onNodeWithContentDescription("Decrease session length").performClick()
        compose.waitFor(hasTestTag(SettingsTags.SESSIONS_PER_WEEK) and hasText("4"))
        compose.waitFor(hasTestTag(SettingsTags.SESSION_LENGTH) and hasText("45"))
        compose.scrollTo(hasText("Calves")).performClick()
        compose.scrollTo(hasText("Kettlebell")).performClick()
        compose.scrollTo(hasTestTag(SettingsTags.NOTES)).performTextInput("Bad left knee")

        app.restart()
        openSettings()

        compose.onNodeWithText("Cut").assertIsSelected()
        compose.onNodeWithText("Maintain").assertIsNotSelected()
        compose.onNodeWithTag(SettingsTags.SESSIONS_PER_WEEK, useUnmergedTree = true).assertTextEquals("4")
        compose.onNodeWithTag(SettingsTags.SESSION_LENGTH, useUnmergedTree = true).assertTextEquals("45")
        compose.scrollTo(hasText("Calves")).assertIsSelected()
        compose.scrollTo(hasText("Biceps")).assertIsNotSelected()
        compose.scrollTo(hasText("Kettlebell")).assertIsNotSelected()
        compose.scrollTo(hasText("Barbell")).assertIsSelected()
        compose.scrollTo(hasTestTag(SettingsTags.NOTES)).assertTextContains("Bad left knee")
    }

    @Test
    fun claudeApiKeyIsSavedButNeverShown() {
        openSettings()
        compose.scrollTo(hasText("No API key saved"))
        compose.scrollTo(hasTestTag(SettingsTags.CLAUDE_API_KEY)).performTextInput("test-key")
        compose.onNodeWithText("Save").performClick()
        compose.waitForText("API key saved")
        assertEquals(0, compose.onAllNodesWithText("test-key", substring = true).fetchSemanticsNodes().size)

        app.restart()
        openSettings()

        compose.scrollTo(hasText("API key saved"))
        assertEquals(0, compose.onAllNodesWithText("test-key", substring = true).fetchSemanticsNodes().size)
        compose.scrollTo(hasText("Remove")).performClick()
        compose.waitForText("No API key saved")
    }

    @Test
    fun avoidedExerciseIsListedLastInTheExercisePicker() {
        openSettings()
        compose.scrollTo(hasText("Exercise preferences")).performClick()
        compose.onNode(hasSetTextAction()).performTextInput("Bench")
        val benchRow = hasAnyAncestor(hasTestTag(ExercisePreferencesTags.row("Barbell Bench Press")))
        compose.waitFor(hasText("Avoid") and benchRow)
        compose.onNode(hasText("Avoid") and benchRow).performClick()
        compose.waitUntil { runCatching { compose.onNode(hasText("Avoid") and benchRow).assertIsSelected() }.isSuccess }

        compose.onNodeWithContentDescription("Back").performClick()
        compose.openTab("Log")
        compose.startWorkout()
        compose.openExercisePicker(searchQuery = "Bench")
        compose.waitForText("Barbell Bench Press")

        assertEquals(
            listOf("Close-Grip Bench Press", "Dumbbell Bench Press", "Incline Barbell Bench Press", "Barbell Bench Press"),
            compose.exerciseNamesInPicker(nameSuffix = "Bench Press"),
        )
    }

    private fun openSettings() {
        compose.waitForText("Settings")
        compose.openTab("Settings")
        compose.waitForText("Training profile")
    }
}
