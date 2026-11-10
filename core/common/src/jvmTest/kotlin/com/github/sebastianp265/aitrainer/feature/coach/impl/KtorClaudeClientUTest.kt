package com.github.sebastianp265.aitrainer.feature.coach.impl

import com.github.sebastianp265.aitrainer.integration.coach.ClaudeContent
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeFailureKind
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeMessage
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeRequest
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeResult
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeRole
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeTool
import com.github.sebastianp265.aitrainer.integration.settings.Secret
import com.github.sebastianp265.aitrainer.integration.settings.SecretStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class KtorClaudeClientUTest {

    private var storedKey: String? = "test-key"
    private val secretStore = object : SecretStore {
        override suspend fun write(secret: Secret, value: String) = Unit
        override suspend fun read(secret: Secret): String? = storedKey
        override suspend fun delete(secret: Secret) = Unit
        override fun observeIsStored(secret: Secret): Flow<Boolean> = flowOf(storedKey != null)
    }
    private var captured: HttpRequestData? = null
    private var capturedBody: String = ""

    private fun client(status: HttpStatusCode = HttpStatusCode.OK, body: String = TEXT_ANSWER, failWith: Exception? = null): KtorClaudeClient {
        val engine = MockEngine { request ->
            captured = request
            capturedBody = request.body.toByteArray().decodeToString()
            if (failWith != null) throw failWith
            respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
        }
        return KtorClaudeClient(HttpClient(engine), secretStore)
    }

    private val request = ClaudeRequest(
        model = "claude-sonnet-5-5",
        system = "You are a coach.",
        messages = listOf(ClaudeMessage(ClaudeRole.USER, listOf(ClaudeContent.Text("Plan my week")))),
        tools = listOf(ClaudeTool("get_plans", "Lists plans", """{"type":"object","properties":{}}""")),
        maxTokens = 2048,
    )

    @Test
    fun `the request carries the key, the model, the system text, the messages and the tools`() = runBlocking {
        client().send(request)

        assertEquals("test-key", captured!!.headers["x-api-key"])
        assertEquals("2023-06-01", captured!!.headers["anthropic-version"])
        val body = Json.parseToJsonElement(capturedBody).jsonObject
        assertEquals("claude-sonnet-5-5", body.getValue("model").jsonPrimitive.content)
        assertEquals("You are a coach.", body.getValue("system").jsonPrimitive.content)
        assertEquals("get_plans", body.getValue("tools").jsonArray.single().jsonObject.getValue("name").jsonPrimitive.content)
        assertEquals("Plan my week", body.getValue("messages").jsonArray.single().jsonObject.getValue("content").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content)
    }

    @Test
    fun `no sampling parameters and no forced tool are sent`() = runBlocking {
        client().send(request)

        val body = Json.parseToJsonElement(capturedBody).jsonObject
        assertTrue("temperature" !in body && "top_p" !in body && "tool_choice" !in body)
    }

    @Test
    fun `text and tool calls of the answer are read with the token counts`() = runBlocking {
        val result = client(body = TOOL_ANSWER).send(request) as ClaudeResult.Success

        assertEquals(
            listOf(ClaudeContent.Text("Let me look."), ClaudeContent.ToolUse("toolu_1", "get_plans", """{"limit":3}""")),
            result.response.content,
        )
        assertEquals("tool_use", result.response.stopReason)
        assertEquals(120 to 45, result.response.inputTokens to result.response.outputTokens)
    }

    @Test
    fun `tool results are sent back as a user message`() = runBlocking {
        val withResult = request.copy(
            messages = request.messages + listOf(
                ClaudeMessage(ClaudeRole.ASSISTANT, listOf(ClaudeContent.ToolUse("toolu_1", "get_plans", "{}"))),
                ClaudeMessage(ClaudeRole.USER, listOf(ClaudeContent.ToolResult("toolu_1", "no plans", isError = false))),
            ),
        )

        client().send(withResult)

        val last = Json.parseToJsonElement(capturedBody).jsonObject.getValue("messages").jsonArray.last().jsonObject
        assertEquals("user", last.getValue("role").jsonPrimitive.content)
        assertEquals("tool_result", last.getValue("content").jsonArray.single().jsonObject.getValue("type").jsonPrimitive.content)
    }

    @Test
    fun `without a stored key nothing is sent`() = runBlocking {
        storedKey = null

        val result = client().send(request) as ClaudeResult.Failure

        assertEquals(ClaudeFailureKind.NO_KEY, result.kind)
        assertNull(captured)
    }

    @Test
    fun `a rejected key, an overloaded service and a lost connection are told apart`() = runBlocking {
        val rejected = client(HttpStatusCode.Unauthorized, ERROR_BODY).send(request) as ClaudeResult.Failure
        val overloaded = client(HttpStatusCode(529, "Overloaded"), ERROR_BODY).send(request) as ClaudeResult.Failure
        val rateLimited = client(HttpStatusCode.TooManyRequests, ERROR_BODY).send(request) as ClaudeResult.Failure
        val offline = client(failWith = IOException("no route")).send(request) as ClaudeResult.Failure

        assertEquals(ClaudeFailureKind.KEY_REJECTED, rejected.kind)
        assertEquals(ClaudeFailureKind.OVERLOADED, overloaded.kind)
        assertEquals(ClaudeFailureKind.OVERLOADED, rateLimited.kind)
        assertEquals(ClaudeFailureKind.NO_NETWORK, offline.kind)
        assertEquals("invalid x-api-key", rejected.message)
    }

    @Test
    fun `the key check accepts a good key and reports why a bad one fails`() = runBlocking {
        assertNull(client(body = """{"data":[]}""").checkApiKey("good-key"))
        assertEquals("good-key", captured!!.headers["x-api-key"])
        assertEquals(ClaudeFailureKind.KEY_REJECTED, client(HttpStatusCode.Unauthorized, ERROR_BODY).checkApiKey("bad-key"))
        assertEquals(ClaudeFailureKind.NO_NETWORK, client(failWith = IOException("offline")).checkApiKey("any-key"))
    }

    private companion object {
        const val TEXT_ANSWER = """{"content":[{"type":"text","text":"Hello"}],"stop_reason":"end_turn","usage":{"input_tokens":10,"output_tokens":5}}"""
        const val TOOL_ANSWER = """{"content":[{"type":"text","text":"Let me look."},{"type":"tool_use","id":"toolu_1","name":"get_plans","input":{"limit":3}}],"stop_reason":"tool_use","usage":{"input_tokens":120,"output_tokens":45}}"""
        const val ERROR_BODY = """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}"""
    }
}
