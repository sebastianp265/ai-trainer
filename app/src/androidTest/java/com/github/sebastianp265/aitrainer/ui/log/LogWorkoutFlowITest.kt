package com.github.sebastianp265.aitrainer.ui.log

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.testing.AppUnderTest
import com.github.sebastianp265.aitrainer.testing.addExercise
import com.github.sebastianp265.aitrainer.testing.assertSetsInCard
import com.github.sebastianp265.aitrainer.testing.inExerciseCard
import com.github.sebastianp265.aitrainer.testing.startWorkout
import com.github.sebastianp265.aitrainer.testing.tapRapidly
import com.github.sebastianp265.aitrainer.testing.waitForCount
import com.github.sebastianp265.aitrainer.testing.waitForText
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class LogWorkoutFlowITest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val app = AppUnderTest()

    @BeforeTest
    fun start() = app.startInEnglishWithFreshData()

    @AfterTest
    fun close() = app.close()

    @Test
    fun loggedWorkoutIsSavedToHistory() {
        logFinishedBenchWorkout()

        compose.waitForText("Workout saved")
        compose.waitForText("Start workout")
        compose.waitForCount("1 exercise · 2 sets", 1)
    }

    @Test
    fun nextWorkoutStartsFromLastWorkoutNumbers() {
        logFinishedBenchWorkout()

        compose.startWorkout()
        compose.addExercise(searchQuery = "Bench Press", exerciseToPick = "Barbell Bench Press")
        compose.assertSetsInCard("Barbell Bench Press", "7.5" to "9")

        compose.onNodeWithText("Same as last time").performClick()
        compose.assertSetsInCard("Barbell Bench Press", "7.5" to "9", "7.5" to "9", "10" to "9")

        compose.onNodeWithText("Discard").performClick()
        compose.onNode(hasText("Discard") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForText("Start workout")
        compose.waitForCount("1 exercise · 2 sets", 1)
    }

    private fun logFinishedBenchWorkout() {
        compose.startWorkout()
        compose.addExercise(searchQuery = "Bench Press", exerciseToPick = "Barbell Bench Press")
        compose.assertSetsInCard("Barbell Bench Press", "0" to "10")

        val increaseWeight = hasContentDescription("Increase weight") and inExerciseCard("Barbell Bench Press")
        compose.onNode(increaseWeight).tapRapidly(times = 3)
        compose.onNode(hasContentDescription("Decrease reps") and inExerciseCard("Barbell Bench Press")).performClick()
        compose.assertSetsInCard("Barbell Bench Press", "7.5" to "9")

        compose.onNodeWithText("+ Set").performClick()
        compose.assertSetsInCard("Barbell Bench Press", "7.5" to "9", "7.5" to "9")
        compose.onAllNodes(increaseWeight)[1].performClick()
        compose.assertSetsInCard("Barbell Bench Press", "7.5" to "9", "10" to "9")

        compose.onNodeWithText("Finish").performClick()
    }
}
