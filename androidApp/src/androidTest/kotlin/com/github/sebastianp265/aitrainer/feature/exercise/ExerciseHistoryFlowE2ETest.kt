package com.github.sebastianp265.aitrainer.feature.exercise

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.feature.progress.ProgressScreen
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.ExercisePickerScreen
import com.github.sebastianp265.aitrainer.shared.e2e.LogWorkoutScreen
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class ExerciseHistoryFlowE2ETest : AbstractE2ETest() {

    private companion object {
        const val BARBELL_BENCH_PRESS = "BARBELL_BENCH_PRESS"
    }

    private val workout = LogWorkoutScreen(compose)
    private val picker = ExercisePickerScreen(compose)
    private val progress = ProgressScreen(compose)
    private val history = ExerciseHistoryScreen(compose)

    @BeforeTest
    fun start() {
        launchApp()
        logFinishedBenchWorkout()
    }

    @Test
    fun `last time label of a running workout opens the exercise history`() {
        workout.startWorkout()
        addBenchPress()

        workout.openLastTimeHistory(BARBELL_BENCH_PRESS)

        history.assertShownFor("Barbell Bench Press", "1 workout")
        history.assertSessionShown("8×80 kg RPE 8")
        history.assertStat("best set")
    }

    @Test
    fun `a strength row in Progress opens the exercise history`() {
        progress.open()

        progress.openStrengthExercise("Barbell Bench Press")

        history.assertShownFor("Barbell Bench Press", "1 workout")
    }

    private fun logFinishedBenchWorkout() {
        workout.startWorkout()
        addBenchPress()
        workout.typeWeight(BARBELL_BENCH_PRESS, setIndex = 0, kg = "80")
        workout.decreaseReps(BARBELL_BENCH_PRESS)
        workout.decreaseReps(BARBELL_BENCH_PRESS)
        workout.chooseRpe(BARBELL_BENCH_PRESS, setIndex = 0, rpe = "8")
        workout.finish()
    }

    private fun addBenchPress() {
        workout.openExercisePicker()
        picker.search("Bench Press")
        picker.pick("Barbell Bench Press")
    }
}
