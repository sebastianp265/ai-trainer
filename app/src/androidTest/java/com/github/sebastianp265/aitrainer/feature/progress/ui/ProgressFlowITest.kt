package com.github.sebastianp265.aitrainer.feature.progress.ui

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.feature.health.data.HealthConnectDayDto
import com.github.sebastianp265.aitrainer.testing.AbstractITest
import com.github.sebastianp265.aitrainer.testing.FakeHealthDataSource
import com.github.sebastianp265.aitrainer.testing.waitForText
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.time.Clock

@RunWith(AndroidJUnit4::class)
class ProgressFlowITest : AbstractITest() {

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())

    @Test
    fun connectingHealthConnectClosesThePermissionScreenAndHidesTheCard() {
        val permissionScreen = HealthConnectPermissionScreen()
        permissionScreen.revokeAllPermissions()
        launchApp()
        compose.openProgress()
        compose.waitForText("Connect to import calories eaten and macronutrients (e.g. from Fitatu).")

        compose.onNodeWithText("Connect").performClick()
        permissionScreen.allowAll()

        permissionScreen.assertClosedAndAppInFront()
        compose.waitForTextToDisappear("Health Connect")
        compose.waitForText("Data read from Health Connect")
    }

    @Test
    fun daysReadFromHealthConnectAreListed() {
        launchApp(
            healthDataSource = FakeHealthDataSource(
                listOf(
                    HealthConnectDayDto(daysAgo(1), 2000, 140.0, 230.0, 70.0, listOf("com.fitatu.tracker")),
                    HealthConnectDayDto(daysAgo(3), 1800, null, null, null, emptyList()),
                ),
            ),
        )

        compose.openProgress()

        compose.assertHealthDay(daysAgo(1), "Eaten 2,000 kcal · Protein 140 g · Carbs 230 g · Fat 70 g")
        compose.assertHealthDay(daysAgo(3), "Eaten 1,800 kcal · Protein – · Carbs – · Fat –")
        compose.assertHealthDaySources(daysAgo(1), "From: com.fitatu.tracker")
        compose.assertHealthDaySources(daysAgo(3), "From: –")
    }

    @Test
    fun dataRemovedFromHealthConnectDisappearsAfterTheAppIsReopened() {
        val healthConnect = FakeHealthDataSource(listOf(HealthConnectDayDto(daysAgo(1), 2000, null, null, null, listOf("com.fitatu.tracker"))))
        launchApp(healthDataSource = healthConnect)
        compose.openProgress()
        compose.assertHealthDay(daysAgo(1), "Eaten 2,000 kcal · Protein – · Carbs – · Fat –")

        healthConnect.days = emptyList()
        restartApp()
        compose.openProgress()

        compose.waitForText("Health Connect has no calorie data for the last 30 days.")
    }

    private fun daysAgo(days: Int): LocalDate = today.minus(DatePeriod(days = days))
}
