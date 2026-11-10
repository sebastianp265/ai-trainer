package com.github.sebastianp265.aitrainer.feature.workout

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import com.github.sebastianp265.aitrainer.feature.workout.impl.ReportTags
import com.github.sebastianp265.aitrainer.shared.e2e.Screen
import com.github.sebastianp265.aitrainer.shared.e2e.TEST_TIMEOUT_MS

class WorkoutReportScreen(compose: ComposeTestRule) : Screen(compose) {

    fun assertSummaryShown() {
        waitForText("Workout saved")
        waitFor(hasTestTag(ReportTags.DONE))
    }

    fun done() = compose.onNode(hasTestTag(ReportTags.DONE)).performClick()

    fun showAllSets() = clickText("See all sets")

    fun assertRecordShown(exercise: String) {
        waitFor(hasTestTag(ReportTags.RECORDS))
        waitForText("New records")
        waitForText(exercise)
    }

    fun assertNoRecords() = assertTextNotShown("New records")

    fun assertPlanComparison(difference: String) {
        waitForText("Compared to plan")
        try {
            compose.waitUntil(TEST_TIMEOUT_MS) { shownDifferences().contains(difference) }
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("expected difference \"$difference\", shown ${shownDifferences()}", e)
        }
    }

    private fun shownDifferences(): List<String> =
        compose.onAllNodes(hasTestTag(ReportTags.DIFFERENCE), useUnmergedTree = true).fetchSemanticsNodes()
            .map { node -> node.config[SemanticsProperties.Text].joinToString("") { it.text } }

    fun assertNoPlanComparison() = assertTextNotShown("Compared to plan")

    fun assertDetailSets(count: Int) {
        compose.waitUntil(TEST_TIMEOUT_MS) {
            compose.onAllNodes(hasTestTag(ReportTags.SET_LINE), useUnmergedTree = true).fetchSemanticsNodes().size == count
        }
    }

    fun edit() {
        waitFor(hasTestTag(ReportTags.EDIT))
        compose.onNode(hasTestTag(ReportTags.EDIT)).performClick()
    }

    fun delete() = compose.onNode(hasContentDescription("Delete workout")).performClick()

    fun confirmDelete() = compose.onNode(hasText("Remove") and hasAnyAncestor(isDialog())).performClick()

    fun back() = compose.onNode(hasContentDescription("Back")).performClick()
}
