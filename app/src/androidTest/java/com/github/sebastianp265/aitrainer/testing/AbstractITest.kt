package com.github.sebastianp265.aitrainer.testing

import android.app.LocaleManager
import android.os.LocaleList
import androidx.compose.ui.test.junit4.v2.createEmptyComposeRule
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sebastianp265.aitrainer.MainActivity
import com.github.sebastianp265.aitrainer.data.db.AppDatabase
import com.github.sebastianp265.aitrainer.data.healthconnect.HealthDataSource
import com.github.sebastianp265.aitrainer.data.secrets.Secret
import com.github.sebastianp265.aitrainer.data.secrets.SecretStore
import com.github.sebastianp265.aitrainer.di.DataStoreFiles
import com.github.sebastianp265.aitrainer.di.dataModule
import com.github.sebastianp265.aitrainer.di.healthDataSourceModule
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
    private var replacedHealthDataSource = false

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
        if (replacedHealthDataSource) loadKoinModules(healthDataSourceModule)
    }

    protected fun launchApp(healthDataSource: HealthDataSource? = null) {
        if (healthDataSource != null) {
            loadKoinModules(module { single<HealthDataSource> { healthDataSource } })
            replacedHealthDataSource = true
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
        unloadKoinModules(dataModule)
        loadKoinModules(dataModule)
    }
}
