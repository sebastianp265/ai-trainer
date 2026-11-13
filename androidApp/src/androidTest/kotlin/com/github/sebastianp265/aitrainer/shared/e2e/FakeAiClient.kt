package com.github.sebastianp265.aitrainer.shared.e2e

import com.github.sebastianp265.aitrainer.integration.coach.AiClient
import com.github.sebastianp265.aitrainer.integration.coach.AiContent
import com.github.sebastianp265.aitrainer.integration.coach.AiFailureKind
import com.github.sebastianp265.aitrainer.integration.coach.AiRequest
import com.github.sebastianp265.aitrainer.integration.coach.AiResponse
import com.github.sebastianp265.aitrainer.integration.coach.AiResult

class FakeAiClient : AiClient {

    val requests = mutableListOf<AiRequest>()
    var keyCheckResult: AiFailureKind? = null
    private val script = ArrayDeque<AiResult>()

    fun answerWith(text: String) {
        script.addLast(AiResult.Success(AiResponse(listOf(AiContent.Text(text)), "end_turn", inputTokens = 100, cacheReadTokens = 0, cacheWriteTokens = 0, outputTokens = 20)))
    }

    fun answerWithToolCall(name: String, inputJson: String, id: String = "toolu_${script.size + 1}", text: String = "") {
        val content = listOfNotNull(text.takeIf { it.isNotEmpty() }?.let { AiContent.Text(it) }, AiContent.ToolUse(id, name, inputJson))
        script.addLast(AiResult.Success(AiResponse(content, "tool_use", inputTokens = 100, cacheReadTokens = 0, cacheWriteTokens = 0, outputTokens = 20)))
    }

    fun failWith(kind: AiFailureKind, message: String = "failure") {
        script.addLast(AiResult.Failure(kind, message))
    }

    override suspend fun send(request: AiRequest): AiResult {
        requests += request
        return script.removeFirstOrNull() ?: AiResult.Success(AiResponse(listOf(AiContent.Text("OK")), "end_turn", inputTokens = 1, cacheReadTokens = 0, cacheWriteTokens = 0, outputTokens = 1))
    }

    fun assertFirstMessageMentions(text: String) {
        val first = requests.last().messages.first().content.first() as AiContent.Text
        kotlin.test.assertTrue(text in first.text, "\"$text\" not in \"${first.text}\"")
    }

    override suspend fun checkApiKey(apiKey: String): AiFailureKind? = keyCheckResult
}
