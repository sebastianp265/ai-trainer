package com.github.sebastianp265.aitrainer.shared.ai

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ai_usage")
internal data class AiUsageEntity(
    @PrimaryKey val month: String,
    val messages: Int,
    val inputTokens: Long,
    val outputTokens: Long,
)
