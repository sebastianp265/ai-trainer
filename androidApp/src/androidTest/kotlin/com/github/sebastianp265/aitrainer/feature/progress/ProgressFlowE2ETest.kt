package com.github.sebastianp265.aitrainer.feature.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.integration.health.DailyAmount
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType
import com.github.sebastianp265.aitrainer.integration.health.NutritionTotals
import com.github.sebastianp265.aitrainer.integration.health.TimedValue
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.FakeHealthService
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

@RunWith(AndroidJUnit4::class)
class ProgressFlowE2ETest : AbstractE2ETest() {

    private val zone = TimeZone.currentSystemDefault()
    private val today = Clock.System.todayIn(zone)
    private val progress = ProgressScreen(compose)
    private val permissions = HealthConnectPermissionScreen()

    @Test
    fun `connecting Health Connect closes the permission screen and hides the card`() {
        permissions.revokeAllPermissions()
        launchApp()
        progress.open()
        progress.assertConnectPromptShown()

        progress.clickConnect()
        permissions.allowAll()

        permissions.assertClosedAndAppInFront()
        progress.assertConnectCardHidden()
    }

    @Test
    fun `cards are shown only for the data types that were allowed`() {
        val healthConnect = FakeHealthService(granted = setOf(HealthDataType.WEIGHT, HealthDataType.STEPS))
        healthConnect.weights = listOf(
            TimedValue(daysAgo(3).atStartOfDayIn(zone) + 8.hours, 83.0),
            TimedValue(daysAgo(1).atStartOfDayIn(zone) + 8.hours, 82.0),
        )
        healthConnect.steps = listOf(DailyAmount(daysAgo(1), 9000))
        healthConnect.nutrition = listOf(nutrition(daysAgo(1), kcal = 2000))
        launchApp(healthDataSource = healthConnect)

        progress.open()

        progress.assertCardShown("Weight · 7-day average")
        progress.assertCardShown("Steps")
        progress.assertCardHidden("Calorie balance · average per day")
        progress.assertCardHidden("Macros · average per day")
        progress.assertCardHidden("Sleep")
        progress.assertCardHidden("HRV")
    }

    @Test
    fun `days synced from Health Connect are listed`() {
        val healthConnect = FakeHealthService()
        healthConnect.nutrition = listOf(nutrition(daysAgo(1), kcal = 2000), nutrition(daysAgo(3), kcal = 1800))
        healthConnect.caloriesBurned = listOf(DailyAmount(daysAgo(1), 2500))
        launchApp(healthDataSource = healthConnect)

        progress.open()

        progress.assertDay(daysAgo(1), "eaten 2,000 kcal · burned 2,500 kcal")
        progress.assertDay(daysAgo(3), "eaten 1,800 kcal")
    }

    @Test
    fun `weight card opens the weight details`() {
        val healthConnect = FakeHealthService(granted = setOf(HealthDataType.WEIGHT))
        healthConnect.weights = listOf(
            TimedValue(daysAgo(10).atStartOfDayIn(zone) + 8.hours, 84.0),
            TimedValue(daysAgo(1).atStartOfDayIn(zone) + 8.hours, 82.0),
        )
        launchApp(healthDataSource = healthConnect)
        progress.open()

        progress.openWeightDetails()

        progress.assertWeightDetailsShown()
    }

    @Test
    fun `a day opens its details`() {
        val healthConnect = FakeHealthService()
        healthConnect.nutrition = listOf(nutrition(daysAgo(1), kcal = 2000))
        healthConnect.caloriesBurned = listOf(DailyAmount(daysAgo(1), 2500))
        launchApp(healthDataSource = healthConnect)
        progress.open()

        progress.openDay(daysAgo(1))

        progress.assertDayDetailsShown("Eaten", "Burned", "Balance")
    }

    @Test
    fun `data removed from Health Connect disappears after the app is reopened`() {
        val healthConnect = FakeHealthService()
        healthConnect.nutrition = listOf(nutrition(daysAgo(1), kcal = 2000))
        launchApp(healthDataSource = healthConnect)
        progress.open()
        progress.assertDay(daysAgo(1), "eaten 2,000 kcal")

        healthConnect.nutrition = emptyList()
        restartApp()
        progress.open()

        progress.assertDayGone(daysAgo(1))
    }

    private fun nutrition(date: LocalDate, kcal: Int) = NutritionTotals(date, kcal, null, null, null, null, null, null, null)

    private fun daysAgo(days: Int): LocalDate = today.minus(DatePeriod(days = days))
}
