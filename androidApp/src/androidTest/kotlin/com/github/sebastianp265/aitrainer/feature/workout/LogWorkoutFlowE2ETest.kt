package com.github.sebastianp265.aitrainer.feature.workout

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BENCH_PRESS
import com.github.sebastianp265.aitrainer.shared.e2e.ExercisePickerScreen
import com.github.sebastianp265.aitrainer.shared.e2e.LogWorkoutScreen
import com.github.sebastianp265.aitrainer.shared.e2e.sets
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class LogWorkoutFlowE2ETest : AbstractE2ETest() {

    private val workout = LogWorkoutScreen(compose)
    private val picker = ExercisePickerScreen(compose)
    private val training = TrainingScreen(compose)

    @Test
    fun `logged workout is saved to history`() {
        launchApp()

        workout.startWorkout()
        addBenchPress()
        workout.assertSets(BENCH_PRESS, "–" to "10")
        workout.assertNoLastTime(BENCH_PRESS)
        workout.typeWeight(BENCH_PRESS, setIndex = 0, kg = "7.5")
        workout.decreaseReps(BENCH_PRESS)
        workout.chooseRpe(BENCH_PRESS, setIndex = 0, rpe = "8")
        workout.assertRpe(BENCH_PRESS, setIndex = 0, expected = "8")
        workout.addSet()
        workout.typeWeight(BENCH_PRESS, setIndex = 1, kg = "10")
        workout.assertSets(BENCH_PRESS, "7.5" to "9", "10" to "9")
        workout.finish()

        workout.assertHistoryEntry("2 sets")
    }

    @Test
    fun `next workout starts from last workout numbers and shows what was done last time`() {
        given.finishedWorkout(sets(BENCH_PRESS, reps = 9, kg = 7.5, rpe = 8.0) + sets(BENCH_PRESS, reps = 9, kg = 10.0))
        launchApp()

        workout.startWorkout()
        addBenchPress()

        workout.assertSets(BENCH_PRESS, "7.5" to "9")
        workout.assertLastTime(BENCH_PRESS, "Last time: 9×7.5 kg RPE 8 · 9×10 kg")
    }

    @Test
    fun `warm-up sets are not part of the last time label`() {
        launchApp()
        workout.startWorkout()
        addBenchPress()
        workout.toggleWarmup(BENCH_PRESS, setIndex = 0)
        workout.addSet()
        workout.typeWeight(BENCH_PRESS, setIndex = 1, kg = "80")
        workout.finish()

        workout.startWorkout()
        addBenchPress()

        workout.assertLastTime(BENCH_PRESS, "Last time: 10×80 kg")
    }

    @Test
    fun `discarding a workout from a plan asks first and keeps the plan`() {
        given.plan("Push A", sets(BENCH_PRESS))
        launchApp()
        training.startPlan("Push A")

        workout.askToDiscard()
        workout.keepWorkout()
        workout.assertSets(BENCH_PRESS, "–" to "10")
        workout.askToDiscard()
        workout.confirmDiscard()

        training.assertNoRecentWorkouts()
        training.assertPlansOffered("Push A")
    }

    private fun addBenchPress() {
        workout.openExercisePicker()
        picker.search("Bench Press")
        picker.pick("Barbell Bench Press")
    }
}
