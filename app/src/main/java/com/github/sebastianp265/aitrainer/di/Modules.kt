package com.github.sebastianp265.aitrainer.di

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.github.sebastianp265.aitrainer.BuildConfig
import com.github.sebastianp265.aitrainer.data.db.AppDatabase
import com.github.sebastianp265.aitrainer.data.db.ExerciseLibraryCallback
import com.github.sebastianp265.aitrainer.data.healthconnect.HealthConnectDataSource
import com.github.sebastianp265.aitrainer.data.healthconnect.HealthDataSource
import com.github.sebastianp265.aitrainer.data.repo.AppSettingsRepository
import com.github.sebastianp265.aitrainer.data.repo.DataStoreAppSettingsRepository
import com.github.sebastianp265.aitrainer.data.repo.ExerciseRepository
import com.github.sebastianp265.aitrainer.data.repo.ProfileRepository
import com.github.sebastianp265.aitrainer.data.repo.RoomExerciseRepository
import com.github.sebastianp265.aitrainer.data.repo.RoomProfileRepository
import com.github.sebastianp265.aitrainer.data.repo.RoomWorkoutRepository
import com.github.sebastianp265.aitrainer.data.repo.WorkoutRepository
import com.github.sebastianp265.aitrainer.data.secrets.KeystoreAesGcmCipher
import com.github.sebastianp265.aitrainer.data.secrets.KeystoreSecretStore
import com.github.sebastianp265.aitrainer.data.secrets.SecretStore
import com.github.sebastianp265.aitrainer.domain.usecase.AddExerciseToWorkoutUseCase
import com.github.sebastianp265.aitrainer.domain.usecase.AddSetUseCase
import com.github.sebastianp265.aitrainer.domain.usecase.ChangeSetUseCase
import com.github.sebastianp265.aitrainer.domain.usecase.CheckHealthConnectAccessUseCase
import com.github.sebastianp265.aitrainer.domain.usecase.CopySetsFromLastTimeUseCase
import com.github.sebastianp265.aitrainer.domain.usecase.ReadHealthConnectDaysUseCase
import com.github.sebastianp265.aitrainer.ui.log.LogWorkoutViewModel
import com.github.sebastianp265.aitrainer.ui.progress.ProgressViewModel
import com.github.sebastianp265.aitrainer.ui.settings.ExercisePreferencesViewModel
import com.github.sebastianp265.aitrainer.ui.settings.SettingsViewModel
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.factoryOf
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Clock

val coreModule = module {
    single<Clock> { Clock.System }
    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}

val dataModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, AppDatabase.NAME)
            .fallbackToDestructiveMigration(dropAllTables = true)
            .addCallback(ExerciseLibraryCallback())
            .build()
    }
    single { get<AppDatabase>().exerciseDao() }
    single { get<AppDatabase>().workoutDao() }
    single { get<AppDatabase>().profilePrefsDao() }

    singleOf(::RoomExerciseRepository) bind ExerciseRepository::class
    singleOf(::RoomWorkoutRepository) bind WorkoutRepository::class
    singleOf(::RoomProfileRepository) bind ProfileRepository::class
}

object DataStoreFiles {
    const val SECRETS = "secrets"
    const val SETTINGS = "settings"
}

private const val SECRETS_KEYSTORE_ALIAS = "aitrainer_secrets"

val storageModule = module {
    single(named(DataStoreFiles.SECRETS)) { preferencesDataStore(DataStoreFiles.SECRETS) }
    single(named(DataStoreFiles.SETTINGS)) { preferencesDataStore(DataStoreFiles.SETTINGS) }
    single { KeystoreAesGcmCipher(SECRETS_KEYSTORE_ALIAS) }
    single<SecretStore> { KeystoreSecretStore(get(named(DataStoreFiles.SECRETS)), get()) }
    single<AppSettingsRepository> { DataStoreAppSettingsRepository(get(named(DataStoreFiles.SETTINGS))) }
}

private fun Scope.preferencesDataStore(fileName: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.create { androidContext().preferencesDataStoreFile(fileName) }

val healthDataSourceModule = module {
    single<HealthDataSource> { HealthConnectDataSource(androidContext()) }
}

val useCaseModule = module {
    factoryOf(::CheckHealthConnectAccessUseCase)
    factoryOf(::ReadHealthConnectDaysUseCase)
    factoryOf(::AddExerciseToWorkoutUseCase)
    factoryOf(::AddSetUseCase)
    factoryOf(::CopySetsFromLastTimeUseCase)
    factoryOf(::ChangeSetUseCase)
}

val networkModule = module {
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(get<Json>()) }
            if (BuildConfig.DEBUG) installHeaderOnlyLogging()
        }
    }
}

private fun HttpClientConfig<*>.installHeaderOnlyLogging() {
    install(Logging) {
        logger = Logger.ANDROID
        level = LogLevel.HEADERS
        sanitizeHeader { it == "x-api-key" || it == HttpHeaders.Authorization }
    }
}

val viewModelModule = module {
    viewModelOf(::LogWorkoutViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::ExercisePreferencesViewModel)
    viewModelOf(::ProgressViewModel)
}

val appModules = listOf(
    coreModule,
    dataModule,
    storageModule,
    healthDataSourceModule,
    useCaseModule,
    networkModule,
    viewModelModule,
)
