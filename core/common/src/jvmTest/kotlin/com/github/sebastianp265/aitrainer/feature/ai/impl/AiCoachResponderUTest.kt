package com.github.sebastianp265.aitrainer.feature.ai.impl

import com.github.sebastianp265.aitrainer.integration.ai.AiCacheDuration
import com.github.sebastianp265.aitrainer.integration.ai.AiContent
import com.github.sebastianp265.aitrainer.integration.ai.AiEffort
import com.github.sebastianp265.aitrainer.integration.ai.AiFailureKind
import com.github.sebastianp265.aitrainer.integration.ai.AiRole
import com.github.sebastianp265.aitrainer.integration.ai.AiRun
import com.github.sebastianp265.aitrainer.integration.ai.AiThinking
import com.github.sebastianp265.aitrainer.integration.ai.AiTuning
import com.github.sebastianp265.aitrainer.integration.coach.CoachFailure
import com.github.sebastianp265.aitrainer.integration.coach.CoachReadiness
import com.github.sebastianp265.aitrainer.integration.coach.CoachReply
import com.github.sebastianp265.aitrainer.integration.coach.CoachRequest
import com.github.sebastianp265.aitrainer.integration.coach.CoachRole
import com.github.sebastianp265.aitrainer.integration.coach.CoachStep
import com.github.sebastianp265.aitrainer.integration.coach.CoachTopic
import com.github.sebastianp265.aitrainer.integration.coach.CoachTurn
import com.github.sebastianp265.aitrainer.integration.coach.ProposalDraft
import com.github.sebastianp265.aitrainer.integration.coach.TopicKind
import com.github.sebastianp265.aitrainer.integration.coach.WeeklyReviewSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class AiCoachResponderUTest {

    private val f = AiFixture()

    @AfterTest
    fun close() = f.database.close()

    private suspend fun reply(request: CoachRequest, steps: MutableList<CoachStep?> = mutableListOf()) =
        f.responder.reply(request, f.desk) { steps += it }

    @Test
    fun `the request carries profile, notes, targets, data summary and the conversation`() = runBlocking {
        f.memory.setProfileField("goal", "get stronger")
        f.memory.addNote("Left shoulder hurts on overhead press")
        f.volume.setTarget("ABS", 6, 10)
        f.finishBenchWorkout()
        f.client.answer("Hello")

        reply(requestOf("How am I doing?"))

        val request = f.client.requests.single()
        assertEquals("claude-sonnet-5-5", request.model)
        assertTrue("goal: get stronger" in request.system)
        assertTrue("Left shoulder hurts on overhead press" in request.system)
        assertTrue("ABS: 6-10 (user)" in request.system)
        assertTrue("CHEST: 10-20 (default)" in request.system)
        assertTrue("1 exercises, 1 working sets" in request.system)
        assertEquals(listOf(AiRole.USER), request.messages.map { it.role })
        assertEquals(AiContent.Text("How am I doing?"), request.messages.single().content.single())
        assertEquals(18, request.tools.size)
    }

    @Test
    fun `a turn about a plan tells the model which plan it is about`() = runBlocking {
        f.client.answer("Looks good")
        val topic = CoachTopic(TopicKind.PLAN, "12", "Push A")

        reply(CoachRequest(listOf(CoachTurn(CoachRole.USER, "Is this too much?", topic))))

        val sent = f.client.requests.single().messages.single().content.single() as AiContent.Text
        assertEquals("[The user asks about this plan id 12: \"Push A\"]\nIs this too much?", sent.text)
    }

    @Test
    fun `an answer lists what the tools read, and the tool result goes back to the model`() = runBlocking {
        f.finishBenchWorkout()
        f.client.callTool("get_workouts", "{}")
        f.client.answer("You trained once.")
        val steps = mutableListOf<CoachStep?>()

        val answer = assertIs<CoachReply.Answer>(reply(requestOf("How was my week?"), steps))

        assertEquals("You trained once.", answer.text)
        assertEquals(listOf("workouts"), answer.checked)
        assertEquals(listOf<CoachStep?>(CoachStep.WORKOUTS), steps)
        val toolResult = f.client.requests.last().messages.last().content.single() as AiContent.ToolResult
        assertTrue("Bench Press" in toolResult.content)
    }

    @Test
    fun `a proposal made by a tool goes through the desk`() = runBlocking {
        f.client.callTool("propose_plan", """{"name":"Push A","summary":"Heavier bench","sets":[{"exerciseKey":"bench","reps":8,"weightKg":82.5,"rpe":8.0}]}""")
        f.client.answer("I wrote a plan.")

        reply(requestOf("Plan please"))

        val draft = assertIs<ProposalDraft.Plan>(f.desk.drafts.single())
        assertEquals("Heavier bench", draft.summary)
        assertEquals("Push A", draft.plan.name)
    }

    @Test
    fun `a failure of the client becomes a failure of the coach`() = runBlocking {
        val expected = mapOf(
            AiFailureKind.NO_KEY to CoachFailure.NEEDS_SETUP,
            AiFailureKind.KEY_REJECTED to CoachFailure.NEEDS_SETUP,
            AiFailureKind.NO_NETWORK to CoachFailure.NO_CONNECTION,
            AiFailureKind.OVERLOADED to CoachFailure.BUSY,
            AiFailureKind.OTHER to CoachFailure.OTHER,
        )
        expected.keys.forEach { f.client.fail(it) }

        val kinds = expected.keys.map { assertIs<CoachReply.Failure>(reply(requestOf("a"))).kind }

        assertEquals(expected.values.toList(), kinds)
    }

    @Test
    fun `token counts are added to this months usage`() = runBlocking {
        f.client.answer("One")
        f.client.answer("Two")

        reply(requestOf("a"))
        reply(requestOf("b"))

        val usage = f.ai.observeUsageThisMonth().first()
        assertEquals(2, usage.messages)
        assertEquals(200L, usage.inputTokens)
        assertEquals(40L, usage.outputTokens)
    }

    @Test
    fun `the chosen tuning and model are sent with every request`() = runBlocking {
        val tuning = AiTuning(effort = AiEffort.LOW, thinking = AiThinking.ADAPTIVE, maxTokens = 4_000, cacheDuration = AiCacheDuration.FIVE_MINUTES)
        f.ai.setTuning(tuning)
        f.ai.setModel("claude-haiku-5-5")
        f.client.answer("Sure.")

        reply(requestOf("a"))

        assertEquals(tuning, f.client.requests.single().tuning)
        assertEquals("claude-haiku-5-5", f.client.requests.single().model)
    }

    @Test
    fun `the last run adds up its rounds, tokens and time`() = runBlocking {
        f.client.whileAnswering = { f.now += 2.seconds }
        f.client.callTool("get_plans", "{}")
        f.client.answer("No plans yet.")

        reply(requestOf("a"))

        assertEquals(
            AiRun(rounds = 2, inputTokens = 200, cacheReadTokens = 0, cacheWriteTokens = 0, outputTokens = 40, duration = 4.seconds, stopReason = "end_turn"),
            f.ai.observeLastRun().first(),
        )
    }

    @Test
    fun `an accepted nutrition target is told to the model`() = runBlocking {
        f.nutrition.setTarget(2400, 160, "lean bulk")
        f.client.answer("ok")

        reply(requestOf("Hi"))

        assertTrue("kcal: 2400, protein: 160 g" in f.client.requests.single().system)
    }

    @Test
    fun `a weekly review is asked for with the numbers of the week`() = runBlocking {
        f.client.answer("A solid week.")
        val review = WeeklyReviewSummary(LocalDate(2026, 11, 2), LocalDate(2026, 11, 8), 1, 0, null, null, null, null, null, null)

        reply(CoachRequest(emptyList(), review))

        val sent = f.client.requests.single().messages.last().content.single() as AiContent.Text
        assertTrue("WEEKLY REVIEW of 2026-11-02 to 2026-11-08" in sent.text)
        assertTrue("gym workouts: 1" in sent.text)
    }

    @Test
    fun `the coach is ready only while an API key is stored`() = runBlocking {
        assertEquals(CoachReadiness.NEEDS_SETUP, f.responder.observeReadiness().first())

        f.ai.checkAndSaveApiKey("test-key")

        assertEquals(CoachReadiness.READY, f.responder.observeReadiness().first())
    }
}
