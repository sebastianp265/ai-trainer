package com.github.sebastianp265.aitrainer.di

import android.content.Context
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import kotlin.test.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

@OptIn(KoinExperimentalAPI::class)
class KoinModulesUTest {

    @Test
    fun `all modules resolve`() {
        val typesProvidedByAndroidOrKtor = listOf(Context::class, HttpClientEngine::class, HttpClientConfig::class)
        module { includes(appModules) }.verify(extraTypes = typesProvidedByAndroidOrKtor)
    }
}
