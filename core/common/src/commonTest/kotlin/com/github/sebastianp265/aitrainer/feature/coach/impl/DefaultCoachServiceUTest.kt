package com.github.sebastianp265.aitrainer.feature.coach.impl

import com.github.sebastianp265.aitrainer.integration.coach.CoachActivity
import com.github.sebastianp265.aitrainer.integration.coach.CoachFailure
import com.github.sebastianp265.aitrainer.integration.coach.CoachReadiness
import com.github.sebastianp265.aitrainer.integration.coach.CoachRole
import com.github.sebastianp265.aitrainer.integration.coach.CoachTopic
import com.github.sebastianp265.aitrainer.integration.coach.ProposalDraft
import com.github.sebastianp265.aitrainer.integration.coach.ProposalState
import com.github.sebastianp265.aitrainer.integration.coach.ProposalType
import com.github.sebastianp265.aitrainer.integration.coach.ProposedPlan
import com.github.sebastianp265.aitrainer.integration.coach.TopicKind
import com.github.sebastianp265.aitrainer.integration.exercise.NewExercise
import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import com.github.sebastianp265.aitrainer.integration.plan.PlanDraft
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.integration.volume.TargetSource
import com.github.sebastianp265.aitrainer.integration.workout.SetChange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class DefaultCoachServiceUTest {

    private val f = CoachFixture()

    @AfterTest
    fun close() = f.database.close()

    private val pushA = ProposalDraft.Plan(ProposedPlan(null, "Push A", listOf(benchSet(82.5, rpe = 8.0))), "Heavier bench")

    @Test
    fun `a message is sent to the responder as part of the conversation`() = runTest {
        f.responder.answer("Hello")

        f.coach.send("How am I doing?")

        val turns = f.responder.requests.single().turns
        assertEquals(listOf(CoachRole.USER to "How am I doing?"), turns.map { it.role to it.text })
    }

    @Test
    fun `a message about a plan is shown with its topic and sent with it`() = runTest {
        f.responder.answer("Looks good")
        val topic = CoachTopic(TopicKind.PLAN, "12", "Push A")

        f.coach.send("Is this too much?", topic)

        assertEquals(topic, f.coach.observeMessages().first().first().topic)
        assertEquals(topic, f.responder.requests.single().turns.single().topic)
    }

    @Test
    fun `a deleted message is gone from the chat and from what the responder sees next`() = runTest {
        f.responder.answer("One")
        f.responder.answer("Two")
        f.coach.send("forget this")
        val forgotten = f.coach.observeMessages().first().first()

        f.coach.deleteMessage(forgotten.id)
        f.coach.send("hello")

        assertEquals(listOf("One", "hello", "Two"), f.coach.observeMessages().first().map { it.text })
        assertEquals(listOf("One", "hello"), f.responder.requests.last().turns.map { it.text })
    }

    @Test
    fun `an answer is saved with what the coach checked`() = runTest {
        f.responder.answer("You trained once.", checked = listOf("workouts"))

        f.coach.send("How was my week?")

        val messages = f.coach.observeMessages().first()
        assertEquals(listOf(CoachRole.USER, CoachRole.COACH), messages.map { it.role })
        assertEquals("You trained once.", messages.last().text)
        assertEquals(listOf("workouts"), messages.last().checked)
        assertEquals(CoachActivity.Idle, f.coach.observeActivity().first())
    }

    @Test
    fun `a failure keeps the message, shows the error, and retry asks again`() = runTest {
        f.responder.fail(CoachFailure.NO_CONNECTION)

        f.coach.send("Plan me a push day")

        assertEquals(1, f.coach.observeMessages().first().size)
        assertEquals(CoachActivity.Failed(CoachFailure.NO_CONNECTION, "failure"), f.coach.observeActivity().first())
        f.responder.answer("Sure.")
        f.coach.retry()
        assertEquals(listOf(CoachRole.USER, CoachRole.COACH), f.coach.observeMessages().first().map { it.role })
        assertEquals(2, f.responder.requests.size)
    }

    @Test
    fun `readiness is the readiness of the responder`() = runTest {
        assertEquals(CoachReadiness.READY, f.coach.observeReadiness().first())

        f.responder.readiness.value = CoachReadiness.NEEDS_SETUP

        assertEquals(CoachReadiness.NEEDS_SETUP, f.coach.observeReadiness().first())
    }

    @Test
    fun `proposals made while answering are attached to the answer`() = runTest {
        f.responder.proposeAndAnswer("I wrote a plan.", pushA)

        f.coach.send("Plan please")

        val answer = f.coach.observeMessages().first().last()
        assertEquals("Heavier bench", answer.proposals.single().summary)
    }

    @Test
    fun `a proposed plan waits until the user accepts, then it is a plan by the coach with the proposal as its note`() = runTest {
        val id = f.proposeId(pushA)

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
    fun `a newer proposal for the same plan supersedes the older one`() = runTest {
        val planId = f.plans.createPlan(PlanDraft("Push A", listOf(benchSet())), PlanAuthor.USER)
        fun change(weight: Double) = ProposalDraft.Plan(ProposedPlan(planId, "Push A", listOf(benchSet(weight))), "x")
        val first = f.proposeId(change(82.5))
        val second = f.proposeId(change(85.0))

        assertEquals(ProposalState.SUPERSEDED, f.coach.observeProposal(first).first()!!.state)
        assertEquals(listOf(second), f.coach.observePendingProposals().first().map { it.id })
        f.coach.accept(second)
        assertEquals(85.0, f.plans.observePlan(planId).first()!!.sets.single().weightKg)
    }

    @Test
    fun `accepting a target change moves the target and leaves plans untouched`() = runTest {
        f.plans.createPlan(PlanDraft("Legs", listOf(PlannedSet("squat", 5, 100.0, null, false))), PlanAuthor.USER)
        val id = f.proposeId(ProposalDraft.Target("LEGS", 8, 12, "recovery", "Legs: 10-20 to 8-12"))
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
    fun `undoing an accepted new plan deletes it`() = runTest {
        val id = f.proposeId(pushA)
        f.coach.accept(id)
        assertTrue(f.coach.observeProposal(id).first()!!.isUndoable)

        assertTrue(f.coach.undo(id))

        assertEquals(emptyList(), f.plans.observeUpcoming().first())
        val proposal = f.coach.observeProposal(id).first()!!
        assertEquals(ProposalState.UNDONE, proposal.state)
        assertTrue(!proposal.isUndoable)
    }

    @Test
    fun `undoing an accepted plan change brings back the plan as it was`() = runTest {
        val planId = f.plans.createPlan(PlanDraft("Push A", listOf(benchSet()), coachNote = "Old note"), PlanAuthor.COACH)
        val id = f.proposeId(ProposalDraft.Plan(ProposedPlan(planId, "Push B", listOf(PlannedSet("squat", 5, 100.0, null, false))), "Heavier"))
        f.coach.accept(id)

        f.coach.undo(id)

        val plan = f.plans.observePlan(planId).first()!!
        assertEquals("Push A", plan.name)
        assertEquals(listOf(benchSet()), plan.sets)
        assertEquals("Old note", plan.coachNote)
    }

    @Test
    fun `undoing an accepted target change brings back the previous target`() = runTest {
        val id = f.proposeId(ProposalDraft.Target("LEGS", 8, 12, "recovery", "Legs"))
        f.coach.accept(id)

        f.coach.undo(id)

        val legs = f.volume.observeTargets().first().single { it.groupKey == "LEGS" }
        assertEquals(10 to 20, legs.minSets to legs.maxSets)
        assertEquals(TargetSource.DEFAULT, legs.source)
    }

    @Test
    fun `a plan used for a workout or a target changed by hand can no longer be undone`() = runTest {
        val planProposal = f.proposeId(pushA)
        val targetProposal = f.proposeId(ProposalDraft.Target("LEGS", 8, 12, "r", "Legs"))
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
    fun `a change of the running workout replaces the sets of its exercises and can be undone until the workout ends`() = runTest {
        f.workouts.startSession()
        val session = f.workouts.observeActiveSession().first()!!.id
        f.workouts.addExercise(session, "bench")
        f.workouts.addExercise(session, "squat")
        val id = f.proposeId(ProposalDraft.WorkoutChange(null, listOf(benchSet(60.0), benchSet(60.0)), emptyList(), "Lighter bench"))

        f.coach.accept(id)

        val sets = f.workouts.observeSets(session).first().sortedBy { it.positionInSession }
        assertEquals(listOf("bench" to 60.0, "bench" to 60.0, "squat" to 0.0), sets.map { it.exerciseKey to it.weightKg })
        f.coach.undo(id)
        assertEquals(listOf("bench" to 0.0, "squat" to 0.0), f.workouts.observeSets(session).first().sortedBy { it.positionInSession }.map { it.exerciseKey to it.weightKg })

        val second = f.proposeId(ProposalDraft.WorkoutChange(null, emptyList(), listOf("squat"), "No squats"))
        f.coach.accept(second)
        assertEquals(listOf("bench"), f.workouts.observeSets(session).first().map { it.exerciseKey })
        f.workouts.finishSession(session)
        assertTrue(!f.coach.observeProposal(second).first()!!.isUndoable)
    }

    @Test
    fun `a finished workout can be corrected by the coach`() = runTest {
        f.finishBenchWorkout(weight = 800.0)
        val sessionId = f.workouts.observeRecentSessions().first().single().id

        f.coach.accept(f.proposeId(ProposalDraft.WorkoutChange(sessionId, listOf(benchSet(80.0, reps = 10)), emptyList(), "800 kg was a typo")))

        assertEquals(80.0, f.workouts.observeSets(sessionId).first().single().weightKg)
    }

    @Test
    fun `a new exercise proposed by the coach exists once accepted`() = runTest {
        val id = f.proposeId(ProposalDraft.Exercise(NewExercise("Landmine Press", "SHOULDERS", listOf("CHEST")), "For your shoulder"))

        f.coach.accept(id)

        val created = f.exercises.exercises().single { it.namesByLanguage["en"] == "Landmine Press" }
        assertEquals(listOf("SHOULDERS"), created.primaryMuscles.map { it.key })
        assertTrue(!f.coach.observeProposal(id).first()!!.isUndoable)
    }

    @Test
    fun `an accepted nutrition target is kept and can be undone while it is unchanged`() = runTest {
        val id = f.proposeId(ProposalDraft.Nutrition(2400, 160, "lean bulk", "2400 kcal, 160 g protein"))

        f.coach.accept(id)

        val target = assertNotNull(f.nutrition.observeTarget().first())
        assertEquals(2400 to 160, target.kcal to target.proteinG)
        assertTrue(f.coach.undo(id))
        assertEquals(null, f.nutrition.observeTarget().first())
    }

    @Test
    fun `a plan or workout changed by hand after accepting can no longer be undone`() = runTest {
        val planProposal = f.proposeId(pushA)
        f.coach.accept(planProposal)
        val plan = f.plans.observeUpcoming().first().single()
        f.plans.updatePlan(plan.id, PlanDraft(plan.name, plan.sets.map { it.copy(reps = 5) }))
        assertTrue(!f.coach.observeProposal(planProposal).first()!!.isUndoable)
        assertTrue(!f.coach.undo(planProposal))
        assertEquals(5, f.plans.observePlan(plan.id).first()!!.sets.single().reps)

        f.workouts.startSession()
        val session = f.workouts.observeActiveSession().first()!!.id
        f.workouts.addExercise(session, "bench")
        val change = f.proposeId(ProposalDraft.WorkoutChange(null, listOf(benchSet(60.0)), emptyList(), "x"))
        f.coach.accept(change)
        f.workouts.changeSet(f.workouts.observeSets(session).first().single().id, SetChange.WeightTo(65.0))
        assertTrue(!f.coach.observeProposal(change).first()!!.isUndoable)
        assertTrue(!f.coach.undo(change))
    }

    @Test
    fun `several plans in one proposal are created together`() = runTest {
        val id = f.proposeId(
            ProposalDraft.Plans(
                listOf(ProposedPlan(null, "A", listOf(benchSet())), ProposedPlan(null, "B", listOf(PlannedSet("squat", 5, 0.0, null, false)))),
                "Week",
            ),
        )

        f.coach.acceptAll(listOf(id))

        assertEquals(setOf("A", "B"), f.plans.observeUpcoming().first().map { it.name }.toSet())
    }

    private suspend fun finishBenchWorkoutLastWeek() {
        f.now = Instant.parse("2026-11-05T10:00:00Z")
        f.finishBenchWorkout()
        f.now = Instant.parse("2026-11-10T10:00:00Z")
    }

    @Test
    fun `last weeks review is posted once with its numbers and a proposal for the next week`() = runTest {
        finishBenchWorkoutLastWeek()
        f.responder.proposeAndAnswer(
            "A solid week.",
            ProposalDraft.Plans(
                listOf(ProposedPlan(null, "Push A", listOf(benchSet(82.5))), ProposedPlan(null, "Legs B", listOf(PlannedSet("squat", 5, 110.0, null, false)))),
                "Next week",
            ),
        )

        f.coach.reviewLastWeekIfDue()
        f.coach.reviewLastWeekIfDue()

        val message = f.coach.observeMessages().first().single()
        assertEquals("A solid week.", message.text)
        val review = assertNotNull(message.review)
        assertEquals(1, review.workouts)
        assertEquals(LocalDate(2026, 11, 2), review.weekStart)
        assertEquals(ProposalType.MULTI_PLAN, message.proposals.single().type)
        assertEquals(2, message.proposals.single().plans.size)
        assertEquals(review, f.responder.requests.single().review)
    }

    @Test
    fun `a week with nothing to review is skipped without asking the responder`() = runTest {
        f.coach.reviewLastWeekIfDue()

        assertEquals(emptyList(), f.coach.observeMessages().first())
        assertEquals(0, f.responder.requests.size)
    }

    @Test
    fun `a review that failed is tried again the next time`() = runTest {
        finishBenchWorkoutLastWeek()
        f.responder.fail(CoachFailure.NO_CONNECTION)
        f.coach.reviewLastWeekIfDue()
        f.responder.answer("Good week.")

        f.coach.reviewLastWeekIfDue()

        assertEquals("Good week.", f.coach.observeMessages().first().single().text)
    }
}
