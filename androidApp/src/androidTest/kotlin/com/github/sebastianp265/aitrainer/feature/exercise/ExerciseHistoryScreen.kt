package com.github.sebastianp265.aitrainer.feature.exercise

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.github.sebastianp265.aitrainer.shared.e2e.Screen

class ExerciseHistoryScreen(compose: ComposeTestRule) : Screen(compose) {

    fun assertShownFor(exerciseName: String, workouts: String) {
        waitForText(exerciseName)
        waitForText(workouts, substring = true)
    }

    fun assertSessionShown(sets: String) = waitForText(sets)

    fun assertStat(label: String) = waitForText(label)
}
