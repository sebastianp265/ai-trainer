package com.github.sebastianp265.aitrainer.feature.exercise

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BENCH_PRESS
import com.github.sebastianp265.aitrainer.shared.e2e.ExercisePickerScreen
import com.github.sebastianp265.aitrainer.shared.e2e.LogWorkoutScreen
import com.github.sebastianp265.aitrainer.shared.e2e.sets
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class ExercisePickerFlowE2ETest : AbstractE2ETest() {

    private val workout = LogWorkoutScreen(compose)
    private val picker = ExercisePickerScreen(compose)

    @Test
    fun `muscle group chip shows only exercises of that group`() {
        openPicker()

        picker.filterByGroup("Legs")

        picker.assertShown("Back Squat")
        picker.assertNotShown("Barbell Bench Press")
    }

    @Test
    fun `a new exercise is created and added to the workout`() {
        openPicker()

        picker.createExercise("Landmine press", primaryGroup = "Shoulders", "Chest")

        workout.assertExerciseShown("Landmine press")
    }

    @Test
    fun `exercises from finished workouts are listed as recently used`() {
        given.finishedWorkout(sets(BENCH_PRESS))

        openPicker()

        picker.assertRecentlyUsedShown("Barbell Bench Press")
    }

    private fun openPicker() {
        launchApp()
        workout.startWorkout()
        workout.openExercisePicker()
    }
}
