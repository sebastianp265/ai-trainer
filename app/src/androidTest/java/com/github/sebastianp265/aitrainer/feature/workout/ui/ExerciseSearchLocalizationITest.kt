package com.github.sebastianp265.aitrainer.feature.workout.ui

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.testing.AbstractITest
import com.github.sebastianp265.aitrainer.testing.waitForText
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ExerciseSearchLocalizationITest : AbstractITest() {

    override val appLanguage = "pl"

    @Test
    fun polishSearchShowsPolishNameMatchesFirstThenEnglishNameMatches() {
        launchApp()
        compose.waitForText("Rozpocznij trening")
        compose.onNodeWithText("Rozpocznij trening").performClick()
        compose.waitUntil { compose.onAllNodesWithContentDescription("Dodaj ćwiczenie").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithContentDescription("Dodaj ćwiczenie").performClick()

        compose.onNode(hasSetTextAction()).performTextInput("fly")
        compose.waitForText("Rozpiętki na wyciągu")

        assertEquals(
            listOf("Rozpiętki na maszynie (butterfly)", "Odwrotne rozpiętki z hantlami", "Rozpiętki na wyciągu"),
            compose.exerciseNamesInPicker(),
        )
    }
}
