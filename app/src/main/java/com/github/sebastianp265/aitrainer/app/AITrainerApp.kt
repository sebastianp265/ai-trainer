package com.github.sebastianp265.aitrainer.app

import android.app.Application
import com.github.sebastianp265.aitrainer.BuildConfig
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

class AITrainerApp : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.INFO else Level.ERROR)
            androidContext(this@AITrainerApp)
            workManagerFactory()
            modules(appModules)
        }
    }
}
