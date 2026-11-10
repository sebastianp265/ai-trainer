package com.github.sebastianp265.aitrainer.feature.coach.impl

import com.github.sebastianp265.aitrainer.integration.coach.ClaudeContent
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeFailureKind
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeRole
import com.github.sebastianp265.aitrainer.integration.coach.CoachActivity
import com.github.sebastianp265.aitrainer.integration.coach.CoachRole
import com.github.sebastianp265.aitrainer.integration.coach.ProposalState
import com.github.sebastianp265.aitrainer.integration.coach.ProposalType
import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DefaultCoachServiceUTest {

    private val f = CoachFixture()

    @AfterTest
    fun close() = f.database.close()

    private val benchPlan = """{"name":"Push A","date":"2026-11-12","summary":"Heavier bench","sets":[{"exerciseKey":"bench","reps":8,"weightKg":82.5,"rpe":8.0}]}"""

    @Test
    fun `the request carries profile, notes, targets, data summary and the conversation`() = runBlocking {
        f.coach.setProfileField("goal", "get stronger")
        f.database.coachDao().insertNote(com.github.sebastianp265.aitrainer.shared.coach.CoachNoteEntity(text = "Left shoulder hurts on overhead press", createdAt = f.now))
        f.volume.setTarget("ABS", 6, 10)
        f.finishBenchWorkout()
        f.claude.answer("Hello")

        f.coach.send("How am I doing?")

        val request = f.claude.requests.single()
        assertEquals("claude-sonnet-5-5", request.model)
        assertTrue("goal: get stronger" in request.system)
        assertTrue("Left shoulder hurts on overhead press" in request.system)
        assertTrue("ABS: 6-10 (user)" in request.system)
        assertTrue("CHEST: 10-20 (default)" in request.system)
        assertTrue("1 exercises, 1 working sets" in request.system)
        assertEquals(listOf(ClaudeRole.USER), request.messages.map { it.role })
        assertEquals(ClaudeContent.Text("How am I doing?"), request.messages.single().content.single())
        assertEquals(15, request.tools.size)
    }

    @Test
    fun `an answer is saved with what the tools read, and the tool result goes back to Claude`() = runBlocking {
        f.finishBenchWorkout()
        f.claude.callTool("get_workouts", "{}")
        f.claude.answer("You trained once.")

        f.coach.send("How was my week?")

        val messages = f.coach.observeMessages().first()
        assertEquals(listOf(CoachRole.USER, CoachRole.COACH), messages.map { it.role })
        assertEquals("You trained once.", messages.last().text)
        assertEquals(listOf("workouts"), messages.last().checked)
        val toolResult = f.claude.requests.last().messages.last().content.single() as ClaudeContent.ToolResult
        assertTrue("Bench Press" in toolResult.content)
        assertEquals(CoachActivity.Idle, f.coach.observeActivity().first())
    }

    @Test
    fun `a failure keeps the message, shows the error, and retry asks again`() = runBlocking {
        f.claude.fail(ClaudeFailureKind.NO_NETWORK)

        f.coach.send("Plan me a push day")

        assertEquals(1, f.coach.observeMessages().first().size)
        assertEquals(CoachActivity.Failed(ClaudeFailureKind.NO_NETWORK, "failure"), f.coach.observeActivity().first())
        f.claude.answer("Sure.")
        f.coach.retry()
        assertEquals(listOf(CoachRole.USER, CoachRole.COACH), f.coach.observeMessages().first().map { it.role })
        assertEquals(2, f.claude.requests.size)
    }

    @Test
    fun `token counts are added to this months usage`() = runBlocking {
        f.claude.answer("One")
        f.claude.answer("Two")

        f.coach.send("a")
        f.coach.send("b")

        val usage = f.coach.observeUsageThisMonth().first()
        assertEquals(2, usage.messages)
        assertEquals(200L, usage.inputTokens)
        assertEquals(40L, usage.outputTokens)
    }

    @Test
    fun `read tools answer from the database`() = runBlocking {
        f.finishBenchWorkout(82.5)
        f.plans.createPlan(com.github.sebastianp265.aitrainer.integration.plan.PlanDraft("Legs", null, listOf(com.github.sebastianp265.aitrainer.integration.plan.PlannedSet("squat", 5, 100.0, null, false))), PlanAuthor.USER)

        val history = f.toolbox.run("get_exercise_history", """{"exerciseKey":"bench"}""")
        val found = f.toolbox.run("find_exercises", """{"query":"squat"}""")
        val upcoming = f.toolbox.run("get_plans", "{}")
        val weekly = f.toolbox.run("get_weekly_sets", """{"weeks":2}""")
        val health = f.toolbox.run("get_health_days", """{"from":"2026-11-01","to":"2026-11-10"}""")

        assertTrue("82.5 kg" in history.text && history.checked == "exercise:bench")
        assertTrue("squat: Back Squat" in found.text)
        assertTrue("\"Legs\"" in upcoming.text && "[squat]" in upcoming.text)
        assertTrue("CHEST 1" in weekly.text && "ARMS 0.5" in weekly.text)
        assertTrue("No Health Connect data" in health.text)
    }

    @Test
    fun `a proposed plan waits until the user accepts, then it is a plan by the coach with the proposal as its note`() = runBlocking {
        val run = f.toolbox.run("propose_plan", benchPlan)
        val id = assertNotNull(run.proposalId)

        assertEquals(emptyList(), f.plans.observeUpcoming().first())
        assertEquals(ProposalState.PENDING, f.coach.observeProposal(id).first()!!.state)

        assertTrue(f.coach.accept(id))

        val plan = f.plans.observeUpcoming().first().single()
        assertEquals("Push A", plan.name)
        assertEquals(PlanAuthor.COACH, plan.author)
        assertEquals("Heavier bench", plan.coachNote)
        assertEquals(ProposalState.ACCEPTED, f.coach.observeProposal(id).first()!!.state)
        assertTrue(!f.coach.accept(id))
    }

    @Test
    fun `a newer proposal for the same plan supersedes the older one`() = runBlocking {
        val planId = f.plans.createPlan(com.github.sebastianp265.aitrainer.integration.plan.PlanDraft("Push A", null, listOf(com.github.sebastianp265.aitrainer.integration.plan.PlannedSet("bench", 8, 80.0, null, false))), PlanAuthor.USER)
        val change = """{"planId":$planId,"name":"Push A","summary":"x","sets":[{"exerciseKey":"bench","reps":8,"weightKg":%s}]}"""
        val first = assertNotNull(f.toolbox.run("propose_plan_change", change.format("82.5")).proposalId)
        val second = assertNotNull(f.toolbox.run("propose_plan_change", change.format("85")).proposalId)

        assertEquals(ProposalState.SUPERSEDED, f.coach.observeProposal(first).first()!!.state)
        assertEquals(listOf(second), f.coach.observePendingProposals().first().map { it.id })
        f.coach.accept(second)
        assertEquals(85.0, f.plans.observePlan(planId).first()!!.sets.single().weightKg)
    }

    @Test
    fun `accepting a target change moves the target and leaves plans untouched`() = runBlocking {
        f.plans.createPlan(com.github.sebastianp265.aitrainer.integration.plan.PlanDraft("Legs", null, listOf(com.github.sebastianp265.aitrainer.integration.plan.PlannedSet("squat", 5, 100.0, null, false))), PlanAuthor.USER)
        val id = assertNotNull(f.toolbox.run("propose_target_change", """{"groupKey":"LEGS","minSets":8,"maxSets":12,"reason":"recovery","summary":"Legs: 10-20 to 8-12"}""").proposalId)
        val proposal = f.coach.observeProposal(id).first()!!
        assertEquals(ProposalType.TARGET, proposal.type)
        val target = assertNotNull(proposal.target)
        assertEquals(10 to 20, target.previousMinSets to target.previousMaxSets)
        val plansBefore = f.plans.observeUpcoming().first()

        f.coach.accept(id)

        val legs = f.volume.observeTargets().first().single { it.groupKey == "LEGS" }
        assertEquals(8 to 12, legs.minSets to legs.maxSets)
        assertEquals("recovery", legs.reason)
        assertEquals(plansBefore, f.plans.observeUpcoming().first())
    }

    @Test
    fun `several plans in one proposal are created together`() = runBlocking {
        val plans = """{"summary":"Week","plans":[{"name":"A","sets":[{"exerciseKey":"bench","reps":8}]},{"name":"B","sets":[{"exerciseKey":"squat","reps":5}]}]}"""
        val id = assertNotNull(f.toolbox.run("propose_plans", plans).proposalId)

        f.coach.acceptAll(listOf(id))

        assertEquals(setOf("A", "B"), f.plans.observeUpcoming().first().map { it.name }.toSet())
    }

    @Test
    fun `an unknown exercise is refused and nothing is saved`() = runBlocking {
        val run = f.toolbox.run("propose_plan", """{"name":"X","summary":"x","sets":[{"exerciseKey":"nope","reps":8}]}""")

        assertTrue(run.isError && "find_exercises" in run.text)
        assertEquals(emptyList(), f.coach.observePendingProposals().first())
    }

    @Test
    fun `proposals created while answering are attached to the answer`() = runBlocking {
        f.claude.callTool("propose_plan", benchPlan)
        f.claude.answer("I wrote a plan.")

        f.coach.send("Plan please")

        val answer = f.coach.observeMessages().first().last()
        assertEquals("Heavier bench", answer.proposals.single().summary)
    }

    @Test
    fun `notes and profile can be changed by the coach and by the user`() = runBlocking {
        f.toolbox.run("add_note", """{"text":"Prefers mornings"}""")
        val id = f.coach.observeNotes().first().single().id
        f.toolbox.run("update_note", """{"id":$id,"text":"Prefers early mornings"}""")
        f.toolbox.run("update_profile", """{"field":"equipment","value":"full gym"}""")
        f.coach.setProfileField("time", "45 minutes")

        assertEquals("Prefers early mornings", f.coach.observeNotes().first().single().text)
        assertEquals(mapOf("equipment" to "full gym", "time" to "45 minutes"), f.coach.observeProfile().first())

        f.coach.deleteNote(id)
        assertEquals(emptyList(), f.coach.observeNotes().first())
        assertTrue(f.toolbox.run("update_profile", """{"field":"mood","value":"x"}""").isError)
    }

    private suspend fun finishBenchWorkoutLastWeek() {
        f.now = kotlin.time.Instant.parse("2026-11-05T10:00:00Z")
        f.finishBenchWorkout()
        f.now = kotlin.time.Instant.parse("2026-11-10T10:00:00Z")
    }

    @Test
    fun `last weeks review is posted once with its numbers and a proposal for the next week`() = runBlocking {
        finishBenchWorkoutLastWeek()
        f.claude.callTool("propose_plans", """{"summary":"Next week","plans":[{"name":"Push A","date":"2026-11-12","sets":[{"exerciseKey":"bench","reps":8,"weightKg":82.5}]},{"name":"Legs B","date":"2026-11-14","sets":[{"exerciseKey":"squat","reps":5,"weightKg":110}]}]}""")
        f.claude.answer("A solid week.")

        f.coach.reviewLastWeekIfDue()
        f.coach.reviewLastWeekIfDue()

        val message = f.coach.observeMessages().first().single()
        assertEquals("A solid week.", message.text)
        val review = assertNotNull(message.review)
        assertEquals(1, review.workouts)
        assertEquals(kotlinx.datetime.LocalDate(2026, 11, 2), review.weekStart)
        assertEquals(ProposalType.MULTI_PLAN, message.proposals.single().type)
        assertEquals(2, message.proposals.single().plans.size)
        assertTrue("WEEKLY REVIEW of 2026-11-02 to 2026-11-08" in (f.claude.requests.first().messages.last().content.single() as ClaudeContent.Text).text)
        assertEquals(2, f.claude.requests.size)
    }

    @Test
    fun `a week with nothing to review is skipped without asking Claude`() = runBlocking {
        f.coach.reviewLastWeekIfDue()

        assertEquals(emptyList(), f.coach.observeMessages().first())
        assertEquals(0, f.claude.requests.size)
    }

    @Test
    fun `a review that failed is tried again the next time`() = runBlocking {
        finishBenchWorkoutLastWeek()
        f.claude.fail(ClaudeFailureKind.NO_NETWORK)
        f.coach.reviewLastWeekIfDue()
        f.claude.answer("Good week.")

        f.coach.reviewLastWeekIfDue()

        assertEquals("Good week.", f.coach.observeMessages().first().single().text)
    }
}
