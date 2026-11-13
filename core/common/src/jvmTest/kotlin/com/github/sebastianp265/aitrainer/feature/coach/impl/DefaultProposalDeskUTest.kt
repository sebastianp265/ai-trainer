package com.github.sebastianp265.aitrainer.feature.coach.impl

import com.github.sebastianp265.aitrainer.integration.coach.ProposalDraft
import com.github.sebastianp265.aitrainer.integration.coach.ProposalOutcome
import com.github.sebastianp265.aitrainer.integration.coach.ProposedPlan
import com.github.sebastianp265.aitrainer.integration.exercise.NewExercise
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class DefaultProposalDeskUTest {

    private val f = CoachFixture()

    @AfterTest
    fun close() = f.database.close()

    private fun planOf(vararg sets: PlannedSet) = ProposalDraft.Plan(ProposedPlan(null, "X", sets.toList()), "x")

    private suspend fun assertRejectedAndNothingSaved(draft: ProposalDraft, reasonPart: String) {
        val outcome = assertIs<ProposalOutcome.Rejected>(f.desk.propose(draft))

        assertTrue(reasonPart in outcome.reason, outcome.reason)
        assertEquals(emptyList(), f.coach.observePendingProposals().first())
    }

    @Test
    fun `an unknown exercise is refused and nothing is saved`() = runBlocking {
        assertRejectedAndNothingSaved(planOf(PlannedSet("nope", 8, 0.0, null, false)), "unknown exerciseKey nope")
    }

    @Test
    fun `an RPE off the scale is refused and nothing is saved`() = runBlocking {
        listOf(5.5, 7.25, 10.5).forEach { rpe ->
            assertRejectedAndNothingSaved(planOf(benchSet(rpe = rpe)), "RPE")
        }
    }

    @Test
    fun `a plan needs a name, sets and positive reps`() = runBlocking {
        assertRejectedAndNothingSaved(ProposalDraft.Plan(ProposedPlan(null, " ", listOf(benchSet())), "x"), "name")
        assertRejectedAndNothingSaved(ProposalDraft.Plan(ProposedPlan(null, "X", emptyList()), "x"), "no sets")
        assertRejectedAndNothingSaved(planOf(benchSet(reps = 0)), "reps")
    }

    @Test
    fun `a target needs a known muscle group and a sane range`() = runBlocking {
        assertRejectedAndNothingSaved(ProposalDraft.Target("NECK", 8, 12, "r", "x"), "unknown muscle group")
        assertRejectedAndNothingSaved(ProposalDraft.Target("LEGS", 12, 8, "r", "x"), "minSets")
    }

    @Test
    fun `a new exercise with a known name is refused`() = runBlocking {
        assertRejectedAndNothingSaved(ProposalDraft.Exercise(NewExercise("bench press", "CHEST", emptyList()), "x"), "bench")
        assertRejectedAndNothingSaved(ProposalDraft.Exercise(NewExercise("Landmine Press", "WINGS", emptyList()), "x"), "unknown muscle group")
    }

    @Test
    fun `a nutrition target needs a value inside the allowed range`() = runBlocking {
        assertRejectedAndNothingSaved(ProposalDraft.Nutrition(null, null, "r", "x"), "kcal, proteinG or both")
        assertRejectedAndNothingSaved(ProposalDraft.Nutrition(500, null, "r", "x"), "kcal")
        assertRejectedAndNothingSaved(ProposalDraft.Nutrition(null, 20, "r", "x"), "proteinG")
    }

    @Test
    fun `a workout change needs a workout and something to change`() = runBlocking {
        assertRejectedAndNothingSaved(ProposalDraft.WorkoutChange(null, listOf(benchSet()), emptyList(), "x"), "no running workout")

        f.workouts.startSession()
        assertRejectedAndNothingSaved(ProposalDraft.WorkoutChange(null, emptyList(), emptyList(), "x"), "nothing to change")
    }
}
