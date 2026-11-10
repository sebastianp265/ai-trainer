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
class TrainingFlowE2ETest : AbstractE2ETest() {

    private companion object {
        const val BENCH = "BARBELL_BENCH_PRESS"
    }

    private val bottomBar = BottomBarScreen(compose)
    private val training = TrainingScreen(compose)
    private val plans = PlanScreen(compose)
    private val picker = ExercisePickerScreen(compose)
    private val workout = LogWorkoutScreen(compose)

    @BeforeTest
    fun start() = launchApp()

    @Test
    fun `first launch offers an empty workout and shows no history`() {
        training.assertFirstLaunch()
    }

    @Test
    fun `the first plan is the next one and the sheet offers every plan and an empty workout`() {
        writePlan("Push A", "Bench Press", "Barbell Bench Press")
        writePlan("Legs B", "Squat", "Back Squat")
        bottomBar.open("Training")

        training.assertNextPlan("Push A")
        training.openOtherWorkouts()

        training.assertSheetOffers("Push A", "Legs B")
    }

    @Test
    fun `an empty workout can be started from the sheet`() {
        writePlan("Push A", "Bench Press", "Barbell Bench Press")
        bottomBar.open("Training")

        training.openOtherWorkouts()
        training.startEmptyFromSheet()

        workout.assertExerciseShown("Add exercise")
    }

    @Test
    fun `a workout started from a plan can be changed and then finishes the plan`() {
        writePlan("Push A", "Bench Press", "Barbell Bench Press", extraSets = 2, weight = "80")
        bottomBar.open("Training")

        training.startNextPlan()
        workout.assertTitle("Push A")
        workout.assertSets(BENCH, "80" to "10", "80" to "10", "80" to "10")
        workout.deleteSet(BENCH, setIndex = 2)
        workout.typeWeight(BENCH, setIndex = 0, kg = "85")
        workout.assertSets(BENCH, "85" to "10", "80" to "10")
        workout.finish()

        training.assertNoNextPlan()
        training.assertRecentWorkout("Push A")
        bottomBar.open("Plan")
        plans.assertNoPlansYet()
        bottomBar.open("Training")
        workout.startWorkout()
        workout.openExercisePicker()
        picker.search("Bench Press")
        picker.pick("Barbell Bench Press")
        workout.assertLastTime(BENCH, "Last time: 10×85 kg · 10×80 kg")
    }

    @Test
    fun `discarding a workout from a plan keeps the plan`() {
        writePlan("Push A", "Bench Press", "Barbell Bench Press")
        bottomBar.open("Training")
        training.startNextPlan()
        workout.assertTitle("Push A")

        workout.askToDiscard()
        workout.confirmDiscard()

        training.assertNextPlan("Push A")
    }

    private fun writePlan(name: String, search: String, exercise: String, extraSets: Int = 0, weight: String? = null) {
        bottomBar.open("Plan")
        plans.startWritingOwnPlan()
        plans.typeName(name)
        plans.openExercisePicker()
        picker.search(search)
        picker.pick(exercise)
        weight?.let { plans.typeWeight(setIndex = 0, kg = it) }
        repeat(extraSets) { plans.addSet() }
        plans.save()
        plans.assertDetailShown(name)
        plans.back()
    }
}
