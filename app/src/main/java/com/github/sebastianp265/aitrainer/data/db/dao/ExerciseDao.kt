package com.github.sebastianp265.aitrainer.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import com.github.sebastianp265.aitrainer.data.db.entity.ExerciseEntity
import com.github.sebastianp265.aitrainer.domain.model.ExercisePreference
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    @Update
    suspend fun update(exercise: ExerciseEntity)

    @Query("UPDATE exercise SET preference = :preference WHERE id = :id")
    suspend fun setPreference(id: Long, preference: ExercisePreference)
}
