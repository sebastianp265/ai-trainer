package com.github.sebastianp265.aitrainer.shared.security.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.sebastianp265.aitrainer.integration.settings.Secret
import com.github.sebastianp265.aitrainer.integration.settings.SecretStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

internal class KeystoreSecretStore(
    private val encryptedValues: DataStore<Preferences>,
    private val cipher: KeystoreAesGcmCipher,
) : SecretStore {

    suspend fun read(secret: Secret): String? =
        encryptedValues.data.first()[secret.preferenceKey()]?.let(cipher::decrypt)

    override suspend fun write(secret: Secret, value: String) {
        val encrypted = cipher.encrypt(value)
        encryptedValues.edit { it[secret.preferenceKey()] = encrypted }
    }

    override suspend fun delete(secret: Secret) {
        encryptedValues.edit { it.remove(secret.preferenceKey()) }
    }

    override fun observeIsStored(secret: Secret): Flow<Boolean> =
        encryptedValues.data.map { secret.preferenceKey() in it }

    private fun Secret.preferenceKey() = stringPreferencesKey(name)
}
