package com.github.sebastianp265.aitrainer.feature.workout

import com.github.sebastianp265.aitrainer.shared.e2e.WorkoutReportScreen
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BENCH_PRESS
import com.github.sebastianp265.aitrainer.shared.e2e.LogWorkoutScreen
import com.github.sebastianp265.aitrainer.shared.e2e.sets
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class WorkoutReportFlowE2ETest : AbstractE2ETest() {

    private val training = TrainingScreen(compose)
    private val workout = LogWorkoutScreen(compose)
    private val report = WorkoutReportScreen(compose)

    @Test
    fun `a finished workout without a plan shows no plan comparison and no records`() {
        given.runningWorkout(sets(BENCH_PRESS, kg = 80.0))
        launchApp()
        workout.assertSets(BENCH_PRESS, "80" to "10")

        workout.finishAndSeeSummary()

        report.assertSummaryShown()
        report.assertNoPlanComparison()
        report.assertNoRecords()
        report.done()
        training.assertRecentWorkout("Workout")
    }

    @Test
    fun `a heavier second workout shows a record`() {
        given.finishedWorkout(sets(BENCH_PRESS, kg = 80.0))
        given.runningWorkout(sets(BENCH_PRESS, kg = 85.0))
        launchApp()
        workout.assertSets(BENCH_PRESS, "85" to "10")

        workout.finishAndSeeSummary()

        report.assertRecordShown("Barbell Bench Press")
    }

    @Test
    fun `a workout from a plan is compared with it`() {
        val planId = given.plan("Push A", sets(BENCH_PRESS, count = 3, kg = 80.0))
        given.runningWorkoutFromPlan(planId, removedSetIndex = 2)
        launchApp()
        workout.assertSets(BENCH_PRESS, "80" to "10", "80" to "10")

        workout.finishAndSeeSummary()

        report.assertPlanComparison("-1 set, -10 reps")
    }

    @Test
    fun `a finished workout opens from the list and saved edits replace its sets`() {
        given.finishedWorkout(sets(BENCH_PRESS, kg = 80.0))
        launchApp()

        training.openRecentWorkout("Workout")
        report.assertDetailSets(1)
        report.edit()
        workout.typeWeight(BENCH_PRESS, setIndex = 0, kg = "82.5")
        workout.saveEdit()

        report.assertDetailSets(1)
        report.assertDetailSetShown("82.5 kg")
    }

    @Test
    fun `cancelled edits leave the workout as it was`() {
        given.finishedWorkout(sets(BENCH_PRESS, kg = 80.0))
        launchApp()
        training.openRecentWorkout("Workout")
        report.edit()
        workout.typeWeight(BENCH_PRESS, setIndex = 0, kg = "82.5")

        workout.cancelEdit()

        report.assertDetailSetShown("80 kg")
    }
}
