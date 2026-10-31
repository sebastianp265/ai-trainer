package com.github.sebastianp265.aitrainer.feature.progress.ui

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onAllNodesWithText
import com.github.sebastianp265.aitrainer.testing.TEST_TIMEOUT_MS
import com.github.sebastianp265.aitrainer.testing.openTab
import com.github.sebastianp265.aitrainer.testing.waitFor
import com.github.sebastianp265.aitrainer.testing.waitForText
import kotlinx.datetime.LocalDate

fun ComposeTestRule.openProgress() {
    waitForText("Progress")
    openTab("Progress")
}

fun ComposeTestRule.assertHealthDay(date: LocalDate, expected: String) =
    waitFor(hasTestTag(ProgressTags.healthDay(date)) and hasText(expected))

fun ComposeTestRule.assertHealthDaySources(date: LocalDate, expected: String) =
    waitFor(hasTestTag(ProgressTags.healthDaySources(date)) and hasText(expected))

fun ComposeTestRule.waitForTextToDisappear(text: String) =
    waitUntil(TEST_TIMEOUT_MS) { onAllNodesWithText(text).fetchSemanticsNodes().isEmpty() }
