package com.github.sebastianp265.aitrainer.feature.coach

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.sebastianp265.aitrainer.shared.e2e.AbstractE2ETest
import com.github.sebastianp265.aitrainer.shared.e2e.BENCH_PRESS
import com.github.sebastianp265.aitrainer.shared.e2e.BottomBarScreen
import com.github.sebastianp265.aitrainer.shared.e2e.FakeAiClient
import com.github.sebastianp265.aitrainer.shared.e2e.PlanScreen
import com.github.sebastianp265.aitrainer.shared.e2e.sets
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.time.Clock

@RunWith(AndroidJUnit4::class)
class WeeklyReviewFlowE2ETest : AbstractE2ETest() {

    private val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
    private val wednesdayLastWeek = today.minus(DatePeriod(days = today.dayOfWeek.ordinal + 5))
    private val nextWeek = """{"summary":"Next week","plans":[
        {"name":"Push A","date":"${today.plus(DatePeriod(days = 1))}","sets":[{"exerciseKey":"BARBELL_BENCH_PRESS","reps":8,"weightKg":82.5}]},
        {"name":"Legs B","date":"${today.plus(DatePeriod(days = 2))}","sets":[{"exerciseKey":"BACK_SQUAT","reps":5,"weightKg":110}]}]}"""

    private val claude = FakeAiClient()
    private val tabs = BottomBarScreen(compose)
    private val coach = CoachScreen(compose)
    private val plans = PlanScreen(compose)

    @Test
    fun `the review of last week is shown when the app opens and its plans are accepted with one tap`() {
        given.claudeApiKey()
        given.finishedWorkout(sets(BENCH_PRESS, reps = 8, kg = 80.0), on = wednesdayLastWeek)
        claude.answerWithToolCall("propose_plans", nextWeek)
        claude.answerWith("Here is next week.")
        launchApp(claudeClient = claude)
        tabs.open("Coach")
        coach.assertReviewShown()
        coach.assertCoachSaid("Here is next week.")
        coach.acceptAll()
        coach.assertProposalState("ACCEPTED")
        tabs.open("Training")
        plans.assertListed("Push A")
        plans.assertListed("Legs B")
    }
}
