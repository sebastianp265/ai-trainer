package com.github.sebastianp265.aitrainer.shared.claude.impl

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
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeCacheDuration
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeClient
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeContent
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeEffort
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeFailureKind
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeMessage
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeRequest
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeResponse
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeResult
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeRole
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeThinking
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeTool
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeTuning
import com.github.sebastianp265.aitrainer.integration.settings.Secret
import com.github.sebastianp265.aitrainer.integration.settings.SecretStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runInterruptible

internal class SdkClaudeClient(
    private val client: AnthropicClient,
    private val secretStore: SecretStore,
) : ClaudeClient {

    private companion object {
        const val SONNET = "claude-sonnet-5-5"
        val cachedForMinutes: CacheControlEphemeral = CacheControlEphemeral.builder().build()
        val cachedForAnHour: CacheControlEphemeral = CacheControlEphemeral.builder().ttl(CacheControlEphemeral.Ttl.TTL_1H).build()
    }

    override suspend fun send(request: ClaudeRequest): ClaudeResult {
        val apiKey = secretStore.read(Secret.CLAUDE_API_KEY) ?: return ClaudeResult.Failure(ClaudeFailureKind.NO_KEY, "No API key")
        return try {
            val message = runInterruptible(Dispatchers.IO) { client.withKey(apiKey).messages().create(request.toParams()) }
            ClaudeResult.Success(message.toResponse())
        } catch (e: CancellationException) {
            throw e
        } catch (e: AnthropicServiceException) {
            ClaudeResult.Failure(e.failureKind(), e.apiMessage().orElse(e.message.orEmpty()))
        } catch (e: Exception) {
            ClaudeResult.Failure(ClaudeFailureKind.NO_NETWORK, e.message.orEmpty())
        }
    }

    override suspend fun checkApiKey(apiKey: String): ClaudeFailureKind? = try {
        runInterruptible(Dispatchers.IO) { client.withKey(apiKey).models().list(ModelListParams.builder().limit(1).build()) }
        null
    } catch (e: CancellationException) {
        throw e
    } catch (e: AnthropicServiceException) {
        e.failureKind()
    } catch (_: Exception) {
        ClaudeFailureKind.NO_NETWORK
    }

    private fun AnthropicClient.withKey(apiKey: String) = withOptions { it.replaceHeaders("x-api-key", apiKey) }

    private fun AnthropicServiceException.failureKind() = when (this) {
        is UnauthorizedException, is PermissionDeniedException -> ClaudeFailureKind.KEY_REJECTED
        is RateLimitException, is OverloadedException -> ClaudeFailureKind.OVERLOADED
        else -> if (statusCode() >= HTTP_SERVER_ERROR) ClaudeFailureKind.OVERLOADED else ClaudeFailureKind.OTHER
    }

    private fun ClaudeRequest.toParams(): MessageCreateParams {
        val cache = if (tuning.cacheDuration == ClaudeCacheDuration.ONE_HOUR) cachedForAnHour else cachedForMinutes
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

    private fun ClaudeRequest.thinkingOff(): String? {
        val allowed = model in ClaudeTuning.MODELS_WITH_THINKING_OFF &&
            (tuning.effort ?: ClaudeTuning.HIGHEST_EFFORT_WITH_THINKING_OFF) <= ClaudeTuning.HIGHEST_EFFORT_WITH_THINKING_OFF
        return when {
            tuning.thinking == ClaudeThinking.ADAPTIVE || !allowed -> null
            model == SONNET -> "between_tools"
            else -> "disabled"
        }
    }

    private fun ClaudeEffort.toSdk(): OutputConfig.Effort = when (this) {
        ClaudeEffort.LOW -> OutputConfig.Effort.LOW
        ClaudeEffort.MEDIUM -> OutputConfig.Effort.MEDIUM
        ClaudeEffort.HIGH -> OutputConfig.Effort.HIGH
        ClaudeEffort.XHIGH -> OutputConfig.Effort.XHIGH
        ClaudeEffort.MAX -> OutputConfig.Effort.MAX
    }

    private fun ClaudeTool.toTool(cache: CacheControlEphemeral?): Tool {
        val builder = Tool.builder()
            .name(name)
            .description(description)
            .inputSchema(jsonMapper().readValue(inputSchemaJson, Tool.InputSchema::class.java))
        cache?.let { builder.cacheControl(it) }
        return builder.build()
    }

    private fun ClaudeMessage.toParam(): MessageParam {
        val blocks = content.map { it.toParam() }
        return if (role == ClaudeRole.USER) MessageParam.ofUserOfContentBlockParams(blocks) else MessageParam.ofAssistantOfContentBlockParams(blocks)
    }

    private fun ClaudeContent.toParam(): ContentBlockParam = when (this) {
        is ClaudeContent.Text -> ContentBlockParam.ofText(TextBlockParam.builder().text(text).build())
        is ClaudeContent.ToolUse -> ContentBlockParam.ofToolUse(
            ToolUseBlockParam.builder().id(id).name(name).input(jsonMapper().readValue(inputJson, JsonValue::class.java)).build(),
        )
        is ClaudeContent.ToolResult -> ContentBlockParam.ofToolResult(
            ToolResultBlockParam.builder().toolUseId(toolUseId).content(content).isError(isError).build(),
        )
        is ClaudeContent.Opaque -> jsonMapper().readValue(rawJson, ContentBlockParam::class.java)
    }

    private fun Message.toResponse(): ClaudeResponse {
        val usage = usage()
        return ClaudeResponse(
            content = content().map { it.toContent() },
            stopReason = stopReason().map { it.asString() }.orElse(null),
            inputTokens = usage.inputTokens().toInt(),
            cacheReadTokens = usage.cacheReadInputTokens().orElse(0).toInt(),
            cacheWriteTokens = usage.cacheCreationInputTokens().orElse(0).toInt(),
            outputTokens = usage.outputTokens().toInt(),
        )
    }

    private fun ContentBlock.toContent(): ClaudeContent = when {
        isText() -> ClaudeContent.Text(asText().text())
        isToolUse() -> asToolUse().let { ClaudeContent.ToolUse(it.id(), it.name(), jsonMapper().writeValueAsString(it._input())) }
        else -> ClaudeContent.Opaque(jsonMapper().writeValueAsString(this))
    }
}

private const val HTTP_SERVER_ERROR = 500
