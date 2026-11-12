package com.github.sebastianp265.aitrainer.shared.e2e

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.github.sebastianp265.aitrainer.feature.plan.impl.PlanTags
import com.github.sebastianp265.aitrainer.feature.workout.impl.LogWorkoutTags
import com.github.sebastianp265.aitrainer.feature.workout.impl.TrainingTab

class PlanScreen(compose: ComposeTestRule) : Screen(compose) {

    fun showPlans() {
        waitFor(hasTestTag(LogWorkoutTags.tab(TrainingTab.PLANS)))
        compose.onNode(hasTestTag(LogWorkoutTags.tab(TrainingTab.PLANS))).performClick()
    }

    fun assertNoPlansYet() {
        showPlans()
        waitForText("No plans yet", substring = true)
    }

    fun startWritingOwnPlan() {
        showPlans()
        scrollTo(hasTestTag(LogWorkoutTags.NEW_PLAN))
        compose.onNode(hasTestTag(LogWorkoutTags.NEW_PLAN)).performClick()
    }

    fun typeName(name: String) {
        waitFor(hasTestTag(PlanTags.NAME_FIELD))
        compose.onNode(hasTestTag(PlanTags.NAME_FIELD)).performTextInput(name)
    }

    fun assertCannotSave() {
        waitFor(hasTestTag(PlanTags.SAVE))
        compose.onNode(hasTestTag(PlanTags.SAVE)).assertIsNotEnabled()
    }

    fun openExercisePicker() {
        waitFor(hasTestTag(PlanTags.ADD_EXERCISE))
        compose.onAllNodes(hasTestTag(PlanTags.ADD_EXERCISE), useUnmergedTree = true)[0].performClick()
    }

    fun typeWeight(setIndex: Int, kg: String) {
        waitFor(hasTestTag(PlanTags.SET_WEIGHT))
        val field = compose.onAllNodes(hasTestTag(PlanTags.SET_WEIGHT))[setIndex]
        field.performTextClearance()
        field.performTextInput(kg)
    }

    fun save() {
        compose.onNode(hasTestTag(PlanTags.SAVE)).assertIsEnabled()
        compose.onNode(hasTestTag(PlanTags.SAVE)).performClick()
    }

    fun assertDetailShown(planName: String, vararg lines: String) {
        waitForText(planName)
        waitForText("Start workout")
        lines.forEach { waitForText(it) }
    }

    fun askCoach() = clickText("Change with the coach")

    fun edit() = compose.onNode(hasContentDescription("Edit")).performClick()

    fun delete() = compose.onNode(hasContentDescription("Delete plan")).performClick()

    fun confirmDelete() = compose.onNode(hasText("Remove") and hasAnyAncestor(isDialog())).performClick()

    fun back() = compose.onNode(hasContentDescription("Back")).performClick()

    fun assertListed(planName: String) = waitFor(listed(planName))

    fun assertNotListed(planName: String) =
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodes(listed(planName), useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }

    fun open(planName: String) {
        showPlans()
        scrollTo(listed(planName))
        compose.onNode(listed(planName)).performClick()
    }

    private fun listed(planName: String) = hasTestTag(LogWorkoutTags.nextPlan(planName)) or hasTestTag(LogWorkoutTags.planRow(planName))
}
