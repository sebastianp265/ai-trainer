package com.github.sebastianp265.aitrainer.feature.settings.impl

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.github.sebastianp265.aitrainer.integration.settings.Secret
import com.github.sebastianp265.aitrainer.integration.settings.SecretStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals

class DefaultSettingsServiceUTest {

    private val secretStore = object : SecretStore {
        override suspend fun write(secret: Secret, value: String) = Unit
        override suspend fun read(secret: Secret): String? = null
        override suspend fun delete(secret: Secret) = Unit
        override fun observeIsStored(secret: Secret): Flow<Boolean> = flowOf(false)
    }
    private val preferences = PreferenceDataStoreFactory.create(
        produceFile = { Files.createTempDirectory("settings").resolve("settings.preferences_pb").toFile() },
    )
    private val service = DefaultSettingsService(preferences, secretStore)

    @Test
    fun `targets are shown and the coach is off until the owner changes it`() = runBlocking {
        assertEquals(true, service.observeShowTargets().first())
        assertEquals(false, service.observeCoachEnabled().first())
    }

    @Test
    fun `the show targets switch is remembered`() = runBlocking {
        service.setShowTargets(false)

        assertEquals(false, service.observeShowTargets().first())
    }

    @Test
    fun `the coach switch is remembered`() = runBlocking {
        service.setCoachEnabled(true)

        assertEquals(true, service.observeCoachEnabled().first())
    }

    @Test
    fun `the model falls back to the default when blank`() = runBlocking {
        service.setClaudeModel("   ")

        assertEquals("claude-sonnet-5-5", service.observeClaudeModel().first())
    }
}
