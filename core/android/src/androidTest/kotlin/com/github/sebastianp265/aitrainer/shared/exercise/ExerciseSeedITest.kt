package com.github.sebastianp265.aitrainer.shared.exercise

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.module.androidModules
import com.github.sebastianp265.aitrainer.integration.module.commonModules
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class ExerciseSeedITest {

    private companion object {
        const val DATABASE_NAME = "aitrainer.db"
    }

    private val supportedLanguages = setOf("en", "pl")

    private lateinit var app: KoinApplication
    private lateinit var exercises: List<Exercise>

    @BeforeTest
    fun seedDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(DATABASE_NAME)
        app = koinApplication {
            androidContext(context)
            modules(commonModules + androidModules + module { single<Clock> { Clock.System } })
        }
        exercises = runBlocking { app.koin.get<ExerciseService>().exercises() }
    }

    @AfterTest
    fun closeDatabase() = app.close()

    @Test
    fun `seed contains exercises`() {
        assertTrue(exercises.isNotEmpty())
    }

    @Test
    fun `every exercise is named in every supported language`() {
        assertTrue(exercises.all { it.namesByLanguage.keys == supportedLanguages })
    }

    @Test
    fun `every muscle group and equipment is named in every supported language`() {
        val muscleGroups = exercises.flatMap { it.primaryMuscles + it.secondaryMuscles }
        assertTrue(muscleGroups.all { it.namesByLanguage.keys == supportedLanguages })
        assertTrue(exercises.all { it.equipment.namesByLanguage.keys == supportedLanguages })
    }

    @Test
    fun `exercise keys are unique`() {
        assertEquals(exercises.size, exercises.map { it.key }.toSet().size)
    }
}
