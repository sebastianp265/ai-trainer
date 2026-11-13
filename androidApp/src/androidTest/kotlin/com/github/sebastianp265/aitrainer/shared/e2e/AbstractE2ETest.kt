package com.github.sebastianp265.aitrainer.shared.e2e

import android.app.LocaleManager
import android.content.Context
import android.os.LocaleList
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sebastianp265.aitrainer.feature.app.impl.MainActivity
import com.github.sebastianp265.aitrainer.integration.ai.AiClient
import com.github.sebastianp265.aitrainer.integration.health.HealthDataSource
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class AbstractE2ETest {

    @get:Rule
    val compose = createEmptyComposeRule()

    protected open val appLanguage = "en"

    protected val given = Given()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val localeManager = context.getSystemService(LocaleManager::class.java)
    private var scenario: ActivityScenario<MainActivity>? = null
    private var realHealthDataSource: HealthDataSource? = null
    private var realAiClient: AiClient? = null

    @BeforeTest
    fun startWithFreshData() {
        disableAnimations()
        localeManager.applicationLocales = LocaleList.forLanguageTags(appLanguage)
        deleteWorkouts()
        val koin = GlobalContext.get()
        runBlocking {
            koin.getAll<DataStore<Preferences>>().forEach { dataStore -> dataStore.edit { it.clear() } }
        }
    }

    @AfterTest
    fun closeApp() {
        scenario?.close()
        localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
        realHealthDataSource?.let { real -> loadKoinModules(module { single<HealthDataSource> { real } }) }
        realHealthDataSource = null
        realAiClient?.let { real -> loadKoinModules(module { factory<AiClient> { real } }) }
        realAiClient = null
    }

    protected fun launchApp(healthDataSource: HealthDataSource? = null, aiClient: AiClient? = null) {
        if (aiClient != null) {
            realAiClient = GlobalContext.get().get<AiClient>()
            loadKoinModules(module { factory<AiClient> { aiClient } })
        }
        if (healthDataSource != null) {
            realHealthDataSource = GlobalContext.get().get<HealthDataSource>()
            loadKoinModules(module { single<HealthDataSource> { healthDataSource } })
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    protected fun restartApp() {
        scenario?.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun disableAnimations() {
        ANIMATION_SCALE_SETTINGS.forEach { setting -> runShellCommand("settings put global $setting 0") }
    }

    private fun runShellCommand(command: String) {
        val output = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(output).use { it.readBytes() }
    }

    private fun deleteWorkouts() {
        if (!context.getDatabasePath(DATABASE_NAME).exists()) return
        context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null).use { database ->
            database.execSQL("DELETE FROM set_entry")
            database.execSQL("DELETE FROM workout_session")
            database.execSQL("DELETE FROM planned_set")
            database.execSQL("DELETE FROM plan")
            listOf("coach_message", "coach_proposal", "coach_note", "coach_profile", "ai_usage", "weekly_target").forEach { database.execSQL("DELETE FROM $it") }
        }
    }

    private companion object {
        const val DATABASE_NAME = "aitrainer.db"
        val ANIMATION_SCALE_SETTINGS = listOf("window_animation_scale", "transition_animation_scale", "animator_duration_scale")
    }
}
