package com.github.sebastianp265.aitrainer.feature.coach

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.feature.settings.SettingsScreen
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BottomBarScreen
import com.github.sebastianp265.aitrainer.shared.e2e.FakeClaudeClient
import com.github.sebastianp265.aitrainer.shared.e2e.PlanScreen
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock

@RunWith(AndroidJUnit4::class)
class WeeklyReviewFlowE2ETest : AbstractE2ETest() {

    private val nextWeek: String
        get() {
            val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
            val first = today.plus(DatePeriod(days = 1))
            val second = today.plus(DatePeriod(days = 2))
            return """{"summary":"Next week","plans":[
                {"name":"Push A","date":"$first","sets":[{"exerciseKey":"BARBELL_BENCH_PRESS","reps":8,"weightKg":82.5}]},
                {"name":"Legs B","date":"$second","sets":[{"exerciseKey":"BACK_SQUAT","reps":5,"weightKg":110}]}]}"""
        }

    private val claude = FakeClaudeClient()
    private val tabs = BottomBarScreen(compose)
    private val coach = CoachScreen(compose)
    private val settings = SettingsScreen(compose)
    private val plans = PlanScreen(compose)

    @BeforeTest
    fun start() = launchApp(claudeClient = claude)

    @Test
    fun `opening the app after a week with workouts posts the review once`() {
        settings.open()
        settings.openApiKey()
        settings.typeClaudeApiKey("test-key")
        settings.saveAndCheck()
        settings.assertKeyWorks()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        val monday = today.minus(DatePeriod(days = today.dayOfWeek.ordinal))
        insertFinishedBenchWorkout(monday.minus(DatePeriod(days = 5)))
        claude.answerWithToolCall("propose_plans", nextWeek)
        claude.answerWith("A solid week.")

        restartApp()
        tabs.open("Coach")

        coach.assertReviewShown()
        coach.assertCoachSaid("A solid week.")
        restartApp()
        tabs.open("Coach")
        coach.assertCoachSaid("A solid week.")
        assertEquals(2, claude.requests.size)
    }

    @Test
    fun `the weekly proposal is accepted with one tap`() {
        settings.open()
        settings.openApiKey()
        settings.typeClaudeApiKey("test-key")
        settings.saveAndCheck()
        settings.assertKeyWorks()
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        insertFinishedBenchWorkout(today.minus(DatePeriod(days = today.dayOfWeek.ordinal + 5)))
        claude.answerWithToolCall("propose_plans", nextWeek)
        claude.answerWith("Here is next week.")
        restartApp()
        tabs.open("Coach")
        coach.assertCoachSaid("Here is next week.")

        coach.acceptAll()
        coach.assertProposalState("ACCEPTED")

        tabs.open("Plan")
        plans.assertListed("Push A")
        plans.assertListed("Legs B")
    }
}
