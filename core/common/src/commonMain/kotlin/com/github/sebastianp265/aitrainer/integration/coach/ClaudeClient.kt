package com.github.sebastianp265.aitrainer.integration.coach

interface ClaudeClient {
    suspend fun send(request: ClaudeRequest): ClaudeResult

    suspend fun checkApiKey(apiKey: String): ClaudeFailureKind?
}

enum class ClaudeRole { USER, ASSISTANT }

sealed interface ClaudeContent {
    data class Text(val text: String) : ClaudeContent

    data class ToolUse(val id: String, val name: String, val inputJson: String) : ClaudeContent

    data class ToolResult(val toolUseId: String, val content: String, val isError: Boolean) : ClaudeContent

    data class Opaque(val rawJson: String) : ClaudeContent
}

data class ClaudeMessage(val role: ClaudeRole, val content: List<ClaudeContent>)

data class ClaudeTool(val name: String, val description: String, val inputSchemaJson: String)

data class ClaudeRequest(
    val model: String,
    val system: String,
    val messages: List<ClaudeMessage>,
    val tools: List<ClaudeTool>,
    val maxTokens: Int,
)

data class ClaudeResponse(
    val content: List<ClaudeContent>,
    val stopReason: String?,
    val inputTokens: Int,
    val outputTokens: Int,
)

enum class ClaudeFailureKind { NO_KEY, NO_NETWORK, KEY_REJECTED, OVERLOADED, OTHER }

sealed interface ClaudeResult {
    data class Success(val response: ClaudeResponse) : ClaudeResult

    data class Failure(val kind: ClaudeFailureKind, val message: String) : ClaudeResult
}
