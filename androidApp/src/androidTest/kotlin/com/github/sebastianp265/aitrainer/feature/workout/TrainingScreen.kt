package com.github.sebastianp265.aitrainer.feature.workout

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyDescendant
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.github.sebastianp265.aitrainer.feature.workout.impl.LogWorkoutTags
import com.github.sebastianp265.aitrainer.feature.workout.impl.TrainingTab
import com.github.sebastianp265.aitrainer.shared.e2e.Screen
import kotlin.test.assertEquals

class TrainingScreen(compose: ComposeTestRule) : Screen(compose) {

    fun assertFirstLaunch() {
        waitForText("Get to know your coach")
        waitFor(hasTestTag(LogWorkoutTags.START_EMPTY))
        assertNoRecentWorkouts()
    }

    fun assertNoRecentWorkouts() {
        showTab(TrainingTab.HISTORY)
        scrollTo(hasText("You have no workouts yet"))
    }

    fun startEmpty() {
        waitFor(hasTestTag(LogWorkoutTags.START_EMPTY))
        compose.onNode(hasTestTag(LogWorkoutTags.START_EMPTY)).performClick()
    }

    fun startPlan(planName: String) {
        waitFor(hasTestTag(LogWorkoutTags.nextPlan(planName)) or hasTestTag(LogWorkoutTags.planRow(planName)))
        if (compose.onAllNodes(hasTestTag(LogWorkoutTags.nextPlan(planName))).fetchSemanticsNodes().isNotEmpty()) {
            compose.onNode(hasTestTag(LogWorkoutTags.NEXT_PLAN_START)).performClick()
        } else {
            showTab(TrainingTab.PLANS)
            scrollTo(hasContentDescription("Start $planName"))
            compose.onNode(hasContentDescription("Start $planName")).performClick()
        }
    }

    fun assertPlansOffered(nextPlan: String, vararg otherPlans: String) {
        waitFor(hasTestTag(LogWorkoutTags.nextPlan(nextPlan)))
        showTab(TrainingTab.PLANS)
        val shown = compose.onAllNodes(hasTestTagStartingWith(LogWorkoutTags.planRow(""))).fetchSemanticsNodes()
            .map { it.config[SemanticsProperties.TestTag] }
        assertEquals((listOf(nextPlan) + otherPlans).map(LogWorkoutTags::planRow), shown)
    }

    fun editPlan(planName: String) {
        showTab(TrainingTab.PLANS)
        scrollTo(hasTestTag(LogWorkoutTags.editPlan(planName)))
        compose.onNode(hasTestTag(LogWorkoutTags.editPlan(planName))).performClick()
    }

    fun assertPlanRowDoesNotOpenOnTap(planName: String) {
        showTab(TrainingTab.PLANS)
        scrollTo(hasTestTag(LogWorkoutTags.planRow(planName)))
        compose.onNode(hasTestTag(LogWorkoutTags.planRow(planName))).performClick()
        compose.onNode(hasTestTag(LogWorkoutTags.planRow(planName))).assertIsDisplayed()
    }

    fun assertRecentWorkout(title: String) {
        showTab(TrainingTab.HISTORY)
        scrollTo(sessionRowWith(title))
    }

    fun openRecentWorkout(title: String) {
        assertRecentWorkout(title)
        compose.onAllNodes(sessionRowWith(title))[0].performClick()
    }

    fun resumeWorkout() {
        waitFor(hasTestTag(LogWorkoutTags.ACTIVE_WORKOUT))
        compose.onNode(hasTestTag(LogWorkoutTags.ACTIVE_WORKOUT_RESUME)).performClick()
    }

    private fun showTab(tab: TrainingTab) {
        waitFor(hasTestTag(LogWorkoutTags.tab(tab)))
        compose.onNode(hasTestTag(LogWorkoutTags.tab(tab))).performClick()
    }
}

private fun sessionRowWith(text: String) =
    hasTestTag(LogWorkoutTags.SESSION_ROW) and (hasText(text, substring = true) or hasAnyDescendant(hasText(text, substring = true)))

private fun hasTestTagStartingWith(prefix: String) = SemanticsMatcher("test tag starts with $prefix") { node ->
    node.config.getOrElseNullable(SemanticsProperties.TestTag) { null }?.startsWith(prefix) == true
}
