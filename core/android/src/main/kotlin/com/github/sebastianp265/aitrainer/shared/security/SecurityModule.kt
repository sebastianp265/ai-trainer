package com.github.sebastianp265.aitrainer.shared.security

import com.github.sebastianp265.aitrainer.integration.settings.SecretStore
import com.github.sebastianp265.aitrainer.integration.shared.PreferencesStores
import com.github.sebastianp265.aitrainer.shared.security.impl.KeystoreAesGcmCipher
import com.github.sebastianp265.aitrainer.shared.security.impl.KeystoreSecretStore
import org.koin.core.qualifier.named
import org.koin.dsl.module

private const val SECRETS_KEYSTORE_ALIAS = "aitrainer_secrets"
private const val SECRETS_DATA_STORE_FILE = "secrets"

internal val securityModule = module {
    single(named(SECRETS_DATA_STORE_FILE)) { get<PreferencesStores>().create(SECRETS_DATA_STORE_FILE) }
    single { KeystoreAesGcmCipher(SECRETS_KEYSTORE_ALIAS) }
    single<SecretStore> { KeystoreSecretStore(get(named(SECRETS_DATA_STORE_FILE)), get()) }
}

