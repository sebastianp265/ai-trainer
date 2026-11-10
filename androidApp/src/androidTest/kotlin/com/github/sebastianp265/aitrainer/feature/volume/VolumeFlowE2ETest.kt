package com.github.sebastianp265.aitrainer.feature.volume

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.feature.progress.ProgressScreen
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BottomBarScreen
import com.github.sebastianp265.aitrainer.shared.e2e.ExercisePickerScreen
import com.github.sebastianp265.aitrainer.shared.e2e.LogWorkoutScreen
import com.github.sebastianp265.aitrainer.shared.e2e.PlanScreen
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class VolumeFlowE2ETest : AbstractE2ETest() {

    private val bottomBar = BottomBarScreen(compose)
    private val progress = ProgressScreen(compose)
    private val targets = VolumeTargetsScreen(compose)
    private val workout = LogWorkoutScreen(compose)
    private val picker = ExercisePickerScreen(compose)
    private val plans = PlanScreen(compose)

    @BeforeTest
    fun start() = launchApp()

    @Test
    fun `progress shows sets per muscle group with the default target`() {
        logBenchWorkout()

        progress.open()

        progress.assertVolumeCardShown()
        progress.assertVolumeTarget("target 10–20")
    }

    @Test
    fun `a target typed by hand shows in progress and defaults can be restored`() {
        logBenchWorkout()
        progress.open()
        progress.openTargets()
        targets.assertShown()

        targets.setRange("CHEST", from = "6", to = "9")
        targets.assertSource("You ·")
        targets.back()
        progress.assertVolumeTarget("target 6–9")

        progress.openTargets()
        targets.restoreDefaults()
        targets.back()
        progress.assertVolumeTarget("target 10–20")
    }

    @Test
    fun `switching targets off leaves only the numbers`() {
        logBenchWorkout()
        progress.open()
        progress.openTargets()

        targets.switchShowTargets()
        targets.back()

        progress.assertVolumeCardShown()
        progress.assertNoVolumeTargets()
    }

    @Test
    fun `the plan tab shows this week and the sets in this weeks plans`() {
        bottomBar.open("Plan")
        plans.startWritingOwnPlan()
        plans.typeName("Push A")
        plans.openExercisePicker()
        picker.search("Bench Press")
        picker.pick("Barbell Bench Press")
        plans.save()
        plans.assertDetailShown("Push A")
        plans.back()

        plans.assertWeekShown()
    }

    private fun logBenchWorkout() {
        workout.startWorkout()
        workout.openExercisePicker()
        picker.search("Bench Press")
        picker.pick("Barbell Bench Press")
        workout.finish()
    }
}
