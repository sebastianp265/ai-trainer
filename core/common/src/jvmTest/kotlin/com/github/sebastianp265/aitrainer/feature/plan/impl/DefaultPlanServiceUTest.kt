package com.github.sebastianp265.aitrainer.feature.plan.impl

import com.github.sebastianp265.aitrainer.feature.workout.impl.DefaultWorkoutService
import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import com.github.sebastianp265.aitrainer.integration.plan.PlanDraft
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.shared.database.impl.inMemoryDatabase
import com.github.sebastianp265.aitrainer.shared.database.impl.seedExercises
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultPlanServiceUTest {

    private val clock = object : Clock {
        override fun now() = Instant.parse("2026-11-10T10:00:00Z")
    }
    private val database = inMemoryDatabase().also { it.seedExercises("bench", "squat") }
    private val plans = DefaultPlanService(database.planDao(), clock)
    private val workouts = DefaultWorkoutService(database.workoutDao(), database.planDao(), plans, clock)

    @AfterTest
    fun close() = database.close()

    private fun set(exercise: String, reps: Int = 8, weight: Double = 80.0, rpe: Double? = null, warmup: Boolean = false) =
        PlannedSet(exercise, reps, weight, rpe, warmup)

    private fun draft(name: String, vararg sets: PlannedSet) =
        PlanDraft(name, sets.toList().ifEmpty { listOf(set("bench")) })

    @Test
    fun `upcoming plans are in the order they were made`() = runBlocking {
        plans.createPlan(draft("First"), PlanAuthor.USER)
        plans.createPlan(draft("Second"), PlanAuthor.COACH)
        plans.createPlan(draft("Third"), PlanAuthor.USER)

        assertEquals(listOf("First", "Second", "Third"), plans.observeUpcoming().first().map { it.name })
    }

    @Test
    fun `a plan keeps its author and sets in order`() = runBlocking {
        val id = plans.createPlan(
            draft("Push", set("bench", 10, 40.0, warmup = true), set("bench", 8, 82.5, 8.0), set("squat", 5, 110.0)),
            PlanAuthor.COACH,
        )

        val plan = plans.observePlan(id).first()!!

        assertEquals(PlanAuthor.COACH, plan.author)
        assertEquals(listOf("bench", "bench", "squat"), plan.sets.map { it.exerciseKey })
        assertEquals(listOf(true, false, false), plan.sets.map { it.isWarmup })
        assertEquals(8.0, plan.sets[1].rpe)
    }

    @Test
    fun `updating a plan replaces its name, sets and coach note`() = runBlocking {
        val id = plans.createPlan(draft("Old", set("bench"), set("bench")).copy(coachNote = "Lighter week"), PlanAuthor.COACH)

        plans.updatePlan(id, draft("New", set("squat", 5, 100.0)))

        val plan = plans.observePlan(id).first()!!
        assertEquals("New", plan.name)
        assertEquals(listOf("squat"), plan.sets.map { it.exerciseKey })
        assertEquals(null, plan.coachNote)
    }

    @Test
    fun `a deleted plan is gone`() = runBlocking {
        val id = plans.createPlan(draft("Gone"), PlanAuthor.USER)

        plans.deletePlan(id)

        assertNull(plans.observePlan(id).first())
        assertEquals(emptyList(), plans.observeUpcoming().first())
    }

    @Test
    fun `a plan needs a name and at least one set`() = runBlocking<Unit> {
        assertFailsWith<IllegalArgumentException> { plans.createPlan(PlanDraft("  ", listOf(set("bench"))), PlanAuthor.USER) }
        assertFailsWith<IllegalArgumentException> { plans.createPlan(PlanDraft("Empty", emptyList()), PlanAuthor.USER) }
    }

    @Test
    fun `a workout started from a plan is filled with its sets`() = runBlocking {
        val id = plans.createPlan(draft("Push", set("bench", 10, 40.0, warmup = true), set("bench", 8, 82.5, 8.0)), PlanAuthor.USER)

        workouts.startSession(id)

        val session = workouts.observeActiveSession().first()!!
        val sets = workouts.observeSets(session.id).first()
        assertEquals(id, session.planId)
        assertEquals(listOf(true, false), sets.map { it.isWarmup })
        assertEquals(listOf(40.0, 82.5), sets.map { it.weightKg })
        assertEquals(8.0, sets.last().rpe)
    }

    @Test
    fun `a plan leaves the upcoming list only when its workout is finished`() = runBlocking {
        val id = plans.createPlan(draft("Push"), PlanAuthor.USER)
        workouts.startSession(id)
        assertEquals(listOf(id), plans.observeUpcoming().first().map { it.id })

        workouts.finishSession(workouts.observeActiveSession().first()!!.id)

        assertEquals(emptyList(), plans.observeUpcoming().first())
    }

    @Test
    fun `a discarded workout leaves its plan upcoming`() = runBlocking {
        val id = plans.createPlan(draft("Push"), PlanAuthor.USER)
        workouts.startSession(id)

        workouts.discardSession(workouts.observeActiveSession().first()!!.id)

        assertEquals(listOf(id), plans.observeUpcoming().first().map { it.id })
    }

    @Test
    fun `deleting the finished workout brings the plan back`() = runBlocking {
        val id = plans.createPlan(draft("Push"), PlanAuthor.USER)
        workouts.startSession(id)
        val sessionId = workouts.observeActiveSession().first()!!.id
        workouts.finishSession(sessionId)

        workouts.discardSession(sessionId)

        assertEquals(listOf(id), plans.observeUpcoming().first().map { it.id })
    }
}
