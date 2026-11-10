package com.github.sebastianp265.aitrainer.feature.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.integration.health.NutritionTotals
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.FakeHealthService
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.time.Clock

@RunWith(AndroidJUnit4::class)
class ProgressFlowE2ETest : AbstractE2ETest() {

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
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
    fun `days synced from Health Connect are listed`() {
        val healthConnect = FakeHealthService()
        healthConnect.nutrition = listOf(
            nutrition(daysAgo(1), kcal = 2000, protein = 140.0, carbs = 230.0, fat = 70.0),
            nutrition(daysAgo(3), kcal = 1800),
        )
        launchApp(healthDataSource = healthConnect)

        progress.open()

        progress.assertDay(daysAgo(1), "Eaten 2,000 kcal · Protein 140 g · Carbs 230 g · Fat 70 g")
        progress.assertDay(daysAgo(3), "Eaten 1,800 kcal · Protein – · Carbs – · Fat –")
    }

    @Test
    fun `data removed from Health Connect disappears after the app is reopened`() {
        val healthConnect = FakeHealthService()
        healthConnect.nutrition = listOf(nutrition(daysAgo(1), kcal = 2000))
        launchApp(healthDataSource = healthConnect)
        progress.open()
        progress.assertDay(daysAgo(1), "Eaten 2,000 kcal · Protein – · Carbs – · Fat –")

        healthConnect.nutrition = emptyList()
        restartApp()
        progress.open()

        progress.assertDayGone(daysAgo(1))
    }

    private fun nutrition(date: LocalDate, kcal: Int, protein: Double? = null, carbs: Double? = null, fat: Double? = null) =
        NutritionTotals(date, kcal, protein, carbs, null, fat, null, null, null)

    private fun daysAgo(days: Int): LocalDate = today.minus(DatePeriod(days = days))
}
