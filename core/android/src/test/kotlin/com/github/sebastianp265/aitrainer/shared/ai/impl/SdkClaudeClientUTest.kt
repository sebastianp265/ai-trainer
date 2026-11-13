package com.github.sebastianp265.aitrainer.shared.ai.impl

import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.github.sebastianp265.aitrainer.integration.ai.AiCacheDuration
import com.github.sebastianp265.aitrainer.integration.ai.AiContent
import com.github.sebastianp265.aitrainer.integration.ai.AiEffort
import com.github.sebastianp265.aitrainer.integration.ai.AiFailureKind
import com.github.sebastianp265.aitrainer.integration.ai.AiMessage
import com.github.sebastianp265.aitrainer.integration.ai.AiRequest
import com.github.sebastianp265.aitrainer.integration.ai.AiResult
import com.github.sebastianp265.aitrainer.integration.ai.AiRole
import com.github.sebastianp265.aitrainer.integration.ai.AiThinking
import com.github.sebastianp265.aitrainer.integration.ai.AiTool
import com.github.sebastianp265.aitrainer.integration.ai.AiTuning
import com.github.sebastianp265.aitrainer.integration.settings.Secret
import com.github.sebastianp265.aitrainer.integration.settings.SecretStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SdkClaudeClientUTest {

    private companion object {
        const val TEXT_ANSWER = """{"id":"msg_1","type":"message","role":"assistant","model":"claude-sonnet-5-5","content":[{"type":"text","text":"Hello"}],"stop_reason":"end_turn","usage":{"input_tokens":10,"output_tokens":5}}"""
        const val TOOL_ANSWER = """{"id":"msg_2","type":"message","role":"assistant","model":"claude-sonnet-5-5","content":[{"type":"text","text":"Let me look."},{"type":"tool_use","id":"toolu_1","name":"get_plans","input":{"limit":3}}],"stop_reason":"tool_use","usage":{"input_tokens":120,"output_tokens":45}}"""
        const val THINKING_ANSWER = """{"id":"msg_3","type":"message","role":"assistant","model":"claude-sonnet-5-5","content":[{"type":"thinking","thinking":"Checking the plans first.","signature":"sig-1"},{"type":"tool_use","id":"toolu_1","name":"get_plans","input":{}}],"stop_reason":"tool_use","usage":{"input_tokens":1,"output_tokens":1}}"""
        const val CACHED_ANSWER = """{"id":"msg_4","type":"message","role":"assistant","model":"claude-sonnet-5-5","content":[{"type":"text","text":"Hello"}],"stop_reason":"end_turn","usage":{"input_tokens":10,"cache_creation_input_tokens":300,"cache_read_input_tokens":2000,"output_tokens":5}}"""
        const val ERROR_BODY = """{"type":"error","error":{"type":"authentication_error","message":"invalid x-api-key"}}"""
        const val MODELS_ANSWER = """{"data":[],"has_more":false,"first_id":null,"last_id":null}"""
    }

    private val server = MockWebServer().apply { start() }
    private var storedKey: String? = "test-key"
    private val secretStore = object : SecretStore {
        override suspend fun write(secret: Secret, value: String) = Unit
        override suspend fun read(secret: Secret): String? = storedKey
        override suspend fun delete(secret: Secret) = Unit
        override fun observeIsStored(secret: Secret): Flow<Boolean> = flowOf(storedKey != null)
    }
    private val client = SdkClaudeClient(
        AnthropicOkHttpClient.builder().baseUrl(server.url("").toString().trimEnd('/')).maxRetries(0).build(),
        secretStore,
    )

    private val request = AiRequest(
        model = "claude-sonnet-5-5",
        system = "You are a coach.",
        messages = listOf(AiMessage(AiRole.USER, listOf(AiContent.Text("Plan my week")))),
        tools = listOf(AiTool("get_plans", "Lists plans", """{"type":"object","properties":{}}""")),
        tuning = AiTuning(maxTokens = 2048),
    )

    @After
    fun stop() = server.shutdown()

    private fun answer(body: String, status: Int = 200) {
        server.enqueue(MockResponse().setResponseCode(status).setHeader("Content-Type", "application/json").setBody(body))
    }

    private suspend fun send(sent: AiRequest = request, body: String = TEXT_ANSWER): Pair<AiResult, RecordedRequest> {
        answer(body)
        val result = client.send(sent)
        return result to server.takeRequest()
    }

    private fun RecordedRequest.json(): JsonObject = Json.parseToJsonElement(body.readUtf8()).jsonObject

    private suspend fun thinkingSentFor(sent: AiRequest): String? =
        send(sent).second.json()["thinking"]?.jsonObject?.getValue("type")?.jsonPrimitive?.content

    @Test
    fun `the request carries the key, the model, the system text, the messages and the tools`() = runBlocking {
        val recorded = send().second

        assertEquals("test-key", recorded.getHeader("x-api-key"))
        val body = recorded.json()
        assertEquals("claude-sonnet-5-5", body.getValue("model").jsonPrimitive.content)
        assertEquals("You are a coach.", body.getValue("system").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content)
        assertEquals("get_plans", body.getValue("tools").jsonArray.single().jsonObject.getValue("name").jsonPrimitive.content)
        assertEquals("Plan my week", body.getValue("messages").jsonArray.single().jsonObject.getValue("content").jsonArray.single().jsonObject.getValue("text").jsonPrimitive.content)
    }

    @Test
    fun `no sampling parameters and no forced tool are sent`() = runBlocking {
        val body = send().second.json()

        assertTrue("temperature" !in body && "top_p" !in body && "tool_choice" !in body)
    }

    @Test
    fun `the tools and the system text can stay cached for an hour while the growing conversation stays for minutes`() = runBlocking {
        val withTwoTools = request.copy(
            tools = request.tools + AiTool("get_workouts", "Lists workouts", """{"type":"object","properties":{}}"""),
            tuning = AiTuning(cacheDuration = AiCacheDuration.ONE_HOUR),
        )

        val body = send(withTwoTools).second.json()

        val tools = body.getValue("tools").jsonArray.map { it.jsonObject }
        val system = body.getValue("system").jsonArray.single().jsonObject
        assertEquals(listOf(null, "1h"), tools.map { it["cache_control"]?.jsonObject?.get("ttl")?.jsonPrimitive?.content })
        assertEquals("1h", system.getValue("cache_control").jsonObject.getValue("ttl").jsonPrimitive.content)
        assertEquals(buildJsonObject { put("type", "ephemeral") }, body.getValue("cache_control"))
    }

    @Test
    fun `the prompt is cached for minutes unless an hour is chosen`() = runBlocking {
        val body = send().second.json()

        assertEquals(buildJsonObject { put("type", "ephemeral") }, body.getValue("system").jsonArray.single().jsonObject.getValue("cache_control"))
    }

    @Test
    fun `uncached, cached and newly cached input tokens are read apart`() = runBlocking {
        val response = (send(body = CACHED_ANSWER).first as AiResult.Success).response

        assertEquals(listOf(10, 2000, 300, 5), listOf(response.inputTokens, response.cacheReadTokens, response.cacheWriteTokens, response.outputTokens))
    }

    @Test
    fun `turned off thinking is sent the way each model takes it`() = runBlocking {
        assertEquals(
            mapOf("claude-sonnet-5-5" to "between_tools", "claude-haiku-5-5" to "disabled", "claude-opus-5-5" to null),
            listOf("claude-sonnet-5-5", "claude-haiku-5-5", "claude-opus-5-5").associateWith { thinkingSentFor(request.copy(model = it)) },
        )
    }

    @Test
    fun `thinking stays on when asked for or above high effort`() = runBlocking {
        assertNull(thinkingSentFor(request.copy(tuning = AiTuning(thinking = AiThinking.ADAPTIVE))))
        assertNull(thinkingSentFor(request.copy(tuning = AiTuning(effort = AiEffort.XHIGH))))
        assertEquals("between_tools", thinkingSentFor(request.copy(tuning = AiTuning(effort = AiEffort.HIGH))))
    }

    @Test
    fun `the effort is sent only when chosen`() = runBlocking {
        assertTrue("output_config" !in send().second.json())

        val body = send(request.copy(tuning = AiTuning(effort = AiEffort.LOW, maxTokens = 4_000))).second.json()

        assertEquals("low", body.getValue("output_config").jsonObject.getValue("effort").jsonPrimitive.content)
        assertEquals(4_000, body.getValue("max_tokens").jsonPrimitive.content.toInt())
    }

    @Test
    fun `the highest answer limit is sent as typed`() = runBlocking {
        val body = send(request.copy(tuning = AiTuning(maxTokens = AiTuning.HIGHEST_MAX_TOKENS))).second.json()

        assertEquals(AiTuning.HIGHEST_MAX_TOKENS, body.getValue("max_tokens").jsonPrimitive.content.toInt())
    }

    @Test
    fun `text and tool calls of the answer are read with the token counts`() = runBlocking {
        val response = (send(body = TOOL_ANSWER).first as AiResult.Success).response

        assertEquals(listOf(AiContent.Text("Let me look."), AiContent.ToolUse("toolu_1", "get_plans", """{"limit":3}""")), response.content)
        assertEquals("tool_use", response.stopReason)
        assertEquals(120 to 45, response.inputTokens to response.outputTokens)
    }

    @Test
    fun `tool results are sent back as a user message`() = runBlocking {
        val withResult = request.copy(
            messages = request.messages + listOf(
                AiMessage(AiRole.ASSISTANT, listOf(AiContent.ToolUse("toolu_1", "get_plans", "{}"))),
                AiMessage(AiRole.USER, listOf(AiContent.ToolResult("toolu_1", "no plans", isError = false))),
            ),
        )

        val last = send(withResult).second.json().getValue("messages").jsonArray.last().jsonObject

        assertEquals("user", last.getValue("role").jsonPrimitive.content)
        assertEquals("tool_result", last.getValue("content").jsonArray.single().jsonObject.getValue("type").jsonPrimitive.content)
    }

    @Test
    fun `a thinking block of the answer is sent back unchanged`() = runBlocking {
        val answered = (send(body = THINKING_ANSWER).first as AiResult.Success).response
        val next = request.copy(messages = request.messages + AiMessage(AiRole.ASSISTANT, answered.content))

        val sentBack = send(next).second.json().getValue("messages").jsonArray.last().jsonObject.getValue("content").jsonArray.first().jsonObject

        assertEquals(
            buildJsonObject {
                put("type", "thinking")
                put("thinking", "Checking the plans first.")
                put("signature", "sig-1")
            },
            sentBack,
        )
    }

    @Test
    fun `without a stored key nothing is sent`() = runBlocking {
        storedKey = null

        val result = client.send(request) as AiResult.Failure

        assertEquals(AiFailureKind.NO_KEY, result.kind)
        assertEquals(0, server.requestCount)
    }

    @Test
    fun `a rejected key, an overloaded service and a lost connection are told apart`() = runBlocking {
        answer(ERROR_BODY, 401)
        val rejected = client.send(request) as AiResult.Failure
        answer(ERROR_BODY, 529)
        val overloaded = client.send(request) as AiResult.Failure
        answer(ERROR_BODY, 429)
        val rateLimited = client.send(request) as AiResult.Failure
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val offline = client.send(request) as AiResult.Failure

        assertEquals(AiFailureKind.KEY_REJECTED, rejected.kind)
        assertEquals(AiFailureKind.OVERLOADED, overloaded.kind)
        assertEquals(AiFailureKind.OVERLOADED, rateLimited.kind)
        assertEquals(AiFailureKind.NO_NETWORK, offline.kind)
        assertEquals("invalid x-api-key", rejected.message)
    }

    @Test
    fun `the key check sends the typed key and reports a lost connection`() = runBlocking {
        answer(MODELS_ANSWER)
        assertNull(client.checkApiKey("good-key"))
        assertEquals("good-key", server.takeRequest().getHeader("x-api-key"))

        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        assertEquals(AiFailureKind.NO_NETWORK, client.checkApiKey("any-key"))
    }
}
