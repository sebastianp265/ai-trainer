package com.github.sebastianp265.aitrainer.testing

import android.app.LocaleManager
import android.os.LocaleList
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sebastianp265.aitrainer.MainActivity
import com.github.sebastianp265.aitrainer.data.db.AppDatabase
import com.github.sebastianp265.aitrainer.data.healthconnect.HealthDataSource
import com.github.sebastianp265.aitrainer.data.repo.ExerciseRepository
import com.github.sebastianp265.aitrainer.data.secrets.Secret
import com.github.sebastianp265.aitrainer.data.secrets.SecretStore
import com.github.sebastianp265.aitrainer.di.DataStoreFiles
import com.github.sebastianp265.aitrainer.di.healthDataSourceModule
import kotlinx.coroutines.runBlocking
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.core.qualifier.named
import org.koin.dsl.module

class AppUnderTest {

    private val localeManager = InstrumentationRegistry.getInstrumentation().targetContext
        .getSystemService(LocaleManager::class.java)
    private var scenario: ActivityScenario<MainActivity>? = null
    private var replacedHealthDataSource = false

    fun startInEnglishWithFreshData(healthDataSource: HealthDataSource? = null) {
        localeManager.applicationLocales = LocaleList.forLanguageTags("en")
        if (healthDataSource != null) {
            loadKoinModules(module { single<HealthDataSource> { healthDataSource } })
            replacedHealthDataSource = true
        }
        val koin = GlobalContext.get()
        koin.get<AppDatabase>().clearAllTables()
        runBlocking {
            koin.get<ExerciseRepository>().ensureSeeded()
            koin.get<DataStore<Preferences>>(named(DataStoreFiles.SETTINGS)).edit { it.clear() }
            koin.get<SecretStore>().delete(Secret.CLAUDE_API_KEY)
        }
        launch()
    }

    fun restart() {
        scenario?.close()
        launch()
    }

    fun close() {
        scenario?.close()
        localeManager.applicationLocales = LocaleList.getEmptyLocaleList()
        if (replacedHealthDataSource) loadKoinModules(healthDataSourceModule)
    }

    private fun launch() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
    }
}
