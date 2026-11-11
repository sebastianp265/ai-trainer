package com.github.sebastianp265.aitrainer.feature.history

import com.github.sebastianp265.aitrainer.shared.e2e.sets
import com.github.sebastianp265.aitrainer.shared.e2e.BENCH_PRESS
import com.github.sebastianp265.aitrainer.shared.e2e.WorkoutReportScreen
import com.github.sebastianp265.aitrainer.shared.e2e.HistoryScreen
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
    private val report = WorkoutReportScreen(compose)

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
        given.finishedWorkout(sets(BENCH_PRESS, kg = 80.0))
        launchApp(healthDataSource = healthConnect)
    }

    @Test
    fun `the gym filter hides workouts from other apps and the other apps filter shows them`() {
        history.open()
        history.assertWorkoutListed("Run")

        history.filterGym()
        history.assertWorkoutNotListed("Run")
        history.filterOtherApps()
        history.assertWorkoutListed("Run")
    }

    @Test
    fun `a gym workout deleted from the history returns to the history without it`() {
        history.open()
        history.openWorkout("Workout")

        report.delete()
        report.confirmDelete()

        history.assertShown()
        history.assertWorkoutGone("Workout")
    }

    @Test
    fun `a workout from another app opens read only`() {
        history.open()

        history.openWorkout("Run")

        history.assertReadOnlyDetails("42 min", "6.8 km", "480 kcal")
    }
}
