package com.github.sebastianp265.aitrainer.core

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.github.sebastianp265.aitrainer.BuildConfig
import com.github.sebastianp265.aitrainer.core.secrets.KeystoreAesGcmCipher
import com.github.sebastianp265.aitrainer.core.secrets.KeystoreSecretStore
import com.github.sebastianp265.aitrainer.core.secrets.SecretStore
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
import org.koin.core.qualifier.named
import org.koin.core.scope.Scope
import org.koin.dsl.module
import kotlin.time.Clock

private const val SECRETS_KEYSTORE_ALIAS = "aitrainer_secrets"

val coreModule = module {
    single<Clock> { Clock.System }
    single {
        Json {
            ignoreUnknownKeys = true
            explicitNulls = false
        }
    }

    single(named(DataStoreFiles.SECRETS)) { preferencesDataStore(DataStoreFiles.SECRETS) }
    single(named(DataStoreFiles.SETTINGS)) { preferencesDataStore(DataStoreFiles.SETTINGS) }
    single { KeystoreAesGcmCipher(SECRETS_KEYSTORE_ALIAS) }
    single<SecretStore> { KeystoreSecretStore(get(named(DataStoreFiles.SECRETS)), get()) }

    single {
        HttpClient(OkHttp) {
            install(ContentNegotiation) { json(get<Json>()) }
            if (BuildConfig.DEBUG) installHeaderOnlyLogging()
        }
    }
}

private fun Scope.preferencesDataStore(fileName: String): DataStore<Preferences> =
    PreferenceDataStoreFactory.create { androidContext().preferencesDataStoreFile(fileName) }

private fun HttpClientConfig<*>.installHeaderOnlyLogging() {
    install(Logging) {
        logger = Logger.ANDROID
        level = LogLevel.HEADERS
        sanitizeHeader { it == "x-api-key" || it == HttpHeaders.Authorization }
    }
}
