package com.github.sebastianp265.aitrainer.integration.ai

import kotlinx.coroutines.flow.Flow
import kotlin.time.Duration
import kotlin.time.Instant

interface AiService {
    fun observeModel(): Flow<String>

    suspend fun setModel(modelId: String)

    fun observeTuning(): Flow<AiTuning>

    suspend fun setTuning(tuning: AiTuning)

    fun observeIsApiKeyStored(): Flow<Boolean>

    fun observeApiKeyCheckedAt(): Flow<Instant?>

    suspend fun checkAndSaveApiKey(apiKey: String): AiFailureKind?

    suspend fun removeApiKey()

    fun observeUsageThisMonth(): Flow<AiUsage>

    fun observeLastRun(): Flow<AiRun?>

    companion object {
        const val DEFAULT_MODEL = "claude-sonnet-5-5"
        val MODELS = listOf("claude-opus-5-5", "claude-sonnet-5-5", "claude-haiku-5-5")
    }
}

data class AiUsage(val messages: Int, val inputTokens: Long, val outputTokens: Long)

data class AiRun(
    val rounds: Int,
    val inputTokens: Int,
    val cacheReadTokens: Int,
    val cacheWriteTokens: Int,
    val outputTokens: Int,
    val duration: Duration,
    val stopReason: String?,
)
