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

        workout.assertHistoryEntry("1 exercise · 2 sets")
    }

    @Test
    fun `next workout starts from last workout numbers and shows what was done last time`() {
        logFinishedBenchWorkout()

        workout.startWorkout()
        addBenchPress()
        workout.assertSets(BARBELL_BENCH_PRESS, "7.5" to "9")
        workout.assertLastTime(BARBELL_BENCH_PRESS, "Last time: 9×7.5 kg RPE 8 · 9×10 kg")

        workout.clickSameAsLastTime()
        workout.assertSets(BARBELL_BENCH_PRESS, "7.5" to "9", "7.5" to "9", "10" to "9")
    }

    @Test
    fun `warm-up sets are not part of the last time label`() {
        workout.startWorkout()
        addBenchPress()
        workout.toggleWarmup(BARBELL_BENCH_PRESS, setIndex = 0)
        workout.addSet()
        workout.typeWeight(BARBELL_BENCH_PRESS, setIndex = 1, kg = "80")
        workout.finish()

        workout.startWorkout()
        addBenchPress()

        workout.assertLastTime(BARBELL_BENCH_PRESS, "Last time: 10×80 kg")
    }

    @Test
    fun `discarding a workout asks first and keeps the sets when cancelled`() {
        workout.startWorkout()
        addBenchPress()

        workout.askToDiscard()
        workout.keepWorkout()
        workout.assertSets(BARBELL_BENCH_PRESS, "–" to "10")

        workout.askToDiscard()
        workout.confirmDiscard()
        workout.assertNoWorkoutsYet()
    }

    private fun logFinishedBenchWorkout() {
        workout.startWorkout()
        addBenchPress()
        workout.assertSets(BARBELL_BENCH_PRESS, "–" to "10")
        workout.assertNoLastTime(BARBELL_BENCH_PRESS)

        workout.typeWeight(BARBELL_BENCH_PRESS, setIndex = 0, kg = "7.5")
        workout.decreaseReps(BARBELL_BENCH_PRESS)
        workout.chooseRpe(BARBELL_BENCH_PRESS, setIndex = 0, rpe = "8")
        workout.assertRpe(BARBELL_BENCH_PRESS, setIndex = 0, expected = "8")
        workout.assertSets(BARBELL_BENCH_PRESS, "7.5" to "9")

        workout.addSet()
        workout.typeWeight(BARBELL_BENCH_PRESS, setIndex = 1, kg = "10")
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
