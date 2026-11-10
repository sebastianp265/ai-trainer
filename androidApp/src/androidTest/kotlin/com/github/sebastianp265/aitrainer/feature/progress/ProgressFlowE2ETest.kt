package com.github.sebastianp265.aitrainer.feature.progress

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
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
        progress.assertImportedDataShown()
    }

    @Test
    fun `days read from Health Connect are listed`() {
        launchApp(
            healthService = FakeHealthService(
                listOf(
                    HealthDay(daysAgo(1), 2000, 140.0, 230.0, 70.0, listOf("com.example.nutrition")),
                    HealthDay(daysAgo(3), 1800, null, null, null, emptyList()),
                ),
            ),
        )

        progress.open()

        progress.assertDay(daysAgo(1), "Eaten 2,000 kcal · Protein 140 g · Carbs 230 g · Fat 70 g")
        progress.assertDay(daysAgo(3), "Eaten 1,800 kcal · Protein – · Carbs – · Fat –")
        progress.assertDaySources(daysAgo(1), "From: com.example.nutrition")
        progress.assertDaySources(daysAgo(3), "From: –")
    }

    @Test
    fun `data removed from Health Connect disappears after the app is reopened`() {
        val healthConnect = FakeHealthService(listOf(HealthDay(daysAgo(1), 2000, null, null, null, listOf("com.example.nutrition"))))
        launchApp(healthService = healthConnect)
        progress.open()
        progress.assertDay(daysAgo(1), "Eaten 2,000 kcal · Protein – · Carbs – · Fat –")

        healthConnect.days = emptyList()
        restartApp()
        progress.open()

        progress.assertNoCalorieData()
    }

    private fun daysAgo(days: Int): LocalDate = today.minus(DatePeriod(days = days))
}
