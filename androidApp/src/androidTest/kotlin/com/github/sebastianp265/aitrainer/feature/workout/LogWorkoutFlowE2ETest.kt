package com.github.sebastianp265.aitrainer.feature.workout

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.ExercisePickerScreen
import com.github.sebastianp265.aitrainer.shared.e2e.LogWorkoutScreen
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class LogWorkoutFlowE2ETest : AbstractE2ETest() {

    private val workout = LogWorkoutScreen(compose)
    private val picker = ExercisePickerScreen(compose)

    @BeforeTest
    fun start() = launchApp()

    @Test
    fun `logged workout is saved to history`() {
        logFinishedBenchWorkout()

        workout.assertWorkoutSaved()
        workout.assertHistoryEntry("1 exercise · 2 sets")
    }

    @Test
    fun `next workout starts from last workout numbers`() {
        logFinishedBenchWorkout()

        workout.startWorkout()
        addBenchPress()
        workout.assertSets(BARBELL_BENCH_PRESS, "7.5" to "9")

        workout.clickSameAsLastTime()
        workout.assertSets(BARBELL_BENCH_PRESS, "7.5" to "9", "7.5" to "9", "10" to "9")

        workout.discard()
        workout.assertHistoryEntry("1 exercise · 2 sets")
    }

    private fun logFinishedBenchWorkout() {
        workout.startWorkout()
        addBenchPress()
        workout.assertSets(BARBELL_BENCH_PRESS, "0" to "10")

        workout.increaseWeight(BARBELL_BENCH_PRESS, taps = 3)
        workout.decreaseReps(BARBELL_BENCH_PRESS)
        workout.assertSets(BARBELL_BENCH_PRESS, "7.5" to "9")

        workout.addSet()
        workout.assertSets(BARBELL_BENCH_PRESS, "7.5" to "9", "7.5" to "9")
        workout.increaseWeight(BARBELL_BENCH_PRESS, setIndex = 1)
        workout.assertSets(BARBELL_BENCH_PRESS, "7.5" to "9", "10" to "9")

        workout.finish()
    }

    private fun addBenchPress() {
        workout.openExercisePicker()
        picker.search("Bench Press")
        picker.pick("Barbell Bench Press")
    }

    private companion object {
        const val BARBELL_BENCH_PRESS = "BARBELL_BENCH_PRESS"
    }
}
