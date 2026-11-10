package com.github.sebastianp265.aitrainer.feature.progress

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
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

    fun clickConnect() = clickText("Connect")

    fun assertConnectCardHidden() =
        compose.waitUntil(TEST_TIMEOUT_MS) { compose.onAllNodesWithText("Health Connect").fetchSemanticsNodes().isEmpty() }

    fun assertCardShown(title: String) = waitForText(title)

    fun assertCardHidden(title: String) = assertTextNotShown(title)

    fun assertDay(date: LocalDate, expected: String) =
        waitFor(hasTestTag(ProgressTags.healthDay(date)) and hasText(expected))

    fun assertDayGone(date: LocalDate) =
        compose.waitUntil(TEST_TIMEOUT_MS) {
            compose.onAllNodes(hasTestTag(ProgressTags.healthDay(date)), useUnmergedTree = true).fetchSemanticsNodes().isEmpty()
        }
}
