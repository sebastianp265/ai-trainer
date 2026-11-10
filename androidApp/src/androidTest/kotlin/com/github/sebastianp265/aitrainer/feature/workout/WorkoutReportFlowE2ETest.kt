package com.github.sebastianp265.aitrainer.feature.workout

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BottomBarScreen
import com.github.sebastianp265.aitrainer.shared.e2e.ExercisePickerScreen
import com.github.sebastianp265.aitrainer.shared.e2e.LogWorkoutScreen
import com.github.sebastianp265.aitrainer.shared.e2e.PlanScreen
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class WorkoutReportFlowE2ETest : AbstractE2ETest() {

    private companion object {
        const val BENCH = "BARBELL_BENCH_PRESS"
    }

    private val bottomBar = BottomBarScreen(compose)
    private val training = TrainingScreen(compose)
    private val plans = PlanScreen(compose)
    private val picker = ExercisePickerScreen(compose)
    private val workout = LogWorkoutScreen(compose)
    private val report = WorkoutReportScreen(compose)

    @BeforeTest
    fun start() = launchApp()

    @Test
    fun `a finished workout without a plan shows no plan comparison and no records`() {
        logBench(weight = "80")

        workout.finishAndSeeSummary()

        report.assertSummaryShown()
        report.assertNoPlanComparison()
        report.assertNoRecords()
        report.done()
        training.assertRecentWorkout("Workout")
    }

    @Test
    fun `a heavier second workout shows a record`() {
        logBench(weight = "80")
        workout.finish()

        logBench(weight = "85")
        workout.finishAndSeeSummary()

        report.assertSummaryShown()
        report.assertRecordShown("Barbell Bench Press")
    }

    @Test
    fun `a workout from a plan is compared with it`() {
        writePlanWithThreeSets()
        bottomBar.open("Training")
        training.startNextPlan()
        workout.deleteSet(BENCH, setIndex = 2)

        workout.finishAndSeeSummary()

        report.assertSummaryShown()
        report.assertPlanComparison("-1 set, -10 reps")
    }

    @Test
    fun `a finished workout opens from the list and can be edited and deleted`() {
        logBench(weight = "80")
        workout.finish()

        training.openRecentWorkout("Workout")
        report.assertDetailSets(1)
        report.edit()
        workout.typeWeight(BENCH, setIndex = 0, kg = "82.5")
        workout.finishAndSeeSummary()
        report.assertSummaryShown()
        report.showAllSets()
        report.assertDetailSets(1)
        report.delete()
        report.confirmDelete()

        training.assertFirstLaunch()
    }

    private fun logBench(weight: String) {
        workout.startWorkout()
        workout.openExercisePicker()
        picker.search("Bench Press")
        picker.pick("Barbell Bench Press")
        workout.typeWeight(BENCH, setIndex = 0, kg = weight)
    }

    private fun writePlanWithThreeSets() {
        bottomBar.open("Plan")
        plans.startWritingOwnPlan()
        plans.typeName("Push A")
        plans.openExercisePicker()
        picker.search("Bench Press")
        picker.pick("Barbell Bench Press")
        plans.typeWeight(setIndex = 0, kg = "80")
        plans.addSet()
        plans.addSet()
        plans.save()
        plans.assertDetailShown("Push A")
        plans.back()
    }
}
