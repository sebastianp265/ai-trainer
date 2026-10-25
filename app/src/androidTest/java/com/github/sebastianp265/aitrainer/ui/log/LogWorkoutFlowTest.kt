package com.github.sebastianp265.aitrainer.ui.log

import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.click
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
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
class LogWorkoutFlowTest {

    @get:Rule
    val compose = createEmptyComposeRule()

    private lateinit var scenario: ActivityScenario<MainActivity>

    @BeforeTest
    fun startAppWithEmptyHistory() {
        val koin = GlobalContext.get()
        koin.get<AppDatabase>().clearAllTables()
        runBlocking { koin.get<ExerciseRepository>().ensureSeeded() }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    @AfterTest
    fun closeApp() = scenario.close()

    @Test
    fun loggedWorkoutIsSavedToHistory() {
        logBenchWorkoutWithTwoSets()

        waitForText("Workout saved")
        waitForText("Start workout")
        waitForCount(compose.onAllNodesWithText("1 exercises · 2 sets"), 1)
    }

    @Test
    fun nextWorkoutStartsFromLastWorkoutNumbers() {
        logBenchWorkoutWithTwoSets()

        startWorkout()
        addExercise("Barbell Bench Press")
        waitForText("7.5")
        waitForText("9")

        compose.onNodeWithText("Same as last time").performClick()
        waitForCount(compose.onAllNodesWithText("7.5"), 3)

        compose.onNodeWithText("Discard").performClick()
        compose.onNode(hasText("Discard") and hasAnyAncestor(isDialog())).performClick()
        waitForText("Start workout")
        waitForCount(compose.onAllNodesWithText("sets", substring = true), 1)
    }

    private fun logBenchWorkoutWithTwoSets() {
        startWorkout()
        addExercise("Barbell Bench Press")
        waitForText("0")

        // One gesture batch, so the taps land faster than the database round-trip.
        compose.onNodeWithContentDescription("Increase weight").performTouchInput {
            repeat(3) { click(); advanceEventTime(200) }
        }
        compose.onNodeWithContentDescription("Decrease reps").performClick()
        waitForText("7.5")
        waitForText("9")

        compose.onNodeWithText("+ Set").performClick()
        waitForCount(compose.onAllNodesWithContentDescription("Increase weight"), 2)
        waitForCount(compose.onAllNodesWithText("7.5"), 2)

        compose.onNodeWithText("Finish").performClick()
    }

    private fun startWorkout() {
        waitForText("Start workout")
        compose.onNodeWithText("Start workout").performClick()
        compose.waitUntil(TIMEOUT_MS) {
            compose.onAllNodesWithContentDescription("Add exercise").fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun addExercise(name: String) {
        compose.onNodeWithContentDescription("Add exercise").performClick()
        // A partial query, so the search field itself doesn't match the exercise name.
        compose.onNode(hasSetTextAction()).performTextInput(name.substringAfter(' '))
        waitForText(name)
        compose.onNode(hasText(name) and hasAnyAncestor(isDialog())).performClick()
    }

    // Database writes finish asynchronously, after Compose reports idle.
    private fun waitForText(text: String) =
        compose.waitUntil(TIMEOUT_MS) { compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty() }

    private fun waitForCount(nodes: SemanticsNodeInteractionCollection, count: Int) =
        compose.waitUntil(TIMEOUT_MS) { nodes.fetchSemanticsNodes().size == count }

    private companion object {
        const val TIMEOUT_MS = 5_000L
    }
}
