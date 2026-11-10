package com.github.sebastianp265.aitrainer.feature.workout

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BACK_SQUAT
import com.github.sebastianp265.aitrainer.shared.e2e.BENCH_PRESS
import com.github.sebastianp265.aitrainer.shared.e2e.BottomBarScreen
import com.github.sebastianp265.aitrainer.shared.e2e.LogWorkoutScreen
import com.github.sebastianp265.aitrainer.shared.e2e.PlanScreen
import com.github.sebastianp265.aitrainer.shared.e2e.sets
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class TrainingFlowE2ETest : AbstractE2ETest() {

    private val bottomBar = BottomBarScreen(compose)
    private val training = TrainingScreen(compose)
    private val plans = PlanScreen(compose)
    private val workout = LogWorkoutScreen(compose)

    @Test
    fun `first launch offers an empty workout and shows no history`() {
        launchApp()

        training.assertFirstLaunch()
    }

    @Test
    fun `the first plan is the next one and the sheet offers every plan and an empty workout`() {
        given.plan("Push A", sets(BENCH_PRESS))
        given.plan("Legs B", sets(BACK_SQUAT))
        launchApp()

        training.assertNextPlan("Push A")
        training.openOtherWorkouts()

        training.assertSheetOffers("Push A", "Legs B")
    }

    @Test
    fun `an empty workout can be started from the sheet`() {
        given.plan("Push A", sets(BENCH_PRESS))
        launchApp()

        training.openOtherWorkouts()
        training.startEmptyFromSheet()

        workout.assertExerciseShown("Add exercise")
    }

    @Test
    fun `a workout started from a plan can be changed and then finishes the plan`() {
        given.plan("Push A", sets(BENCH_PRESS, count = 3, kg = 80.0))
        launchApp()

        training.startNextPlan()
        workout.assertTitle("Push A")
        workout.assertSets(BENCH_PRESS, "80" to "10", "80" to "10", "80" to "10")
        workout.deleteSet(BENCH_PRESS, setIndex = 2)
        workout.typeWeight(BENCH_PRESS, setIndex = 0, kg = "85")
        workout.assertSets(BENCH_PRESS, "85" to "10", "80" to "10")
        workout.finish()

        training.assertNoNextPlan()
        training.assertRecentWorkout("Push A")
        bottomBar.open("Plan")
        plans.assertNoPlansYet()
    }
}
