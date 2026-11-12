package com.github.sebastianp265.aitrainer.shared.claude

import com.anthropic.client.AnthropicClient
import com.anthropic.client.okhttp.AnthropicOkHttpClient
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeClient
import com.github.sebastianp265.aitrainer.shared.claude.impl.SdkClaudeClient
import org.koin.core.module.dsl.singleOf
import org.koin.dsl.bind
import org.koin.dsl.module

internal val claudeModule = module {
    single<AnthropicClient> { AnthropicOkHttpClient.builder().build() }
    singleOf(::SdkClaudeClient) bind ClaudeClient::class
}
