package com.github.sebastianp265.aitrainer.feature.history

import androidx.compose.ui.test.junit4.ComposeTestRule
import com.github.sebastianp265.aitrainer.shared.e2e.Screen

class HistoryScreen(compose: ComposeTestRule) : Screen(compose) {

    fun open() {
        waitForText("All")
        clickText("All")
        waitForText("All workouts")
    }

    fun filterOtherApps() = clickText("From other apps")

    fun filterGym() = clickText("Gym")

    fun assertWorkoutListed(title: String) = waitForText(title)

    fun assertWorkoutNotListed(title: String) = assertTextNotShown(title)

    fun openWorkout(title: String) = clickText(title)

    fun assertReadOnlyDetails(vararg values: String) {
        waitForText("Read only")
        values.forEach { waitForText(it) }
    }
}
