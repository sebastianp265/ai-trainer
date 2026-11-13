package com.github.sebastianp265.aitrainer.shared.ai.impl

import com.anthropic.client.AnthropicClient
import com.anthropic.core.JsonValue
import com.anthropic.core.jsonMapper
import com.anthropic.errors.AnthropicServiceException
import com.anthropic.errors.OverloadedException
import com.anthropic.errors.PermissionDeniedException
import com.anthropic.errors.RateLimitException
import com.anthropic.errors.UnauthorizedException
import com.anthropic.models.messages.CacheControlEphemeral
import com.anthropic.models.messages.ContentBlock
import com.anthropic.models.messages.ContentBlockParam
import com.anthropic.models.messages.Message
import com.anthropic.models.messages.MessageCreateParams
import com.anthropic.models.messages.MessageParam
import com.anthropic.models.messages.OutputConfig
import com.anthropic.models.messages.TextBlockParam
import com.anthropic.models.messages.Tool
import com.anthropic.models.messages.ToolResultBlockParam
import com.anthropic.models.messages.ToolUnion
import com.anthropic.models.messages.ToolUseBlockParam
import com.anthropic.models.models.ModelListParams
import com.github.sebastianp265.aitrainer.integration.ai.AiCacheDuration
import com.github.sebastianp265.aitrainer.integration.ai.AiClient
import com.github.sebastianp265.aitrainer.integration.ai.AiContent
import com.github.sebastianp265.aitrainer.integration.ai.AiEffort
import com.github.sebastianp265.aitrainer.integration.ai.AiFailureKind
import com.github.sebastianp265.aitrainer.integration.ai.AiMessage
import com.github.sebastianp265.aitrainer.integration.ai.AiRequest
import com.github.sebastianp265.aitrainer.integration.ai.AiResponse
import com.github.sebastianp265.aitrainer.integration.ai.AiResult
import com.github.sebastianp265.aitrainer.integration.ai.AiRole
import com.github.sebastianp265.aitrainer.integration.ai.AiThinking
import com.github.sebastianp265.aitrainer.integration.ai.AiTool
import com.github.sebastianp265.aitrainer.integration.ai.AiTuning
import com.github.sebastianp265.aitrainer.integration.settings.Secret
import com.github.sebastianp265.aitrainer.integration.settings.SecretStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

