package com.github.sebastianp265.aitrainer.feature.ai.impl

import com.github.sebastianp265.aitrainer.integration.ai.AiCacheDuration
import com.github.sebastianp265.aitrainer.integration.ai.AiEffort
import com.github.sebastianp265.aitrainer.integration.ai.AiFailureKind
import com.github.sebastianp265.aitrainer.integration.ai.AiThinking
import com.github.sebastianp265.aitrainer.integration.ai.AiTuning
import com.github.sebastianp265.aitrainer.integration.settings.Secret
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant

class DefaultAiServiceUTest {

    private val f = AiFixture()

    @AfterTest
    fun close() = f.database.close()

    @Test
    fun `a key that works is saved and the time of the check is remembered`() = runTest {
        val failure = f.ai.checkAndSaveApiKey("  test-key ")

        assertEquals(null, failure)
        assertEquals(listOf("test-key"), f.client.checkedKeys)
        assertEquals("test-key", f.secrets.read(Secret.CLAUDE_API_KEY))
        assertEquals(true, f.ai.observeIsApiKeyStored().first())
        assertEquals(Instant.parse("2026-11-10T10:00:00Z"), f.ai.observeApiKeyCheckedAt().first())
    }

    @Test
    fun `a key that is rejected is not saved`() = runTest {
        f.client.keyCheckResult = AiFailureKind.KEY_REJECTED

        val failure = f.ai.checkAndSaveApiKey("bad-key")

        assertEquals(AiFailureKind.KEY_REJECTED, failure)
        assertEquals(null, f.secrets.read(Secret.CLAUDE_API_KEY))
        assertEquals(null, f.ai.observeApiKeyCheckedAt().first())
    }

    @Test
    fun `removing the key forgets it and the time it was checked`() = runTest {
        f.ai.checkAndSaveApiKey("test-key")

        f.ai.removeApiKey()

        assertEquals(false, f.ai.observeIsApiKeyStored().first())
        assertEquals(null, f.ai.observeApiKeyCheckedAt().first())
    }

    @Test
    fun `the model tuning starts at the defaults and is remembered`() = runTest {
        assertEquals(AiTuning(), f.ai.observeTuning().first())
        val tuning = AiTuning(effort = AiEffort.MAX, thinking = AiThinking.ADAPTIVE, maxTokens = 8_000, cacheDuration = AiCacheDuration.FIVE_MINUTES)

        f.ai.setTuning(tuning)

        assertEquals(tuning, f.ai.observeTuning().first())
    }

    @Test
    fun `back at the default effort no effort is stored`() = runTest {
        f.ai.setTuning(AiTuning(effort = AiEffort.LOW))

        f.ai.setTuning(AiTuning())

        assertEquals(AiTuning(), f.ai.observeTuning().first())
    }

    @Test
    fun `the model falls back to the default when blank`() = runTest {
        f.ai.setModel("   ")

        assertEquals("claude-sonnet-5-5", f.ai.observeModel().first())
    }
}
