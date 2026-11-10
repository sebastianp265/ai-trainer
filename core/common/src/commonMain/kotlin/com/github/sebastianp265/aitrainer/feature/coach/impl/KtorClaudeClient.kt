package com.github.sebastianp265.aitrainer.feature.coach.impl

import com.github.sebastianp265.aitrainer.integration.coach.ClaudeClient
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeContent
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeFailureKind
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeMessage
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeRequest
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeResponse
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeResult
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeRole
import com.github.sebastianp265.aitrainer.integration.settings.Secret
import com.github.sebastianp265.aitrainer.integration.settings.SecretStore
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

internal class KtorClaudeClient(
    private val http: HttpClient,
    private val secretStore: SecretStore,
) : ClaudeClient {

    private companion object {
        const val BASE_URL = "https://api.anthropic.com/v1"
        const val API_VERSION = "2023-06-01"
        const val HTTP_UNAUTHORIZED = 401
        const val HTTP_FORBIDDEN = 403
        const val HTTP_TOO_MANY_REQUESTS = 429
        const val HTTP_SERVER_ERROR = 500
        const val ERROR_BODY_LIMIT = 300
        val json = Json { ignoreUnknownKeys = true }
    }

    override suspend fun send(request: ClaudeRequest): ClaudeResult {
        val apiKey = secretStore.read(Secret.CLAUDE_API_KEY) ?: return ClaudeResult.Failure(ClaudeFailureKind.NO_KEY, "No API key")
        return try {
            val response = http.post("$BASE_URL/messages") {
                header("x-api-key", apiKey)
                header("anthropic-version", API_VERSION)
                contentType(ContentType.Application.Json)
                setBody(request.toBody().toString())
            }
            if (response.status.isSuccess()) ClaudeResult.Success(parseResponse(response.bodyAsText()))
            else failureOf(response)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ClaudeResult.Failure(ClaudeFailureKind.NO_NETWORK, e.message.orEmpty())
        }
    }

    override suspend fun checkApiKey(apiKey: String): ClaudeFailureKind? = try {
        val response = http.get("$BASE_URL/models?limit=1") {
            header("x-api-key", apiKey)
            header("anthropic-version", API_VERSION)
        }
        if (response.status.isSuccess()) null else failureOf(response).kind
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        ClaudeFailureKind.NO_NETWORK
    }

    private suspend fun failureOf(response: HttpResponse): ClaudeResult.Failure {
        val status = response.status.value
        val message = errorMessage(response.bodyAsText())
        val kind = when {
            status == HTTP_UNAUTHORIZED || status == HTTP_FORBIDDEN -> ClaudeFailureKind.KEY_REJECTED
            status == HTTP_TOO_MANY_REQUESTS || status >= HTTP_SERVER_ERROR -> ClaudeFailureKind.OVERLOADED
            else -> ClaudeFailureKind.OTHER
        }
        return ClaudeResult.Failure(kind, message)
    }

    private fun errorMessage(body: String): String = runCatching {
        json.parseToJsonElement(body).jsonObject["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
    }.getOrNull() ?: body.take(ERROR_BODY_LIMIT)

    private fun parseResponse(body: String): ClaudeResponse {
        val root = json.parseToJsonElement(body).jsonObject
        val content = root.getValue("content").jsonArray.mapNotNull { block ->
            val obj = block.jsonObject
            when (obj["type"]?.jsonPrimitive?.contentOrNull) {
                "text" -> ClaudeContent.Text(obj.getValue("text").jsonPrimitive.content)
                "tool_use" -> ClaudeContent.ToolUse(
                    id = obj.getValue("id").jsonPrimitive.content,
                    name = obj.getValue("name").jsonPrimitive.content,
                    inputJson = obj["input"]?.toString() ?: "{}",
                )
                else -> ClaudeContent.Opaque(obj.toString())
            }
        }
        val usage = root["usage"]?.jsonObject
        return ClaudeResponse(
            content = content,
            stopReason = root["stop_reason"]?.jsonPrimitive?.contentOrNull,
            inputTokens = usage?.get("input_tokens")?.jsonPrimitive?.intOrNull ?: 0,
            outputTokens = usage?.get("output_tokens")?.jsonPrimitive?.intOrNull ?: 0,
        )
    }

    private fun ClaudeRequest.toBody(): JsonObject = buildJsonObject {
        put("model", model)
        put("max_tokens", maxTokens)
        put("system", system)
        putJsonObject("thinking") { put("type", "between_tools") }
        put("messages", buildJsonArray { messages.forEach { add(it.toJson()) } })
        if (tools.isNotEmpty()) {
            putJsonArray("tools") {
                tools.forEach { tool ->
                    add(
                        buildJsonObject {
                            put("name", tool.name)
                            put("description", tool.description)
                            put("input_schema", json.parseToJsonElement(tool.inputSchemaJson))
                        },
                    )
                }
            }
        }
    }

    private fun ClaudeMessage.toJson(): JsonObject = buildJsonObject {
        put("role", if (role == ClaudeRole.USER) "user" else "assistant")
        put("content", JsonArray(content.map { it.toJson() }))
    }

    private fun ClaudeContent.toJson(): JsonObject = when (this) {
        is ClaudeContent.Text -> buildJsonObject {
            put("type", "text")
            put("text", text)
        }
        is ClaudeContent.ToolUse -> buildJsonObject {
            put("type", "tool_use")
            put("id", id)
            put("name", name)
            put("input", json.parseToJsonElement(inputJson))
        }
        is ClaudeContent.ToolResult -> buildJsonObject {
            put("type", "tool_result")
            put("tool_use_id", toolUseId)
            put("content", content)
            if (isError) put("is_error", true)
        }
        is ClaudeContent.Opaque -> json.parseToJsonElement(rawJson).jsonObject
    }
}
