package com.github.sebastianp265.aitrainer.feature.plan

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BottomBarScreen
import com.github.sebastianp265.aitrainer.shared.e2e.ExercisePickerScreen
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class PlanFlowE2ETest : AbstractE2ETest() {

    private val bottomBar = BottomBarScreen(compose)
    private val plans = PlanScreen(compose)
    private val picker = ExercisePickerScreen(compose)

    @BeforeTest
    fun start() {
        launchApp()
        bottomBar.open("Plan")
    }

    @Test
    fun `without plans the Plan tab invites to write one`() {
        plans.assertNoPlansYet()
    }

    @Test
    fun `a written plan is saved and shown in detail and in the list`() {
        writePushPlan()

        plans.assertDetailShown("Push A", "Barbell Bench Press", "1 × 10 · 80 kg")
        plans.back()
        plans.assertListedUnder("No date", "Push A")
    }

    @Test
    fun `saving needs a name and an exercise`() {
        plans.startWritingOwnPlan()
        plans.assertCannotSave()

        plans.typeName("Push A")
        plans.assertCannotSave()

        plans.openExercisePicker()
        picker.search("Bench Press")
        picker.pick("Barbell Bench Press")
        plans.save()
        plans.assertDetailShown("Push A")
    }

    @Test
    fun `an edited plan shows its new numbers`() {
        writePushPlan()

        plans.edit()
        plans.typeWeight(setIndex = 0, kg = "85")
        plans.save()

        plans.assertDetailShown("Push A", "1 × 10 · 85 kg")
    }

    @Test
    fun `a deleted plan disappears after confirmation`() {
        writePushPlan()

        plans.delete()
        plans.confirmDelete()

        plans.assertNoPlansYet()
    }

    private fun writePushPlan() {
        plans.startWritingOwnPlan()
        plans.typeName("Push A")
        plans.openExercisePicker()
        picker.search("Bench Press")
        picker.pick("Barbell Bench Press")
        plans.typeWeight(setIndex = 0, kg = "80")
        plans.save()
    }
}
