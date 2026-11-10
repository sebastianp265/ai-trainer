package com.github.sebastianp265.aitrainer.shared.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpTimeout
import org.koin.dsl.module

private const val CONNECT_TIMEOUT_MS = 15_000L
private const val REQUEST_TIMEOUT_MS = 180_000L

internal val networkModule = module {
    single {
        HttpClient(OkHttp) {
            install(HttpTimeout) {
                connectTimeoutMillis = CONNECT_TIMEOUT_MS
                requestTimeoutMillis = REQUEST_TIMEOUT_MS
            }
        }
    }
}
