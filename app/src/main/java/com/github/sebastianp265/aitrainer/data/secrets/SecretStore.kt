package com.github.sebastianp265.aitrainer.data.secrets

import kotlinx.coroutines.flow.Flow

enum class Secret {
    CLAUDE_API_KEY,
}

interface SecretStore {
    suspend fun read(secret: Secret): String?
    suspend fun write(secret: Secret, value: String)
    suspend fun delete(secret: Secret)
    fun observeIsStored(secret: Secret): Flow<Boolean>
}
