package com.github.sebastianp265.aitrainer.feature.coach.impl

import com.github.sebastianp265.aitrainer.integration.coach.ClaudeCacheDuration
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeEffort
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeThinking
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeTuning
import com.github.sebastianp265.aitrainer.integration.coach.CoachRun
import kotlin.time.Duration.Companion.seconds
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
        assertEquals(18, request.tools.size)
    }

    @Test
    fun `a message about a plan is shown with its topic and tells Claude which plan it is about`() = runBlocking {
        f.claude.answer("Looks good")
        val topic = com.github.sebastianp265.aitrainer.integration.coach.CoachTopic(com.github.sebastianp265.aitrainer.integration.coach.TopicKind.PLAN, "12", "Push A")

        f.coach.send("Is this too much?", topic)

        assertEquals(topic, f.coach.observeMessages().first().first().topic)
        val sent = f.claude.requests.single().messages.single().content.single() as ClaudeContent.Text
        assertEquals("[The user asks about this plan id 12: \"Push A\"]\nIs this too much?", sent.text)
    }

    @Test
    fun `a deleted message is gone from the chat and from what Claude reads next`() = runBlocking {
        f.claude.answer("One")
        f.claude.answer("Two")
        f.coach.send("forget this")
        val forgotten = f.coach.observeMessages().first().first()

        f.coach.deleteMessage(forgotten.id)
        f.coach.send("hello")

        assertEquals(listOf("One", "hello", "Two"), f.coach.observeMessages().first().map { it.text })
        val sent = f.claude.requests.last().messages.flatMap { it.content }.filterIsInstance<ClaudeContent.Text>().joinToString("\n") { it.text }
        assertTrue("forget this" !in sent)
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
    fun `the chosen tuning is sent with every request`() = runBlocking {
        val tuning = ClaudeTuning(effort = ClaudeEffort.LOW, thinking = ClaudeThinking.ADAPTIVE, maxTokens = 4_000, cacheDuration = ClaudeCacheDuration.FIVE_MINUTES)
        f.settings.setClaudeTuning(tuning)
        f.claude.answer("Sure.")

        f.coach.send("a")

        assertEquals(tuning, f.claude.requests.single().tuning)
    }

    @Test
    fun `the last run adds up its rounds, tokens and time`() = runBlocking {
        f.claude.whileAnswering = { f.now += 2.seconds }
        f.claude.callTool("get_plans", "{}")
        f.claude.answer("No plans yet.")

        f.coach.send("a")

        assertEquals(
            CoachRun(rounds = 2, inputTokens = 200, cacheReadTokens = 0, cacheWriteTokens = 0, outputTokens = 40, duration = 4.seconds, stopReason = "end_turn"),
            f.coach.observeLastRun().first(),
        )
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
    fun `undoing an accepted new plan deletes it`() = runBlocking {
        val id = assertNotNull(f.toolbox.run("propose_plan", benchPlan).proposalId)
        f.coach.accept(id)
        assertTrue(f.coach.observeProposal(id).first()!!.isUndoable)

        assertTrue(f.coach.undo(id))

        assertEquals(emptyList(), f.plans.observeUpcoming().first())
        val proposal = f.coach.observeProposal(id).first()!!
        assertEquals(ProposalState.UNDONE, proposal.state)
        assertTrue(!proposal.isUndoable)
    }

    @Test
    fun `undoing an accepted plan change brings back the plan as it was`() = runBlocking {
        val planId = f.plans.createPlan(com.github.sebastianp265.aitrainer.integration.plan.PlanDraft("Push A", null, listOf(com.github.sebastianp265.aitrainer.integration.plan.PlannedSet("bench", 8, 80.0, null, false)), coachNote = "Old note"), PlanAuthor.COACH)
        val change = """{"planId":$planId,"name":"Push B","summary":"Heavier","sets":[{"exerciseKey":"squat","reps":5,"weightKg":100}]}"""
        val id = assertNotNull(f.toolbox.run("propose_plan_change", change).proposalId)
        f.coach.accept(id)

        f.coach.undo(id)

        val plan = f.plans.observePlan(planId).first()!!
        assertEquals("Push A", plan.name)
        assertEquals(listOf(com.github.sebastianp265.aitrainer.integration.plan.PlannedSet("bench", 8, 80.0, null, false)), plan.sets)
        assertEquals("Old note", plan.coachNote)
    }

    @Test
    fun `undoing an accepted target change brings back the previous target`() = runBlocking {
        val id = assertNotNull(f.toolbox.run("propose_target_change", """{"groupKey":"LEGS","minSets":8,"maxSets":12,"reason":"recovery","summary":"Legs"}""").proposalId)
        f.coach.accept(id)

        f.coach.undo(id)

        val legs = f.volume.observeTargets().first().single { it.groupKey == "LEGS" }
        assertEquals(10 to 20, legs.minSets to legs.maxSets)
        assertEquals(com.github.sebastianp265.aitrainer.integration.volume.TargetSource.DEFAULT, legs.source)
    }

    @Test
    fun `a plan used for a workout or a target changed by hand can no longer be undone`() = runBlocking {
        val planProposal = assertNotNull(f.toolbox.run("propose_plan", benchPlan).proposalId)
        val targetProposal = assertNotNull(f.toolbox.run("propose_target_change", """{"groupKey":"LEGS","minSets":8,"maxSets":12,"reason":"r","summary":"Legs"}""").proposalId)
        f.coach.accept(planProposal)
        f.coach.accept(targetProposal)

        f.workouts.startSession(f.plans.observeUpcoming().first().single().id)
        f.volume.setTarget("LEGS", 9, 14)

        assertTrue(!f.coach.observeProposal(planProposal).first()!!.isUndoable)
        assertTrue(!f.coach.undo(planProposal))
        assertTrue(!f.coach.observeProposal(targetProposal).first()!!.isUndoable)
        assertTrue(!f.coach.undo(targetProposal))
    }

    @Test
    fun `a change of the running workout replaces the sets of its exercises and can be undone until the workout ends`() = runBlocking {
        f.workouts.startSession()
        val session = f.workouts.observeActiveSession().first()!!.id
        f.workouts.addExercise(session, "bench")
        f.workouts.addExercise(session, "squat")
        assertTrue("Bench Press [bench]: 10×0 kg" in f.toolbox.run("get_workout", "{}").text)
        val change = """{"summary":"Lighter bench","sets":[{"exerciseKey":"bench","reps":8,"weightKg":60},{"exerciseKey":"bench","reps":8,"weightKg":60}]}"""
        val id = assertNotNull(f.toolbox.run("propose_workout_change", change).proposalId)

        f.coach.accept(id)

        val sets = f.workouts.observeSets(session).first().sortedBy { it.positionInSession }
        assertEquals(listOf("bench" to 60.0, "bench" to 60.0, "squat" to 0.0), sets.map { it.exerciseKey to it.weightKg })
        f.coach.undo(id)
        assertEquals(listOf("bench" to 0.0, "squat" to 0.0), f.workouts.observeSets(session).first().sortedBy { it.positionInSession }.map { it.exerciseKey to it.weightKg })

        val second = assertNotNull(f.toolbox.run("propose_workout_change", """{"summary":"No squats","removeExerciseKeys":["squat"]}""").proposalId)
        f.coach.accept(second)
        assertEquals(listOf("bench"), f.workouts.observeSets(session).first().map { it.exerciseKey })
        f.workouts.finishSession(session)
        assertTrue(!f.coach.observeProposal(second).first()!!.isUndoable)
    }

    @Test
    fun `a finished workout can be corrected by the coach`() = runBlocking {
        f.finishBenchWorkout(weight = 800.0)
        val sessionId = f.workouts.observeRecentSessions().first().single().id
        assertTrue("sessionId=$sessionId" in f.toolbox.run("get_workouts", "{}").text)
        val fix = """{"sessionId":$sessionId,"summary":"800 kg was a typo","sets":[{"exerciseKey":"bench","reps":10,"weightKg":80}]}"""

        f.coach.accept(assertNotNull(f.toolbox.run("propose_workout_change", fix).proposalId))

        assertEquals(80.0, f.workouts.observeSets(sessionId).first().single().weightKg)
    }

    @Test
    fun `a new exercise proposed by the coach exists once accepted, and a known name is refused`() = runBlocking {
        val refused = f.toolbox.run("propose_new_exercise", """{"name":"bench press","primaryGroupKey":"CHEST","summary":"x"}""")
        assertTrue(refused.isError)
        val id = assertNotNull(f.toolbox.run("propose_new_exercise", """{"name":"Landmine Press","primaryGroupKey":"SHOULDERS","secondaryGroupKeys":["CHEST"],"summary":"For your shoulder"}""").proposalId)

        f.coach.accept(id)

        val created = f.exercises.exercises().single { it.namesByLanguage["en"] == "Landmine Press" }
        assertEquals(listOf("SHOULDERS"), created.primaryMuscles.map { it.key })
        assertTrue(!f.coach.observeProposal(id).first()!!.isUndoable)
    }

    @Test
    fun `an accepted nutrition target is told to the coach and can be undone while it is unchanged`() = runBlocking {
        val id = assertNotNull(f.toolbox.run("propose_nutrition_target", """{"kcal":2400,"proteinG":160,"reason":"lean bulk","summary":"2400 kcal, 160 g protein"}""").proposalId)

        f.coach.accept(id)

        val target = assertNotNull(f.nutrition.observeTarget().first())
        assertEquals(2400 to 160, target.kcal to target.proteinG)
        f.claude.answer("ok")
        f.coach.send("Hi")
        assertTrue("kcal: 2400, protein: 160 g" in f.claude.requests.single().system)
        assertTrue(f.coach.undo(id))
        assertEquals(null, f.nutrition.observeTarget().first())
    }

    @Test
    fun `a plan or workout changed by hand after accepting can no longer be undone`() = runBlocking {
        val planProposal = assertNotNull(f.toolbox.run("propose_plan", benchPlan).proposalId)
        f.coach.accept(planProposal)
        val plan = f.plans.observeUpcoming().first().single()
        f.plans.updatePlan(plan.id, com.github.sebastianp265.aitrainer.integration.plan.PlanDraft(plan.name, plan.date, plan.sets.map { it.copy(reps = 5) }))
        assertTrue(!f.coach.observeProposal(planProposal).first()!!.isUndoable)
        assertTrue(!f.coach.undo(planProposal))
        assertEquals(5, f.plans.observePlan(plan.id).first()!!.sets.single().reps)

        f.workouts.startSession()
        val session = f.workouts.observeActiveSession().first()!!.id
        f.workouts.addExercise(session, "bench")
        val change = assertNotNull(f.toolbox.run("propose_workout_change", """{"summary":"x","sets":[{"exerciseKey":"bench","reps":8,"weightKg":60}]}""").proposalId)
        f.coach.accept(change)
        f.workouts.changeSet(f.workouts.observeSets(session).first().single().id, com.github.sebastianp265.aitrainer.integration.workout.SetChange.WeightTo(65.0))
        assertTrue(!f.coach.observeProposal(change).first()!!.isUndoable)
        assertTrue(!f.coach.undo(change))
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
    fun `an RPE off the scale is refused and nothing is saved`() = runBlocking {
        val offScale = listOf(5.5, 7.25, 10.5).map { rpe ->
            f.toolbox.run("propose_plan", """{"name":"X","summary":"x","sets":[{"exerciseKey":"bench","reps":8,"rpe":$rpe}]}""")
        }

        assertTrue(offScale.all { it.isError && "RPE" in it.text })
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
