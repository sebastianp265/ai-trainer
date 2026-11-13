package com.github.sebastianp265.aitrainer.feature.progress

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import com.github.sebastianp265.aitrainer.feature.progress.impl.ProgressTags
import com.github.sebastianp265.aitrainer.shared.e2e.Screen
import com.github.sebastianp265.aitrainer.shared.e2e.TEST_TIMEOUT_MS
import kotlinx.datetime.LocalDate

class ProgressScreen(compose: ComposeTestRule) : Screen(compose) {

    fun open() {
        waitForText("Progress")
        clickText("Progress")
    }

    fun assertConnectPromptShown() =
        waitForText("Connect to see weight, calories, sleep and heart rate here. You choose which data to share.")

    fun assertCardShown(title: String) = waitForText(title)

    fun assertCardHidden(title: String) = assertTextNotShown(title)

    fun openWeightDetails() {
        waitForText("Weight · 7-day average")
        compose.onAllNodes(hasText("Details") and hasClickAction()).onFirst().performClick()
    }

    fun openStrengthExercise(exercise: String) {
        waitForText(exercise)
        clickText(exercise)
    }

    fun assertVolumeCardShown() {
        scrollToVolumeCard()
        waitForText("Sets per muscle group · weekly average")
    }

    fun assertVolumeTarget(text: String) {
        scrollToVolumeCard()
        waitForText(text)
    }

    fun assertNoVolumeTargets() {
        scrollToVolumeCard()
        assertTextNotShown("target ")
    }

    fun openTargets() {
        scrollToVolumeCard()
        compose.onNode(hasTestTag(ProgressTags.VOLUME_TARGETS_LINK)).performClick()
    }

    private fun scrollToVolumeCard() {
        waitFor(hasTestTag(ProgressTags.VOLUME_CARD).or(hasText("Details")))
        compose.onNode(hasScrollAction()).performScrollToNode(hasTestTag(ProgressTags.VOLUME_CARD))
    }

    fun assertWeightDetailsShown() {
        waitForText("7-day average")
        waitForText("lowest")
        waitForText("highest")
    }

    fun openDay(date: LocalDate) {
        waitFor(hasTestTag(ProgressTags.healthDay(date)))
        compose.onNode(hasTestTag(ProgressTags.healthDay(date)), useUnmergedTree = true).performClick()
    }

    fun assertDayDetailsShown(vararg rows: String) {
        waitForText("everything we know about this day")
        rows.forEach { waitForText(it) }
    }

    fun assertDay(date: LocalDate, expected: String) =
        waitFor(hasTestTag(ProgressTags.healthDay(date)) and hasText(expected))

    fun assertDayGone(date: LocalDate) =
        compose.waitUntil(TEST_TIMEOUT_MS) {
            compose.onAllNodes(hasTestTag(ProgressTags.healthDay(date)), useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
}
