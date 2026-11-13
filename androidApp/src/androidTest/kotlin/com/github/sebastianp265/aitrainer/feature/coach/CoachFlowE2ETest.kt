package com.github.sebastianp265.aitrainer.feature.coach

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.feature.settings.SettingsScreen
import com.github.sebastianp265.aitrainer.integration.ai.AiFailureKind
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BottomBarScreen
import com.github.sebastianp265.aitrainer.shared.e2e.BENCH_PRESS
import com.github.sebastianp265.aitrainer.shared.e2e.FakeAiClient
import com.github.sebastianp265.aitrainer.shared.e2e.LogWorkoutScreen
import com.github.sebastianp265.aitrainer.shared.e2e.sets
import com.github.sebastianp265.aitrainer.shared.e2e.PlanScreen
import org.junit.runner.RunWith
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class CoachFlowE2ETest : AbstractE2ETest() {

    private companion object {
        const val LIGHTER_BENCH = """{"summary":"Lighter bench","sets":[{"exerciseKey":"BARBELL_BENCH_PRESS","reps":8,"weightKg":70},{"exerciseKey":"BARBELL_BENCH_PRESS","reps":8,"weightKg":70}]}"""
        const val PUSH_PLAN = """{"name":"Push A","summary":"Push day with a heavier bench","sets":[{"exerciseKey":"BARBELL_BENCH_PRESS","reps":8,"weightKg":80,"rpe":8}]}"""
    }

    private val claude = FakeAiClient()
    private val tabs = BottomBarScreen(compose)
    private val coach = CoachScreen(compose)
    private val settings = SettingsScreen(compose)
    private val plans = PlanScreen(compose)
    private val workout = LogWorkoutScreen(compose)

    @Test
    fun `without a key the Coach tab leads to the key screen`() {
        launchApp(claudeClient = claude)
        tabs.open("Coach")
        coach.assertKeyPrompt()
        coach.openApiKeyFromPrompt()
        settings.assertNoClaudeApiKeySaved()
    }

    @Test
    fun `the first conversation offers suggestions that start it`() {
        openCoachWithKey()
        coach.assertGreeting()
        claude.answerWith("Sure, here is a plan outline.")
        coach.tapSuggestion("Write me a plan for this week")
        coach.assertYouSaid("Write me a plan for this week")
        coach.assertCoachSaid("Sure, here is a plan outline.")
    }

    @Test
    fun `a message held down can be deleted from the chat`() {
        openCoachWithKey()
        claude.answerWith("Noted, three times a week.")
        coach.send("I train three times a week")
        coach.assertCoachSaid("Noted, three times a week.")

        coach.deleteMessage("I train three times a week")

        coach.assertNotShown("I train three times a week")
        coach.assertCoachSaid("Noted, three times a week.")
    }

    @Test
    fun `a proposed plan accepted from its card is offered when starting a workout until it is undone`() {
        openCoachWithKey()
        claude.answerWithToolCall("propose_plan", PUSH_PLAN)
        claude.answerWith("I wrote a push day for you.")
        coach.send("Plan a push day")
        coach.assertProposalShown("Push day with a heavier bench")
        coach.accept()
        coach.assertProposalState("ACCEPTED")
        tabs.open("Training")
        plans.assertListed("Push A")
        tabs.open("Coach")
        coach.undo()
        coach.assertProposalState("UNDONE")
        tabs.open("Training")
        plans.assertNotListed("Push A")
    }

    @Test
    fun `the preview shows the proposed exercises and can accept the proposal`() {
        openCoachWithKey()
        claude.answerWithToolCall("propose_plan", PUSH_PLAN)
        claude.answerWith("Here you go.")
        coach.send("Plan a push day")
        coach.openPreview()
        coach.assertPreviewShows("Barbell Bench Press")
        coach.accept()
        coach.assertProposalState("ACCEPTED")
    }

    @Test
    fun `a pending proposal is also shown on the Training tab`() {
        openCoachWithKey()
        claude.answerWithToolCall("propose_plan", PUSH_PLAN)
        claude.answerWith("Proposal ready.")
        coach.send("Plan a push day")
        coach.assertProposalShown("Push day with a heavier bench")
        tabs.open("Training")
        coach.assertPendingProposalBanner()
    }

    @Test
    fun `the answer says what the coach read`() {
        openCoachWithKey()
        claude.answerWithToolCall("get_plans", "{}")
        claude.answerWith("You have no plans yet.")
        coach.send("What plans do I have?")
        coach.assertCoachSaid("You have no plans yet.")
        coach.assertChecked("plans")
    }

    @Test
    fun `without a connection the message is kept and can be sent again`() {
        openCoachWithKey()
        claude.failWith(AiFailureKind.NO_NETWORK)
        coach.send("Add something for abs")
        coach.assertOfflineBanner()
        coach.assertNotSent()
        claude.answerWith("Added planks.")
        coach.retry()
        coach.assertCoachSaid("Added planks.")
    }

    @Test
    fun `a rejected key leads to the key screen`() {
        openCoachWithKey()
        claude.failWith(AiFailureKind.KEY_REJECTED)
        coach.send("Hello")
        coach.assertErrorCard("The API key was rejected")
        coach.tapErrorAction()
        settings.assertApiKeyScreenShown()
    }

    @Test
    fun `an overloaded service can be retried and the message stays`() {
        openCoachWithKey()
        claude.failWith(AiFailureKind.OVERLOADED)
        coach.send("Hello again")
        coach.assertErrorCard("The coach did not answer")
        claude.answerWith("Back now.")
        coach.tapErrorAction()
        coach.assertCoachSaid("Back now.")
        coach.assertYouSaid("Hello again")
    }

    @Test
    fun `notes written by the coach are listed in its memory and can be deleted`() {
        openCoachWithKey()
        claude.answerWithToolCall("add_note", """{"text":"Prefers early mornings"}""")
        claude.answerWith("Noted.")
        coach.send("I like training early")
        coach.assertCoachSaid("Noted.")
        coach.openMemory()
        coach.assertNote("Prefers early mornings")
        coach.deleteFirstNote()
        coach.assertNoNotes()
    }

    @Test
    fun `the profile can be edited by hand`() {
        openCoachWithKey()
        coach.openMemory()
        coach.editProfile("goal", "Get stronger")
        coach.assertProfile("Get stronger")
    }

    @Test
    fun `asking the coach from a plan sends the message about that plan`() {
        given.claudeApiKey()
        val planId = given.plan("Push A", sets(BENCH_PRESS, kg = 80.0))
        launchApp(claudeClient = claude)
        plans.openNext("Push A")
        plans.askCoach()
        coach.assertTopic("About plan: Push A")
        claude.answerWith("It fits your week.")
        coach.send("Is this too much?")
        coach.assertYouSaid("Is this too much?")
        coach.assertCoachSaid("It fits your week.")
        coach.assertTopic("About plan: Push A")
        claude.assertFirstMessageMentions("plan id $planId")
    }

    @Test
    fun `during a workout the coach changes its sets after a quick question`() {
        given.claudeApiKey()
        given.runningWorkout(sets(BENCH_PRESS, count = 2, reps = 8, kg = 80.0))
        launchApp(claudeClient = claude)
        workout.askCoach()
        coach.assertTopic("About the workout in progress")
        claude.answerWithToolCall("propose_workout_change", LIGHTER_BENCH)
        claude.answerWith("Drop to 70 kg.")
        coach.tapQuickQuestion("This is too heavy")
        coach.assertCoachSaid("Drop to 70 kg.")
        coach.accept()
        coach.assertProposalState("ACCEPTED")
        coach.back()
        workout.assertSets(BENCH_PRESS, "70" to "8", "70" to "8")
    }

    private fun openCoachWithKey() {
        given.claudeApiKey()
        launchApp(claudeClient = claude)
        tabs.open("Coach")
    }
}
