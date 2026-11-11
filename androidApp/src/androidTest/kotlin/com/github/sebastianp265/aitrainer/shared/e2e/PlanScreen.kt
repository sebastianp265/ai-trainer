package com.github.sebastianp265.aitrainer.shared.e2e

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.github.sebastianp265.aitrainer.feature.plan.impl.PlanTags

class PlanScreen(compose: ComposeTestRule) : Screen(compose) {

    fun assertNoPlansYet() = waitForText("You have no plans yet")

    fun startWritingOwnPlan() {
        waitFor(hasTestTag(PlanTags.NEW_PLAN))
        compose.onNode(hasTestTag(PlanTags.NEW_PLAN), useUnmergedTree = true).performClick()
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

    fun assertWeekShown() {
        waitFor(hasTestTag(PlanTags.WEEK_STRIP))
        waitForText("This week", substring = true)
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

    fun assertListed(planName: String) {
        waitFor(hasScrollAction())
        compose.onNode(hasScrollAction()).performScrollToNode(hasTestTag(PlanTags.card(planName)))
        waitFor(hasTestTag(PlanTags.card(planName)))
    }

    fun assertNotListed(planName: String) =
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodes(hasTestTag(PlanTags.card(planName)), useUnmergedTree = true).fetchSemanticsNodes().isEmpty() }

    fun open(planName: String) {
        waitFor(hasTestTag(PlanTags.card(planName)))
        compose.onNode(hasTestTag(PlanTags.card(planName)), useUnmergedTree = true).performClick()
    }

    fun assertListedUnder(section: String, planName: String) {
        waitForText(section)
        assertListed(planName)
    }
}
