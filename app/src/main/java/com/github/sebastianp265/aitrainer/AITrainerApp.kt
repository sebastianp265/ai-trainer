package com.github.sebastianp265.aitrainer

import android.app.Application
import com.github.sebastianp265.aitrainer.data.repo.ExerciseRepository
import com.github.sebastianp265.aitrainer.di.appModules
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AITrainerApp : Application() {

    private val appScope = MainScope()
    private val exerciseRepository: ExerciseRepository by inject()

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.ERROR)
            androidContext(this@AITrainerApp)
            modules(appModules)
        }
        // Room runs suspend queries off the main thread itself.
        appScope.launch { exerciseRepository.ensureSeeded() }
    }
}
