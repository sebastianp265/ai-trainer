package com.github.sebastianp265.aitrainer.feature.settings.impl

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.github.sebastianp265.aitrainer.integration.ai.AiCacheDuration
import com.github.sebastianp265.aitrainer.integration.ai.AiEffort
import com.github.sebastianp265.aitrainer.integration.ai.AiThinking
import com.github.sebastianp265.aitrainer.integration.ai.AiTuning
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
    fun `targets are shown and the coach is on until the owner switches it off`() = runBlocking {
        assertEquals(true, service.observeShowTargets().first())
        assertEquals(true, service.observeCoachEnabled().first())
    }

    @Test
    fun `the show targets switch is remembered`() = runBlocking {
        service.setShowTargets(false)

        assertEquals(false, service.observeShowTargets().first())
    }

    @Test
    fun `the coach switch is remembered`() = runBlocking {
        service.setCoachEnabled(false)

        assertEquals(false, service.observeCoachEnabled().first())
    }

    @Test
    fun `the time the key was last checked is remembered and can be cleared`() = runBlocking {
        val checkedAt = kotlin.time.Instant.parse("2026-11-10T08:10:00Z")
        service.setApiKeyCheckedAt(checkedAt)
        assertEquals(checkedAt, service.observeApiKeyCheckedAt().first())

        service.setApiKeyCheckedAt(null)

        assertEquals(null, service.observeApiKeyCheckedAt().first())
    }

    @Test
    fun `the model tuning starts at the defaults and is remembered`() = runBlocking {
        assertEquals(AiTuning(), service.observeClaudeTuning().first())
        val tuning = AiTuning(effort = AiEffort.MAX, thinking = AiThinking.ADAPTIVE, maxTokens = 8_000, cacheDuration = AiCacheDuration.FIVE_MINUTES)

        service.setClaudeTuning(tuning)

        assertEquals(tuning, service.observeClaudeTuning().first())
    }

    @Test
    fun `back at the default effort no effort is stored`() = runBlocking {
        service.setClaudeTuning(AiTuning(effort = AiEffort.LOW))

        service.setClaudeTuning(AiTuning())

        assertEquals(AiTuning(), service.observeClaudeTuning().first())
    }

    @Test
    fun `the model falls back to the default when blank`() = runBlocking {
        service.setClaudeModel("   ")

        assertEquals("claude-sonnet-5-5", service.observeClaudeModel().first())
    }
}
