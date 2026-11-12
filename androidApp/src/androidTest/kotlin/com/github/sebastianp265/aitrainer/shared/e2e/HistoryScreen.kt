package com.github.sebastianp265.aitrainer.shared.e2e

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule

class HistoryScreen(compose: ComposeTestRule) : Screen(compose) {

    fun open() {
        scrollTo(hasText("All"))
        clickText("All")
        waitForText("All workouts")
    }

    fun assertShown() = waitForText("All workouts")

    fun assertWorkoutGone(title: String) = waitForTextGone(title)

    fun openWorkout(title: String) = clickText(title)
}
