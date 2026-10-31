package com.github.sebastianp265.aitrainer.testing

import android.app.LocaleManager
import android.os.LocaleList
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sebastianp265.aitrainer.app.AppDatabase
import com.github.sebastianp265.aitrainer.app.MainActivity
import com.github.sebastianp265.aitrainer.app.databaseModule
import com.github.sebastianp265.aitrainer.app.featureModules
import com.github.sebastianp265.aitrainer.core.DataStoreFiles
import com.github.sebastianp265.aitrainer.core.secrets.Secret
import com.github.sebastianp265.aitrainer.core.secrets.SecretStore
import com.github.sebastianp265.aitrainer.feature.health.data.HealthDataSource
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest

abstract class AbstractITest {

    @get:Rule
    val compose = createEmptyComposeRule()

    protected open val appLanguage = "en"

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val localeManager = context.getSystemService(LocaleManager::class.java)
    private var scenario: ActivityScenario<MainActivity>? = null
    private val modulesUsingTheDatabaseOrIntegrations = listOf(databaseModule) + featureModules

    @BeforeTest
    fun startWithFreshData() {
        localeManager.applicationLocales = LocaleList.forLanguageTags(appLanguage)
        recreateDatabase()
        val koin = GlobalContext.get()
        runBlocking {
            koin.get<DataStore<Preferences>>(named(DataStoreFiles.SETTINGS)).edit { it.clear() }
            koin.get<SecretStore>().delete(Secret.CLAUDE_API_KEY)
        }
    }

    @AfterTest
    fun closeApp() {
        scenario?.close()
        localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
    }

    protected fun launchApp(healthDataSource: HealthDataSource? = null) {
        if (healthDataSource != null) {
            loadKoinModules(module { single<HealthDataSource> { healthDataSource } })
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    protected fun restartApp() {
        scenario?.close()
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }

    private fun recreateDatabase() {
        GlobalContext.get().get<AppDatabase>().close()
        context.deleteDatabase(AppDatabase.NAME)
        unloadKoinModules(modulesUsingTheDatabaseOrIntegrations)
        loadKoinModules(modulesUsingTheDatabaseOrIntegrations)
    }
}
