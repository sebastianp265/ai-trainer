package com.github.sebastianp265.aitrainer.feature.workout

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.github.sebastianp265.aitrainer.feature.workout.impl.LogWorkoutTags
import com.github.sebastianp265.aitrainer.shared.e2e.Screen
import com.github.sebastianp265.aitrainer.shared.e2e.TEST_TIMEOUT_MS

class TrainingScreen(compose: ComposeTestRule) : Screen(compose) {

    fun assertFirstLaunch() {
        waitForText("Get to know your coach")
        waitForText("Or start on your own")
        waitForText("You have no workouts yet")
    }

    fun assertNextPlan(planName: String) {
        waitFor(hasTestTag(LogWorkoutTags.NEXT_PLAN))
        waitForText(planName)
    }

    fun startNextPlan() {
        waitFor(hasTestTag(LogWorkoutTags.START_NEXT_PLAN))
        compose.onNode(hasTestTag(LogWorkoutTags.START_NEXT_PLAN), useUnmergedTree = true).performClick()
    }

    fun openOtherWorkouts() {
        waitFor(hasTestTag(LogWorkoutTags.OTHER_WORKOUT))
        compose.onNode(hasTestTag(LogWorkoutTags.OTHER_WORKOUT), useUnmergedTree = true).performClick()
    }

    fun assertSheetOffers(vararg planNames: String) {
        waitForText("From a plan")
        planNames.forEach { waitFor(hasTestTag(LogWorkoutTags.sheetPlan(it))) }
        waitFor(hasTestTag(LogWorkoutTags.SHEET_EMPTY))
    }

    fun startFromSheet(planName: String) {
        compose.onNode(hasTestTag(LogWorkoutTags.sheetPlan(planName)), useUnmergedTree = true).performClick()
    }

    fun startEmptyFromSheet() {
        compose.onNode(hasTestTag(LogWorkoutTags.SHEET_EMPTY), useUnmergedTree = true).performClick()
    }

    fun assertRecentWorkout(title: String) {
        waitFor(hasTestTag(LogWorkoutTags.RECENT_SESSION))
        waitForText(title)
    }

    fun openRecentWorkout(title: String) {
        waitFor(hasTestTag(LogWorkoutTags.RECENT_SESSION))
        compose.onAllNodes(hasTestTag(LogWorkoutTags.RECENT_SESSION) and hasText(title), useUnmergedTree = false)[0].performClick()
    }

    fun assertNoNextPlan() =
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodes(hasTestTag(LogWorkoutTags.NEXT_PLAN)).fetchSemanticsNodes().isEmpty() }
}
