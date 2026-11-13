package com.github.sebastianp265.aitrainer.shared.nutrition

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
internal interface NutritionDao {
    @Query("SELECT * FROM nutrition_target WHERE id = 1")
    fun observeTarget(): Flow<NutritionTargetEntity?>

    @Upsert
    suspend fun upsertTarget(target: NutritionTargetEntity)

    @Query("DELETE FROM nutrition_target")
    suspend fun deleteTarget()
}
