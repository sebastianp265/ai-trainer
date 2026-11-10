package com.github.sebastianp265.aitrainer.shared.e2e

import com.github.sebastianp265.aitrainer.integration.coach.ClaudeClient
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeContent
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeFailureKind
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeRequest
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeResponse
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeResult

class FakeClaudeClient : ClaudeClient {

    val requests = mutableListOf<ClaudeRequest>()
    var keyCheckResult: ClaudeFailureKind? = null
    private val script = ArrayDeque<ClaudeResult>()

    fun answerWith(text: String) {
        script.addLast(ClaudeResult.Success(ClaudeResponse(listOf(ClaudeContent.Text(text)), "end_turn", inputTokens = 100, outputTokens = 20)))
    }

    fun answerWithToolCall(name: String, inputJson: String, id: String = "toolu_${script.size + 1}", text: String = "") {
        val content = listOfNotNull(text.takeIf { it.isNotEmpty() }?.let { ClaudeContent.Text(it) }, ClaudeContent.ToolUse(id, name, inputJson))
        script.addLast(ClaudeResult.Success(ClaudeResponse(content, "tool_use", inputTokens = 100, outputTokens = 20)))
    }

    fun failWith(kind: ClaudeFailureKind, message: String = "failure") {
        script.addLast(ClaudeResult.Failure(kind, message))
    }

    override suspend fun send(request: ClaudeRequest): ClaudeResult {
        requests += request
        return script.removeFirstOrNull() ?: ClaudeResult.Success(ClaudeResponse(listOf(ClaudeContent.Text("OK")), "end_turn", 1, 1))
    }

    override suspend fun checkApiKey(apiKey: String): ClaudeFailureKind? = keyCheckResult
}
