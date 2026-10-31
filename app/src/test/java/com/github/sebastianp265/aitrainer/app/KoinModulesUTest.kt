package com.github.sebastianp265.aitrainer.app

import android.content.Context
import androidx.work.WorkerParameters
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify
import kotlin.test.Test

@OptIn(KoinExperimentalAPI::class)
class KoinModulesUTest {

    @Test
    fun `all modules resolve`() {
        val typesProvidedByAndroidKtorOrWorkManager =
            listOf(Context::class, HttpClientEngine::class, HttpClientConfig::class, WorkerParameters::class)
        module { includes(appModules) }.verify(extraTypes = typesProvidedByAndroidKtorOrWorkManager)
    }
}
