package com.github.sebastianp265.aitrainer.ui.progress

import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.domain.model.HealthDayTotals
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup
import com.github.sebastianp265.aitrainer.testing.AppUnderTest
import com.github.sebastianp265.aitrainer.testing.FakeHealthDataSource
import com.github.sebastianp265.aitrainer.testing.addExercise
import com.github.sebastianp265.aitrainer.testing.assertSetsInCard
import com.github.sebastianp265.aitrainer.testing.openTab
import com.github.sebastianp265.aitrainer.testing.scrollTo
import com.github.sebastianp265.aitrainer.testing.startWorkout
import com.github.sebastianp265.aitrainer.testing.waitFor
import com.github.sebastianp265.aitrainer.testing.waitForText
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Clock

@RunWith(AndroidJUnit4::class)
class ProgressFlowITest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val app = AppUnderTest()

    @AfterTest
    fun close() = app.close()

    @Test
    fun calorieBalanceSummarisesTheLastSevenCompleteDaysFromHealthConnect() {
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        fun daysAgo(days: Int, eaten: Int, burned: Int) =
            HealthDayTotals(today.minus(DatePeriod(days = days)), eaten, null, null, null, burned)
        app.startInEnglishWithFreshData(
            healthDataSource = FakeHealthDataSource(
                listOf(
                    daysAgo(0, eaten = 1000, burned = 500),
                    daysAgo(1, eaten = 2000, burned = 2500),
                    daysAgo(2, eaten = 2200, burned = 2600),
                    daysAgo(3, eaten = 2400, burned = 2700),
                    daysAgo(10, eaten = 5000, burned = 1000),
                ),
            ),
        )

        openProgress()
        compose.waitForText("Connected. Food and burned calories sync daily.")

        assertValue(ProgressTags.AVERAGE_INTAKE, "2,200 kcal")
        assertValue(ProgressTags.AVERAGE_BURN, "2,600 kcal")
        assertValue(ProgressTags.AVERAGE_BALANCE, "-400 kcal")
    }

    @Test
    fun dataRemovedFromHealthConnectDisappearsAfterSyncNow() {
        val yesterday = Clock.System.todayIn(TimeZone.currentSystemDefault()).minus(DatePeriod(days = 1))
        val healthConnect = FakeHealthDataSource(listOf(HealthDayTotals(yesterday, 2000, null, null, null, 2500)))
        app.startInEnglishWithFreshData(healthDataSource = healthConnect)
        openProgress()
        assertValue(ProgressTags.AVERAGE_INTAKE, "2,000 kcal")
        assertValue(ProgressTags.AVERAGE_BURN, "2,500 kcal")

        healthConnect.days = emptyList()
        compose.scrollTo(hasText("Sync now")).performClick()

        assertValue(ProgressTags.AVERAGE_INTAKE, "Not enough data")
        assertValue(ProgressTags.AVERAGE_BURN, "Not enough data")
        assertValue(ProgressTags.AVERAGE_BALANCE, "Not enough data")
    }

    @Test
    fun manualWeightEntriesShowLatestAndSevenDayAverage() {
        app.startInEnglishWithFreshData()
        openProgress()

        addWeight("82.4")
        addWeight("82.0")

        assertValue(ProgressTags.LATEST_WEIGHT, "82.0 kg")
        assertValue(ProgressTags.WEIGHT_AVERAGE, "82.2 kg")
        assertValue(ProgressTags.WEIGHT_TREND, "Not enough data")
    }

    @Test
    fun finishedWorkoutCountsSetsPerMuscleGroup() {
        app.startInEnglishWithFreshData()
        compose.startWorkout()
        compose.addExercise(searchQuery = "Bench Press", exerciseToPick = "Barbell Bench Press")
        compose.assertSetsInCard("Barbell Bench Press", "0" to "10")
        compose.onNodeWithText("+ Set").performClick()
        compose.assertSetsInCard("Barbell Bench Press", "0" to "10", "0" to "10")
        compose.onNodeWithText("Finish").performClick()
        compose.waitForText("Workout saved")

        openProgress()

        assertValue(ProgressTags.muscleSets(MuscleGroup.CHEST), "2")
        assertValue(ProgressTags.muscleSets(MuscleGroup.TRICEPS), "1")
        assertValue(ProgressTags.muscleSets(MuscleGroup.FRONT_DELTS), "1")
    }

    private fun openProgress() {
        compose.waitForText("Progress")
        compose.openTab("Progress")
        compose.waitForText("Body weight")
    }

    private fun addWeight(kg: String) {
        compose.onNodeWithText("Add weight").performClick()
        compose.onNodeWithTag(ProgressTags.WEIGHT_INPUT).performTextInput(kg)
        compose.onNode(hasText("Save") and hasAnyAncestor(isDialog())).performClick()
    }

    private fun assertValue(tag: String, expected: String) {
        compose.scrollTo(hasTestTag(tag))
        compose.waitFor(hasTestTag(tag) and hasText(expected))
    }
}
