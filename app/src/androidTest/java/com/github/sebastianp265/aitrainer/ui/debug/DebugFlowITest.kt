package com.github.sebastianp265.aitrainer.ui.debug

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.testing.AppUnderTest
import com.github.sebastianp265.aitrainer.testing.addExercise
import com.github.sebastianp265.aitrainer.testing.assertSetsInCard
import com.github.sebastianp265.aitrainer.testing.openTab
import com.github.sebastianp265.aitrainer.testing.scrollTo
import com.github.sebastianp265.aitrainer.testing.startWorkout
import com.github.sebastianp265.aitrainer.testing.waitForText
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class DebugFlowITest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val app = AppUnderTest()

    @BeforeTest
    fun start() = app.startInEnglishWithFreshData()

    @AfterTest
    fun close() = app.close()

    @Test
    fun demoDataFillsHistoryAndPrefillsTheNextWorkout() {
        compose.openTab("Settings")
        compose.waitForText("Training profile")
        compose.scrollTo(hasText("Debug tools")).performClick()
        compose.onNodeWithText("Replace data with demo data").performClick()
        compose.onNode(hasText("Replace") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForText("Demo data created")

        compose.onNodeWithContentDescription("Back").performClick()
        compose.openTab("Log")
        compose.waitForText("4 exercises · 12 sets")

        compose.startWorkout()
        compose.addExercise(searchQuery = "Squat", exerciseToPick = "Back Squat")
        compose.assertSetsInCard("Back Squat", "92.5" to "8")
    }
}
