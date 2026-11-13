package com.github.sebastianp265.aitrainer.shared.ai

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.github.sebastianp265.aitrainer.integration.ai.AiClient
import com.github.sebastianp265.aitrainer.shared.ai.impl.SdkClaudeClient
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val aiClientModule = module {
    single<AnthropicClient> { AnthropicOkHttpClient.builder().build() }
    singleOf(::SdkClaudeClient) bind AiClient::class
}
