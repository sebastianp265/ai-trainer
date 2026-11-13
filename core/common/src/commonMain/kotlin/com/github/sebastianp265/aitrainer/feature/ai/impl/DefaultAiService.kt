package com.github.sebastianp265.aitrainer.feature.ai.impl

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.github.sebastianp265.aitrainer.integration.ai.AiCacheDuration
import com.github.sebastianp265.aitrainer.integration.ai.AiClient
import com.github.sebastianp265.aitrainer.integration.ai.AiEffort
import com.github.sebastianp265.aitrainer.integration.ai.AiFailureKind
import com.github.sebastianp265.aitrainer.integration.ai.AiRun
import com.github.sebastianp265.aitrainer.integration.ai.AiService
import com.github.sebastianp265.aitrainer.integration.ai.AiThinking
import com.github.sebastianp265.aitrainer.integration.ai.AiTuning
import com.github.sebastianp265.aitrainer.integration.ai.AiUsage
import com.github.sebastianp265.aitrainer.integration.settings.Secret
import com.github.sebastianp265.aitrainer.integration.settings.SecretStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock
import kotlin.time.Instant

internal class DefaultAiService(
    private val settings: DataStore<Preferences>,
    private val secretStore: SecretStore,
    private val client: () -> AiClient,
    private val usage: AiUsageLog,
    private val clock: Clock,
) : AiService {

    private val modelKey = stringPreferencesKey("claude_model")
    private val apiKeyCheckedAtKey = longPreferencesKey("api_key_checked_at")
    private val effortKey = stringPreferencesKey("claude_effort")
    private val thinkingKey = stringPreferencesKey("claude_thinking")
    private val maxTokensKey = intPreferencesKey("claude_max_tokens")
    private val cacheDurationKey = stringPreferencesKey("claude_cache_duration")

    override fun observeModel(): Flow<String> =
        settings.data.map { it[modelKey]?.ifBlank { null } ?: AiService.DEFAULT_MODEL }

    override suspend fun setModel(modelId: String) {
        settings.edit { it[modelKey] = modelId.trim() }
    }

    override fun observeTuning(): Flow<AiTuning> = settings.data.map { preferences ->
        val defaults = AiTuning()
        AiTuning(
            effort = preferences[effortKey]?.let(AiEffort::valueOf),
            thinking = preferences[thinkingKey]?.let(AiThinking::valueOf) ?: defaults.thinking,
            maxTokens = preferences[maxTokensKey] ?: defaults.maxTokens,
            cacheDuration = preferences[cacheDurationKey]?.let(AiCacheDuration::valueOf) ?: defaults.cacheDuration,
        )
    }

    override suspend fun setTuning(tuning: AiTuning) {
        settings.edit { preferences ->
            val effort = tuning.effort
            if (effort == null) preferences.remove(effortKey) else preferences[effortKey] = effort.name
            preferences[thinkingKey] = tuning.thinking.name
            preferences[maxTokensKey] = tuning.maxTokens
            preferences[cacheDurationKey] = tuning.cacheDuration.name
        }
    }

    override fun observeIsApiKeyStored(): Flow<Boolean> = secretStore.observeIsStored(Secret.CLAUDE_API_KEY)

    override fun observeApiKeyCheckedAt(): Flow<Instant?> =
        settings.data.map { preferences -> preferences[apiKeyCheckedAtKey]?.let(Instant::fromEpochMilliseconds) }

    override suspend fun checkAndSaveApiKey(apiKey: String): AiFailureKind? {
        val key = apiKey.trim()
        val failure = client().checkApiKey(key)
        if (failure == null) {
            secretStore.write(Secret.CLAUDE_API_KEY, key)
            settings.edit { it[apiKeyCheckedAtKey] = clock.now().toEpochMilliseconds() }
        }
        return failure
    }

    override suspend fun removeApiKey() {
        secretStore.delete(Secret.CLAUDE_API_KEY)
        settings.edit { it.remove(apiKeyCheckedAtKey) }
    }

    override fun observeUsageThisMonth(): Flow<AiUsage> = usage.observeUsageThisMonth()

    override fun observeLastRun(): Flow<AiRun?> = usage.observeLastRun()
}
