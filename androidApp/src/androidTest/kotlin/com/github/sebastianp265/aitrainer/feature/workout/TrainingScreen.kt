package com.github.sebastianp265.aitrainer.feature.workout

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.github.sebastianp265.aitrainer.feature.workout.impl.LogWorkoutTags
import com.github.sebastianp265.aitrainer.shared.e2e.Screen
import androidx.test.espresso.Espresso
import kotlin.test.assertEquals
import com.github.sebastianp265.aitrainer.shared.e2e.TEST_TIMEOUT_MS

class TrainingScreen(compose: ComposeTestRule) : Screen(compose) {

    fun assertFirstLaunch() {
        waitForText("Get to know your coach")
        waitFor(hasTestTag(LogWorkoutTags.START_WORKOUT))
        assertNoRecentWorkouts()
    }

    fun assertNoRecentWorkouts() {
        scrollTo(hasText("You have no workouts yet"))
        waitForText("You have no workouts yet")
    }

    fun openStartSheet() {
        waitFor(hasTestTag(LogWorkoutTags.START_WORKOUT))
        compose.onNode(hasTestTag(LogWorkoutTags.START_WORKOUT)).performClick()
        waitFor(hasTestTag(LogWorkoutTags.SHEET_EMPTY))
    }

    fun startPlan(planName: String) {
        openStartSheet()
        waitFor(hasTestTag(LogWorkoutTags.sheetPlan(planName)))
        compose.onNode(hasTestTag(LogWorkoutTags.sheetPlan(planName))).performClick()
    }

    fun assertSheetOffers(vararg planNames: String) {
        waitForText("From a plan")
        val shown = compose.onAllNodes(hasTestTagStartingWith(LogWorkoutTags.sheetPlan(""))).fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.TestTag] }
        assertEquals(planNames.map(LogWorkoutTags::sheetPlan), shown)
        waitFor(hasTestTag(LogWorkoutTags.SHEET_EMPTY))
    }

    fun closeStartSheet() = Espresso.pressBack()

    fun startEmptyFromSheet() {
        compose.onNode(hasTestTag(LogWorkoutTags.SHEET_EMPTY), useUnmergedTree = true).performClick()
    }

    fun assertRecentWorkout(title: String) {
        scrollTo(hasTestTag(LogWorkoutTags.RECENT_SESSION))
        waitForText(title)
    }

    fun openRecentWorkout(title: String) {
        scrollTo(hasTestTag(LogWorkoutTags.RECENT_SESSION))
        compose.onAllNodes(hasTestTag(LogWorkoutTags.RECENT_SESSION) and hasText(title), useUnmergedTree = false)[0].performClick()
    }
}

private fun hasTestTagStartingWith(prefix: String) = SemanticsMatcher("test tag starts with $prefix") { node ->
    node.config.getOrElseNullable(SemanticsProperties.TestTag) { null }?.startsWith(prefix) == true
}
