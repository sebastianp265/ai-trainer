package com.github.sebastianp265.aitrainer.ui.log

import android.app.LocaleManager
import android.os.LocaleList
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sebastianp265.aitrainer.MainActivity
import com.github.sebastianp265.aitrainer.data.db.AppDatabase
import com.github.sebastianp265.aitrainer.data.repo.ExerciseRepository
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@RunWith(AndroidJUnit4::class)
class LogWorkoutFlowITest {

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }

    @get:Rule
    val compose = createEmptyComposeRule()

    private val localeManager = InstrumentationRegistry.getInstrumentation().targetContext
        .getSystemService(LocaleManager::class.java)
    private lateinit var scenario: ActivityScenario<MainActivity>

    @BeforeTest
    fun startAppInEnglishWithEmptyHistory() {
        localeManager.applicationLocales = LocaleList.forLanguageTags("en")
        val koin = GlobalContext.get()
        koin.get<AppDatabase>().clearAllTables()
        runBlocking { koin.get<ExerciseRepository>().ensureSeeded() }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @AfterTest
    fun closeApp() {
        scenario.close()
        localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
    }

    @Test
    fun loggedWorkoutIsSavedToHistory() {
        logFinishedBenchWorkout()

        waitForText("Workout saved")
        waitForText("Start workout")
        waitForCount("1 exercise · 2 sets", 1)
    }

    @Test
    fun nextWorkoutStartsFromLastWorkoutNumbers() {
        logFinishedBenchWorkout()

        startWorkout()
        addExercise(searchQuery = "Bench Press", exerciseToPick = "Barbell Bench Press")
        assertSetsInCard("Barbell Bench Press", "7.5" to "9")

        compose.onNodeWithText("Same as last time").performClick()
        assertSetsInCard("Barbell Bench Press", "7.5" to "9", "7.5" to "9", "10" to "9")

        compose.onNodeWithText("Discard").performClick()
        compose.onNode(hasText("Discard") and hasAnyAncestor(isDialog())).performClick()
        waitForText("Start workout")
        waitForCount("1 exercise · 2 sets", 1)
    }

    private fun logFinishedBenchWorkout() {
        startWorkout()
        addExercise(searchQuery = "Bench Press", exerciseToPick = "Barbell Bench Press")
        assertSetsInCard("Barbell Bench Press", "0" to "10")

        val increaseWeight = hasContentDescription("Increase weight") and inCard("Barbell Bench Press")
        compose.onNode(increaseWeight).tapRapidly(times = 3)
        compose.onNode(hasContentDescription("Decrease reps") and inCard("Barbell Bench Press")).performClick()
        assertSetsInCard("Barbell Bench Press", "7.5" to "9")

        compose.onNodeWithText("+ Set").performClick()
        assertSetsInCard("Barbell Bench Press", "7.5" to "9", "7.5" to "9")
        compose.onAllNodes(increaseWeight)[1].performClick()
        assertSetsInCard("Barbell Bench Press", "7.5" to "9", "10" to "9")

        compose.onNodeWithText("Finish").performClick()
    }

    private fun startWorkout() {
        waitForText("Start workout")
        compose.onNodeWithText("Start workout").performClick()
        compose.waitUntil(TIMEOUT_MS) {
            compose.onAllNodesWithContentDescription("Add exercise").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun addExercise(searchQuery: String, exerciseToPick: String) {
        compose.onNodeWithContentDescription("Add exercise").performClick()
        compose.onNode(hasSetTextAction()).performTextInput(searchQuery)
        waitForText(exerciseToPick)
        compose.onNode(hasText(exerciseToPick) and hasAnyAncestor(isDialog())).performClick()
    }

    private fun SemanticsNodeInteraction.tapRapidly(times: Int) = performTouchInput {
        repeat(times) { click(); advanceEventTime(200) }
    }

    private fun inCard(exerciseName: String) = hasAnyAncestor(hasTestTag(LogWorkoutTags.exerciseCard(exerciseName)))

    private fun weightAndRepsPerSetInCard(exerciseName: String): List<Pair<String, String>> {
        fun values(tag: String) = compose.onAllNodes(hasTestTag(tag) and inCard(exerciseName), useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { node -> node.config[SemanticsProperties.Text].joinToString("") { it.text } }
        val weights = values(LogWorkoutTags.WEIGHT)
        val reps = values(LogWorkoutTags.REPS)
        check(weights.size == reps.size) { "weights $weights vs reps $reps" }
        return weights.zip(reps)
    }

    private fun assertSetsInCard(exerciseName: String, vararg expected: Pair<String, String>) {
        try {
            compose.waitUntil(TIMEOUT_MS) { weightAndRepsPerSetInCard(exerciseName) == expected.toList() }
        } catch (e: ComposeTimeoutException) {
            throw AssertionError("$exerciseName: expected sets ${expected.toList()}, shown ${weightAndRepsPerSetInCard(exerciseName)}", e)
        }
    }

    private fun waitForText(text: String) =
        compose.waitUntil(TIMEOUT_MS) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun waitForCount(text: String, count: Int) =
        compose.waitUntil(TIMEOUT_MS) { compose.onAllNodesWithText(text).fetchSemanticsNodes().size == count }
}