internal class SdkClaudeClient(
    private val client: AnthropicClient,
    private val secretStore: SecretStore,
) : AiClient {

    private companion object {
        const val SONNET = "claude-sonnet-5-5"
        val cachedForMinutes: CacheControlEphemeral = CacheControlEphemeral.builder().build()
        val cachedForAnHour: CacheControlEphemeral = CacheControlEphemeral.builder().ttl(CacheControlEphemeral.Ttl.TTL_1H).build()
    }

    override suspend fun send(request: AiRequest): AiResult {
        val apiKey = secretStore.read(Secret.CLAUDE_API_KEY) ?: return AiResult.Failure(AiFailureKind.NO_KEY, "No API key")
        return try {
            val message = runInterruptible(Dispatchers.IO) { client.withKey(apiKey).messages().create(request.toParams()) }
            AiResult.Success(message.toResponse())
        } catch (e: CancellationException) {
            throw e
        } catch (e: AnthropicServiceException) {
            AiResult.Failure(e.failureKind(), e.apiMessage().orElse(e.message.orEmpty()))
        } catch (e: Exception) {
            AiResult.Failure(AiFailureKind.NO_NETWORK, e.message.orEmpty())
        }
    }

    override suspend fun checkApiKey(apiKey: String): AiFailureKind? = try {
        runInterruptible(Dispatchers.IO) { client.withKey(apiKey).models().list(ModelListParams.builder().limit(1).build()) }
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: AnthropicServiceException) {
        e.failureKind()
    } catch (_: Exception) {
        AiFailureKind.NO_NETWORK
    }

    private fun AnthropicClient.withKey(apiKey: String) = withOptions { it.replaceHeaders("x-api-key", apiKey) }

    private fun AnthropicServiceException.failureKind() = when (this) {
        is UnauthorizedException, is PermissionDeniedException -> AiFailureKind.KEY_REJECTED
        is RateLimitException, is OverloadedException -> AiFailureKind.OVERLOADED
        else -> if (statusCode() >= HTTP_SERVER_ERROR) AiFailureKind.OVERLOADED else AiFailureKind.OTHER
    }

    private fun AiRequest.toParams(): MessageCreateParams {
        val cache = if (tuning.cacheDuration == AiCacheDuration.ONE_HOUR) cachedForAnHour else cachedForMinutes
        val builder = MessageCreateParams.builder()
            .model(model)
            .maxTokens(tuning.maxTokens.toLong())
            .systemOfTextBlockParams(listOf(TextBlockParam.builder().text(system).cacheControl(cache).build()))
            .cacheControl(cachedForMinutes)
            .messages(messages.map { it.toParam() })
            .tools(tools.mapIndexed { index, tool -> tool.toTool(cache.takeIf { index == tools.lastIndex }) }.map { ToolUnion.ofTool(it) })
        tuning.effort?.let { builder.outputConfig(OutputConfig.builder().effort(it.toSdk()).build()) }
        thinkingOff()?.let { builder.putAdditionalBodyProperty("thinking", JsonValue.from(mapOf("type" to it))) }
        return builder.build()
    }

    private fun AiRequest.thinkingOff(): String? {
        val allowed = model in AiTuning.MODELS_WITH_THINKING_OFF &&
            (tuning.effort ?: AiTuning.HIGHEST_EFFORT_WITH_THINKING_OFF) <= AiTuning.HIGHEST_EFFORT_WITH_THINKING_OFF
        return when {
            tuning.thinking == AiThinking.ADAPTIVE || !allowed -> null
            model == SONNET -> "between_tools"
            else -> "disabled"
        }
    }

    private fun AiEffort.toSdk(): OutputConfig.Effort = when (this) {
        AiEffort.LOW -> OutputConfig.Effort.LOW
        AiEffort.MEDIUM -> OutputConfig.Effort.MEDIUM
        AiEffort.HIGH -> OutputConfig.Effort.HIGH
        AiEffort.XHIGH -> OutputConfig.Effort.XHIGH
        AiEffort.MAX -> OutputConfig.Effort.MAX
    }

    private fun AiTool.toTool(cache: CacheControlEphemeral?): Tool {
        val builder = Tool.builder()
            .name(name)
            .description(description)
            .inputSchema(jsonMapper().readValue(inputSchemaJson, Tool.InputSchema::class.java))
        cache?.let { builder.cacheControl(it) }
        return builder.build()
    }

    private fun AiMessage.toParam(): MessageParam {
        val blocks = content.map { it.toParam() }
        return if (role == AiRole.USER) MessageParam.ofUserOfContentBlockParams(blocks) else MessageParam.ofAssistantOfContentBlockParams(blocks)
    }

    private fun AiContent.toParam(): ContentBlockParam = when (this) {
        is AiContent.Text -> ContentBlockParam.ofText(TextBlockParam.builder().text(text).build())
        is AiContent.ToolUse -> ContentBlockParam.ofToolUse(
            ToolUseBlockParam.builder().id(id).name(name).input(jsonMapper().readValue(inputJson, JsonValue::class.java)).build(),
        )
        is AiContent.ToolResult -> ContentBlockParam.ofToolResult(
            ToolResultBlockParam.builder().toolUseId(toolUseId).content(content).isError(isError).build(),
        )
        is AiContent.Opaque -> jsonMapper().readValue(rawJson, ContentBlockParam::class.java)
    }

    private fun Message.toResponse(): AiResponse {
        val usage = usage()
        return AiResponse(
            content = content().map { it.toContent() },
            stopReason = stopReason().map { it.asString() }.orElse(null),
            inputTokens = usage.inputTokens().toInt(),
            cacheReadTokens = usage.cacheReadInputTokens().orElse(0).toInt(),
            cacheWriteTokens = usage.cacheCreationInputTokens().orElse(0).toInt(),
            outputTokens = usage.outputTokens().toInt(),
        )
    }

    private fun ContentBlock.toContent(): AiContent = when {
        isText() -> AiContent.Text(asText().text())
        isToolUse() -> asToolUse().let { AiContent.ToolUse(it.id(), it.name(), jsonMapper().writeValueAsString(it._input())) }
        else -> AiContent.Opaque(jsonMapper().writeValueAsString(this))
    }
}

private const val HTTP_SERVER_ERROR = 500
