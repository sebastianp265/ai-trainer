package com.github.sebastianp265.aitrainer.shared.ai

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
internal interface AiDao {
    @Query("SELECT * FROM ai_usage WHERE month = :month")
    fun observeUsage(month: String): Flow<AiUsageEntity?>

    @Query("SELECT * FROM ai_usage WHERE month = :month")
    suspend fun usage(month: String): AiUsageEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertUsage(usage: AiUsageEntity)
}
