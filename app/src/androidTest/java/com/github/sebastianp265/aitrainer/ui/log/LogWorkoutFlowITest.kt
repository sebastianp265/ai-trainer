package com.github.sebastianp265.aitrainer.ui.log

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.domain.model.ExerciseKey
import com.github.sebastianp265.aitrainer.testing.AbstractITest
import com.github.sebastianp265.aitrainer.testing.startWorkout
import com.github.sebastianp265.aitrainer.testing.waitForText
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class LogWorkoutFlowITest : AbstractITest() {

    @BeforeTest
    fun start() = launchApp()

    @Test
    fun loggedWorkoutIsSavedToHistory() {
        compose.logFinishedBenchWorkout()

        compose.waitForText("Workout saved")
        compose.waitForText("Start workout")
        compose.waitForCount("1 exercise · 2 sets", 1)
    }

    @Test
    fun nextWorkoutStartsFromLastWorkoutNumbers() {
        compose.logFinishedBenchWorkout()

        compose.startWorkout()
        compose.addExercise(searchQuery = "Bench Press", exerciseToPick = "Barbell Bench Press")
        compose.assertSetsInCard(ExerciseKey.BARBELL_BENCH_PRESS, "7.5" to "9")

        compose.onNodeWithText("Same as last time").performClick()
        compose.assertSetsInCard(ExerciseKey.BARBELL_BENCH_PRESS, "7.5" to "9", "7.5" to "9", "10" to "9")

        compose.onNodeWithText("Discard").performClick()
        compose.onNode(hasText("Discard") and hasAnyAncestor(isDialog())).performClick()
        compose.waitForText("Start workout")
        compose.waitForCount("1 exercise · 2 sets", 1)
    }
}
