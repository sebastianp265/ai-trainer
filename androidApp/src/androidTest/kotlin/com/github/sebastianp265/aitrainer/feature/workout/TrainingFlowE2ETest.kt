package com.github.sebastianp265.aitrainer.feature.workout

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BACK_SQUAT
import com.github.sebastianp265.aitrainer.shared.e2e.BENCH_PRESS
import com.github.sebastianp265.aitrainer.shared.e2e.LogWorkoutScreen
import com.github.sebastianp265.aitrainer.shared.e2e.PlanScreen
import com.github.sebastianp265.aitrainer.shared.e2e.sets
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class TrainingFlowE2ETest : AbstractE2ETest() {

    private val training = TrainingScreen(compose)
    private val plans = PlanScreen(compose)
    private val workout = LogWorkoutScreen(compose)

    @Test
    fun `first launch offers an empty workout and shows no history`() {
        launchApp()

        training.assertFirstLaunch()
    }

    @Test
    fun `the next plan is offered on top and the other plans below it`() {
        given.plan("Push A", sets(BENCH_PRESS))
        given.plan("Legs B", sets(BACK_SQUAT))
        launchApp()

        training.assertPlansOffered("Push A", "Legs B")
    }

    @Test
    fun `tapping a plan does nothing and its edit button opens the editor`() {
        given.plan("Push A", sets(BENCH_PRESS))
        given.plan("Legs B", sets(BACK_SQUAT))
        launchApp()

        training.assertPlanRowDoesNotOpenOnTap("Legs B")
        training.editPlan("Legs B")

        plans.assertEditorShown()
    }

    @Test
    fun `a workout without a plan starts empty`() {
        given.plan("Push A", sets(BENCH_PRESS))
        launchApp()

        training.startEmpty()

        workout.assertExerciseShown("Add exercise")
    }

    @Test
    fun `a workout started from a plan can be changed and then finishes the plan`() {
        given.plan("Push A", sets(BENCH_PRESS, count = 3, kg = 80.0))
        launchApp()

        training.startPlan("Push A")
        workout.assertTitle("Push A")
        workout.assertSets(BENCH_PRESS, "80" to "10", "80" to "10", "80" to "10")
        workout.deleteSet(BENCH_PRESS, setIndex = 2)
        workout.typeWeight(BENCH_PRESS, setIndex = 0, kg = "85")
        workout.assertSets(BENCH_PRESS, "85" to "10", "80" to "10")
        workout.finish()

        training.assertRecentWorkout("Push A")
        plans.assertNoPlansYet()
    }

    @Test
    fun `going back from a running workout keeps it running on top of Training`() {
        given.runningWorkout(sets(BENCH_PRESS, kg = 80.0))
        launchApp()

        workout.goBack()
        training.resumeWorkout()

        workout.assertSets(BENCH_PRESS, "80" to "10")
    }
}
