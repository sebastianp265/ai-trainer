package com.github.sebastianp265.aitrainer.integration.ai

interface AiClient {
    suspend fun send(request: AiRequest): AiResult

    suspend fun checkApiKey(apiKey: String): AiFailureKind?
}

enum class AiRole { USER, ASSISTANT }

sealed interface AiContent {
    data class Text(val text: String) : AiContent

    data class ToolUse(val id: String, val name: String, val inputJson: String) : AiContent

    data class ToolResult(val toolUseId: String, val content: String, val isError: Boolean) : AiContent

    data class Opaque(val rawJson: String) : AiContent
}

data class AiMessage(val role: AiRole, val content: List<AiContent>)

data class AiTool(val name: String, val description: String, val inputSchemaJson: String)

enum class AiEffort { LOW, MEDIUM, HIGH, XHIGH, MAX }

enum class AiThinking { ADAPTIVE, OFF }

enum class AiCacheDuration { FIVE_MINUTES, ONE_HOUR }

data class AiTuning(
    val effort: AiEffort? = null,
    val thinking: AiThinking = AiThinking.OFF,
    val maxTokens: Int = 16_000,
    val cacheDuration: AiCacheDuration = AiCacheDuration.FIVE_MINUTES,
) {
    companion object {
        const val HIGHEST_MAX_TOKENS = 32_000
        val MODELS_WITH_THINKING_OFF = listOf("claude-sonnet-5-5", "claude-haiku-5-5")
        val HIGHEST_EFFORT_WITH_THINKING_OFF = AiEffort.HIGH
    }
}

data class AiRequest(
    val model: String,
    val system: String,
    val messages: List<AiMessage>,
    val tools: List<AiTool>,
    val tuning: AiTuning,
)

data class AiResponse(
    val content: List<AiContent>,
    val stopReason: String?,
    val inputTokens: Int,
    val cacheReadTokens: Int,
    val cacheWriteTokens: Int,
    val outputTokens: Int,
)

enum class AiFailureKind { NO_KEY, NO_NETWORK, KEY_REJECTED, OVERLOADED, OTHER }

sealed interface AiResult {
    data class Success(val response: AiResponse) : AiResult

    data class Failure(val kind: AiFailureKind, val message: String) : AiResult
}
