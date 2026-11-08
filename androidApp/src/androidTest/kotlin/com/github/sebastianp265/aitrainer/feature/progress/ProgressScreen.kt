package com.github.sebastianp265.aitrainer.feature.progress

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import com.github.sebastianp265.aitrainer.feature.progress.impl.ProgressTags
import com.github.sebastianp265.aitrainer.shared.e2e.Screen
import kotlinx.datetime.LocalDate

class ProgressScreen(compose: ComposeTestRule) : Screen(compose) {

    fun open() {
        waitForText("Progress")
        clickText("Progress")
    }

    fun assertConnectPromptShown() =
        waitForText("Connect to import calories eaten and macronutrients (e.g. from Fitatu).")

    fun clickConnect() = clickText("Connect")

    fun assertConnectCardHidden() = waitForTextToDisappear("Health Connect")

    fun assertImportedDataShown() = waitForText("Data read from Health Connect")

    fun assertNoCalorieData() = waitForText("Health Connect has no calorie data for the last 30 days.")

    fun assertDay(date: LocalDate, expected: String) =
        waitFor(hasTestTag(ProgressTags.healthDay(date)) and hasText(expected))

    fun assertDaySources(date: LocalDate, expected: String) =
        waitFor(hasTestTag(ProgressTags.healthDaySources(date)) and hasText(expected))
}
