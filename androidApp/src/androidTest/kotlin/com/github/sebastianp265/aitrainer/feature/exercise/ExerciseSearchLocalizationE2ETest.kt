package com.github.sebastianp265.aitrainer.feature.exercise

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.ExercisePickerScreen
import com.github.sebastianp265.aitrainer.shared.e2e.LogWorkoutScreen
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class ExerciseSearchLocalizationE2ETest : AbstractE2ETest() {

    override val appLanguage = "pl"

    private val workout = LogWorkoutScreen(compose, startWorkoutText = "Rozpocznij trening", addExerciseDescription = "Dodaj ćwiczenie")
    private val picker = ExercisePickerScreen(compose)

    @Test
    fun `Polish search shows Polish name matches first then English name matches`() {
        launchApp()
        workout.startWorkout()
        workout.openExercisePicker()

        picker.search("fly")

        picker.assertResultsInOrder("Rozpiętki na maszynie (butterfly)", "Odwrotne rozpiętki z hantlami", "Rozpiętki na wyciągu")
    }
}
