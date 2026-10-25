package com.github.sebastianp265.aitrainer.di

import androidx.room.Room
import com.github.sebastianp265.aitrainer.BuildConfig
import com.github.sebastianp265.aitrainer.data.db.AppDatabase
import com.github.sebastianp265.aitrainer.data.repo.ExerciseRepository
import com.github.sebastianp265.aitrainer.data.repo.RoomExerciseRepository
import com.github.sebastianp265.aitrainer.data.repo.RoomWorkoutRepository
import com.github.sebastianp265.aitrainer.data.repo.WorkoutRepository
import com.github.sebastianp265.aitrainer.ui.log.LogWorkoutViewModel
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.ANDROID
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json
import kotlinx.datetime.TimeZone
import kotlinx.serialization.json.Json
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module
import kotlin.time.Clock

val coreModule = module {
    single<Clock> { Clock.System }
    // Captured once: a timezone change takes effect after an app restart.
    single { TimeZone.currentSystemDefault() }
    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }
}

val dataModule = module {
    single {
        Room.databaseBuilder(androidContext(), AppDatabase::class.java, AppDatabase.NAME).build()
    }
    single { get<AppDatabase>().exerciseDao() }
    single { get<AppDatabase>().workoutDao() }
    single { get<AppDatabase>().dailyEnergyDao() }
    single { get<AppDatabase>().bodyWeightDao() }
    single { get<AppDatabase>().profilePrefsDao() }
    single { get<AppDatabase>().planDao() }

    singleOf(::RoomExerciseRepository) bind ExerciseRepository::class
    singleOf(::RoomWorkoutRepository) bind WorkoutRepository::class
}

val networkModule = module {
    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(get<Json>()) }
            if (BuildConfig.DEBUG) {
                install(Logging) {
                    logger = Logger.ANDROID
                    // Never log bodies: they may contain OAuth tokens.
                    level = LogLevel.HEADERS
                    sanitizeHeader { it == "x-api-key" || it == HttpHeaders.Authorization }
                }
            }
        }
    }
}

val viewModelModule = module {
    viewModelOf(::LogWorkoutViewModel)
}

val appModules = listOf(coreModule, dataModule, networkModule, viewModelModule)
