package com.github.sebastianp265.aitrainer.data.secrets

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class KeystoreSecretStoreITest {

    private companion object {
        val encryptedValues = PreferenceDataStoreFactory.create {
            InstrumentationRegistry.getInstrumentation().targetContext.preferencesDataStoreFile("secrets-itest")
        }
        val rawClaudeApiKey = stringPreferencesKey("CLAUDE_API_KEY")
    }

    private val store = KeystoreSecretStore(encryptedValues, KeystoreAesGcmCipher("secrets_itest_key"))

    @BeforeTest
    fun startWithNoSecrets() = runBlocking {
        encryptedValues.edit { it.clear() }
        Unit
    }

    @Test
    fun writtenSecretIsReadBack() = runBlocking {
        store.write(Secret.CLAUDE_API_KEY, "test-key")

        assertEquals("test-key", store.read(Secret.CLAUDE_API_KEY))
        assertTrue(store.observeIsStored(Secret.CLAUDE_API_KEY).first())
    }

    @Test
    fun secretIsNotStoredAsPlaintext() = runBlocking {
        store.write(Secret.CLAUDE_API_KEY, "test-key")

        val stored = encryptedValues.data.first()[rawClaudeApiKey]!!
        assertFalse("test-key" in stored)
    }

    @Test
    fun writingTheSameSecretTwiceProducesDifferentCiphertexts() = runBlocking {
        store.write(Secret.CLAUDE_API_KEY, "test-key")
        val first = encryptedValues.data.first()[rawClaudeApiKey]
        store.write(Secret.CLAUDE_API_KEY, "test-key")
        val second = encryptedValues.data.first()[rawClaudeApiKey]

        assertNotEquals(first, second)
    }

    @Test
    fun deletedSecretIsGone() = runBlocking {
        store.write(Secret.CLAUDE_API_KEY, "test-key")

        store.delete(Secret.CLAUDE_API_KEY)

        assertNull(store.read(Secret.CLAUDE_API_KEY))
        assertFalse(store.observeIsStored(Secret.CLAUDE_API_KEY).first())
    }
}
