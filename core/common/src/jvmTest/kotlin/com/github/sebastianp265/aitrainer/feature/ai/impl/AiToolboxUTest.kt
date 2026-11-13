package com.github.sebastianp265.aitrainer.feature.ai.impl

import com.github.sebastianp265.aitrainer.integration.coach.ProposalDraft
import com.github.sebastianp265.aitrainer.integration.coach.ProposedPlan
import com.github.sebastianp265.aitrainer.integration.exercise.NewExercise
import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import com.github.sebastianp265.aitrainer.integration.plan.PlanDraft
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AiToolboxUTest {

    private val f = AiFixture()

    @AfterTest
    fun close() = f.database.close()

    private suspend fun run(name: String, inputJson: String) = f.toolbox.run(name, inputJson, f.desk)

    @Test
    fun `read tools answer from the database`() = runBlocking {
        f.finishBenchWorkout(82.5)
        f.plans.createPlan(PlanDraft("Legs", listOf(PlannedSet("squat", 5, 100.0, null, false))), PlanAuthor.USER)

        val history = run("get_exercise_history", """{"exerciseKey":"bench"}""")
        val found = run("find_exercises", """{"query":"squat"}""")
        val upcoming = run("get_plans", "{}")
        val weekly = run("get_weekly_sets", """{"weeks":2}""")
        val health = run("get_health_days", """{"from":"2026-11-01","to":"2026-11-10"}""")

        assertTrue("82.5 kg" in history.text && history.checked == "exercise:bench")
        assertTrue("squat: Back Squat" in found.text)
        assertTrue("\"Legs\"" in upcoming.text && "[squat]" in upcoming.text)
        assertTrue("CHEST 1" in weekly.text && "ARMS 0.5" in weekly.text)
        assertTrue("No Health Connect data" in health.text)
    }

    @Test
    fun `the running workout and a finished one can be read`() = runBlocking {
        f.workouts.startSession()
        val session = f.workouts.observeActiveSession().first()!!.id
        f.workouts.addExercise(session, "bench")
        assertTrue("Bench Press [bench]: 10×0 kg" in run("get_workout", "{}").text)
        f.workouts.finishSession(session)

        assertTrue("sessionId=$session" in run("get_workouts", "{}").text)
    }

    @Test
    fun `a proposed plan reaches the desk as a new plan`() = runBlocking {
        val result = run("propose_plan", """{"name":"Push A","summary":"Heavier bench","sets":[{"exerciseKey":"bench","reps":8,"weightKg":82.5,"rpe":8.0}]}""")

        assertFalse(result.isError)
        assertEquals(
            ProposalDraft.Plan(ProposedPlan(null, "Push A", listOf(PlannedSet("bench", 8, 82.5, 8.0, false))), "Heavier bench"),
            f.desk.drafts.single(),
        )
    }

    @Test
    fun `a plan change needs the id of the plan`() = runBlocking {
        val result = run("propose_plan_change", """{"name":"Push A","summary":"x","sets":[{"exerciseKey":"bench","reps":8}]}""")

        assertTrue(result.isError)
        assertEquals(emptyList(), f.desk.drafts)
    }

    @Test
    fun `a plan change, several plans, a target, a workout change, an exercise and a nutrition target reach the desk`() = runBlocking {
        run("propose_plan_change", """{"planId":7,"name":"Push A","summary":"a","sets":[{"exerciseKey":"bench","reps":8,"weightKg":85}]}""")
        run("propose_plans", """{"summary":"Week","plans":[{"name":"A","sets":[{"exerciseKey":"bench","reps":8}]},{"planId":3,"name":"B","sets":[{"exerciseKey":"squat","reps":5}]}]}""")
        run("propose_target_change", """{"groupKey":"LEGS","minSets":8,"maxSets":12,"reason":"recovery","summary":"Legs"}""")
        run("propose_workout_change", """{"sessionId":4,"summary":"typo","sets":[{"exerciseKey":"bench","reps":10,"weightKg":80,"warmup":true}],"removeExerciseKeys":["squat"]}""")
        run("propose_new_exercise", """{"name":"Landmine Press","primaryGroupKey":"SHOULDERS","secondaryGroupKeys":["CHEST"],"summary":"For your shoulder"}""")
        run("propose_nutrition_target", """{"kcal":2400,"proteinG":160,"reason":"lean bulk","summary":"2400 kcal"}""")

        assertEquals(
            listOf(
                ProposalDraft.Plan(ProposedPlan(7, "Push A", listOf(PlannedSet("bench", 8, 85.0, null, false))), "a"),
                ProposalDraft.Plans(
                    listOf(
                        ProposedPlan(null, "A", listOf(PlannedSet("bench", 8, 0.0, null, false))),
                        ProposedPlan(3, "B", listOf(PlannedSet("squat", 5, 0.0, null, false))),
                    ),
                    "Week",
                ),
                ProposalDraft.Target("LEGS", 8, 12, "recovery", "Legs"),
                ProposalDraft.WorkoutChange(4, listOf(PlannedSet("bench", 10, 80.0, null, true)), listOf("squat"), "typo"),
                ProposalDraft.Exercise(NewExercise("Landmine Press", "SHOULDERS", listOf("CHEST")), "For your shoulder"),
                ProposalDraft.Nutrition(2400, 160, "lean bulk", "2400 kcal"),
            ),
            f.desk.drafts,
        )
    }

    @Test
    fun `a proposal the desk rejects is an error with the reason`() = runBlocking {
        f.desk.rejection = "unknown exerciseKey nope"

        val result = run("propose_plan", """{"name":"X","summary":"x","sets":[{"exerciseKey":"nope","reps":8}]}""")

        assertTrue(result.isError && "unknown exerciseKey nope" in result.text)
    }

    @Test
    fun `input that is not valid json for the tool is an error`() = runBlocking {
        val result = run("propose_plan", """{"summary":"x"}""")

        assertTrue(result.isError && "Invalid input" in result.text)
        assertTrue(run("no_such_tool", "{}").isError)
    }

    @Test
    fun `notes and profile are kept in the coach memory`() = runBlocking {
        run("add_note", """{"text":"Prefers mornings"}""")
        val id = f.memory.observeNotes().first().single().id
        run("update_note", """{"id":$id,"text":"Prefers early mornings"}""")
        run("update_profile", """{"field":"equipment","value":"full gym"}""")

        assertEquals("Prefers early mornings", f.memory.observeNotes().first().single().text)
        assertEquals(mapOf("equipment" to "full gym"), f.memory.observeProfile().first())

        run("delete_note", """{"id":$id}""")
        assertEquals(emptyList(), f.memory.observeNotes().first())
        assertTrue(run("delete_note", """{"id":$id}""").isError)
        assertTrue(run("update_profile", """{"field":"mood","value":"x"}""").isError)
    }
}
