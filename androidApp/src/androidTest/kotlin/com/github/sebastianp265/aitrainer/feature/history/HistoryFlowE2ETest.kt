package com.github.sebastianp265.aitrainer.feature.history

import com.github.sebastianp265.aitrainer.shared.e2e.sets
import com.github.sebastianp265.aitrainer.shared.e2e.BENCH_PRESS
import com.github.sebastianp265.aitrainer.shared.e2e.WorkoutReportScreen
import com.github.sebastianp265.aitrainer.shared.e2e.HistoryScreen
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class HistoryFlowE2ETest : AbstractE2ETest() {

    private val history = HistoryScreen(compose)
    private val report = WorkoutReportScreen(compose)

    @BeforeTest
    fun start() {
        given.finishedWorkout(sets(BENCH_PRESS, kg = 80.0))
        launchApp()
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
}
