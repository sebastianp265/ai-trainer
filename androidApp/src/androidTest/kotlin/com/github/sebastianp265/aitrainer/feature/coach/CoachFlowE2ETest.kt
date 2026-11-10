package com.github.sebastianp265.aitrainer.feature.coach

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.feature.settings.SettingsScreen
import com.github.sebastianp265.aitrainer.feature.workout.TrainingScreen
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeFailureKind
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BottomBarScreen
import com.github.sebastianp265.aitrainer.shared.e2e.FakeClaudeClient
import com.github.sebastianp265.aitrainer.shared.e2e.PlanScreen
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class CoachFlowE2ETest : AbstractE2ETest() {

    private companion object {
        const val PUSH_PLAN = """{"name":"Push A","summary":"Push day with a heavier bench","sets":[{"exerciseKey":"BARBELL_BENCH_PRESS","reps":8,"weightKg":80,"rpe":8}]}"""
    }

    private val claude = FakeClaudeClient()
    private val tabs = BottomBarScreen(compose)
    private val coach = CoachScreen(compose)
    private val settings = SettingsScreen(compose)
    private val plans = PlanScreen(compose)
    private val training = TrainingScreen(compose)

    @BeforeTest
    fun start() = launchApp(claudeClient = claude)

    @Test
    fun `without a key the Coach tab leads to the key screen`() {
        tabs.open("Coach")

        coach.assertKeyPrompt()
        coach.openApiKeyFromPrompt()

        settings.assertNoClaudeApiKeySaved()
    }

    @Test
    fun `the first conversation offers suggestions that start it`() {
        enterKeyAndOpenCoach()
        coach.assertGreeting()
        claude.answerWith("Sure, here is a plan outline.")

        coach.tapSuggestion("Write me a plan for this week")

        coach.assertYouSaid("Write me a plan for this week")
        coach.assertCoachSaid("Sure, here is a plan outline.")
    }

    @Test
    fun `a requested plan is proposed and accepted and found on the Plan tab`() {
        enterKeyAndOpenCoach()
        claude.answerWithToolCall("propose_plan", PUSH_PLAN)
        claude.answerWith("I wrote a push day for you.")

        coach.send("Plan a push day")

        coach.assertCoachSaid("I wrote a push day for you.")
        coach.assertProposalShown("Push day with a heavier bench")
        coach.accept()
        coach.assertProposalState("ACCEPTED")
        tabs.open("Plan")
        plans.assertListed("Push A")
    }

    @Test
    fun `the answer says what the coach read`() {
        enterKeyAndOpenCoach()
        claude.answerWithToolCall("get_plans", "{}")
        claude.answerWith("You have no plans yet.")

        coach.send("What plans do I have?")

        coach.assertCoachSaid("You have no plans yet.")
        coach.assertChecked("plans")
    }

    @Test
    fun `the preview shows the proposed exercises and accepting it creates the plan`() {
        enterKeyAndOpenCoach()
        claude.answerWithToolCall("propose_plan", PUSH_PLAN)
        claude.answerWith("Here you go.")
        coach.send("Plan a push day")
        coach.assertCoachSaid("Here you go.")

        coach.openPreview()

        coach.assertPreviewShown()
        coach.assertPreviewShows("Barbell Bench Press")
        coach.accept()
        tabs.open("Plan")
        plans.assertListed("Push A")
    }

    @Test
    fun `a pending proposal is also shown on the Training tab`() {
        enterKeyAndOpenCoach()
        claude.answerWithToolCall("propose_plan", PUSH_PLAN)
        claude.answerWith("Proposal ready.")
        coach.send("Plan a push day")
        coach.assertCoachSaid("Proposal ready.")

        tabs.open("Training")

        coach.assertPendingProposalBanner()
    }

    @Test
    fun `without a connection the message is kept and can be sent again`() {
        enterKeyAndOpenCoach()
        claude.failWith(ClaudeFailureKind.NO_NETWORK)

        coach.send("Add something for abs")

        coach.assertOfflineBanner()
        coach.assertNotSent()
        claude.answerWith("Added planks.")
        coach.retry()
        coach.assertCoachSaid("Added planks.")
    }

    @Test
    fun `a rejected key leads to the key screen`() {
        enterKeyAndOpenCoach()
        claude.failWith(ClaudeFailureKind.KEY_REJECTED)

        coach.send("Hello")

        coach.assertErrorCard("The API key was rejected")
        coach.tapErrorAction()
        settings.assertKeyWorks()
    }

    @Test
    fun `an overloaded service can be retried and the message stays`() {
        enterKeyAndOpenCoach()
        claude.failWith(ClaudeFailureKind.OVERLOADED)

        coach.send("Hello again")

        coach.assertErrorCard("The coach did not answer")
        claude.answerWith("Back now.")
        coach.tapErrorAction()
        coach.assertCoachSaid("Back now.")
        coach.assertYouSaid("Hello again")
    }

    @Test
    fun `notes written by the coach are listed in its memory and can be deleted`() {
        enterKeyAndOpenCoach()
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
        enterKeyAndOpenCoach()

        coach.openMemory()
        coach.editProfile("goal", "Get stronger")

        coach.assertProfile("Get stronger")
    }

    private fun enterKeyAndOpenCoach() {
        settings.open()
        settings.openApiKey()
        settings.typeClaudeApiKey("test-key")
        settings.saveAndCheck()
        settings.assertKeyWorks()
        settings.goBack()
        settings.goBackToTab()
        tabs.open("Coach")
    }
}
