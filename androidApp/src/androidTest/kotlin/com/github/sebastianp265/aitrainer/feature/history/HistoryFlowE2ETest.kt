package com.github.sebastianp265.aitrainer.feature.history

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.integration.health.ExternalWorkoutReading
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.FakeHealthService
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours

@RunWith(AndroidJUnit4::class)
class HistoryFlowE2ETest : AbstractE2ETest() {

    private val zone = TimeZone.currentSystemDefault()
    private val today = Clock.System.todayIn(zone)
    private val history = HistoryScreen(compose)

    @BeforeTest
    fun start() {
        val healthConnect = FakeHealthService()
        healthConnect.workouts = listOf(
            ExternalWorkoutReading(
                type = "running",
                startedAt = today.minus(DatePeriod(days = 2)).atStartOfDayIn(zone) + 7.hours,
                durationMinutes = 42,
                distanceM = 6800.0,
                kcal = 480,
                averageHeartRate = null,
                writtenByThisApp = false,
            ),
        )
        launchApp(healthDataSource = healthConnect)
    }

    @Test
    fun `workouts from other apps are listed with the other apps filter`() {
        history.open()

        history.filterOtherApps()

        history.assertWorkoutListed("Run")
    }

    @Test
    fun `gym filter hides workouts from other apps`() {
        history.open()
        history.assertWorkoutListed("Run")

        history.filterGym()

        history.assertWorkoutNotListed("Run")
    }

    @Test
    fun `a workout from another app opens read only`() {
        history.open()

        history.openWorkout("Run")

        history.assertReadOnlyDetails("42 min", "6.8 km", "480 kcal")
    }
}
