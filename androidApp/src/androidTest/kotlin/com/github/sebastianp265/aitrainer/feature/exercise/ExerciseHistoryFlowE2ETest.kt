package com.github.sebastianp265.aitrainer.feature.exercise

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.feature.progress.ProgressScreen
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BENCH_PRESS
import com.github.sebastianp265.aitrainer.shared.e2e.LogWorkoutScreen
import com.github.sebastianp265.aitrainer.shared.e2e.sets
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class ExerciseHistoryFlowE2ETest : AbstractE2ETest() {

    private val workout = LogWorkoutScreen(compose)
    private val progress = ProgressScreen(compose)
    private val history = ExerciseHistoryScreen(compose)

    @BeforeTest
    fun start() {
        given.finishedWorkout(sets(BENCH_PRESS, reps = 8, kg = 80.0, rpe = 8.0))
    }

    @Test
    fun `last time label of a running workout opens the exercise history`() {
        given.runningWorkout(sets(BENCH_PRESS))
        launchApp()

        workout.openLastTimeHistory(BENCH_PRESS)

        history.assertShownFor("Barbell Bench Press", "1 workout")
        history.assertSessionShown("8×80 kg RPE 8")
        history.assertStat("best set")
    }

    @Test
    fun `a strength row in Progress opens the exercise history`() {
        launchApp()
        progress.open()

        progress.openStrengthExercise("Barbell Bench Press")

        history.assertShownFor("Barbell Bench Press", "1 workout")
    }
}
