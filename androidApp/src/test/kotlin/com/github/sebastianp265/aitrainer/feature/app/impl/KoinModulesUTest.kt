package com.github.sebastianp265.aitrainer.feature.app.impl

import android.content.Context
import androidx.work.WorkerParameters
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

@OptIn(KoinExperimentalAPI::class)
class KoinModulesUTest {

    @Test
    fun `all modules resolve`() {
        val typesProvidedByAndroidOrWorkManager = listOf(Context::class, WorkerParameters::class)
        module { includes(appModules) }.verify(extraTypes = typesProvidedByAndroidOrWorkManager)
    }
}
