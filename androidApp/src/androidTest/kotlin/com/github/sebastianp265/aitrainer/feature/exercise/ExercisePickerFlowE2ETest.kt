package com.github.sebastianp265.aitrainer.feature.exercise

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.ExercisePickerScreen
import com.github.sebastianp265.aitrainer.shared.e2e.LogWorkoutScreen
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class ExercisePickerFlowE2ETest : AbstractE2ETest() {

    private val workout = LogWorkoutScreen(compose)
    private val picker = ExercisePickerScreen(compose)

    @BeforeTest
    fun start() {
        launchApp()
        workout.startWorkout()
        workout.openExercisePicker()
    }

    @Test
    fun `muscle group chip shows only exercises of that group`() {
        picker.filterByGroup("Legs")

        picker.assertShown("Back Squat")
        picker.assertNotShown("Barbell Bench Press")
    }

    @Test
    fun `a new exercise is created and added to the workout`() {
        picker.createExercise("Landmine press", primaryGroup = "Shoulders", "Chest")

        workout.assertExerciseShown("Landmine press")
    }

    @Test
    fun `exercises from finished workouts are listed as recently used`() {
        picker.pick("Barbell Bench Press")
        workout.finish()
        workout.startWorkout()
        workout.openExercisePicker()

        picker.assertRecentlyUsedShown("Barbell Bench Press")
    }
}
