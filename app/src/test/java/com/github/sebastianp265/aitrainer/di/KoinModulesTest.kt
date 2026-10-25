package com.github.sebastianp265.aitrainer.di

import android.content.Context
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngine
import kotlin.test.Test
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.module
import org.koin.test.verify.verify

@OptIn(KoinExperimentalAPI::class)
class KoinModulesTest {

    @Test
    fun `all modules resolve`() {
        module { includes(appModules) }.verify(
            // Provided by Android or by Ktor internals, not by our modules.
            extraTypes = listOf(
                Context::class,
                HttpClientEngine::class,
                HttpClientConfig::class,
            ),
        )
    }
}
