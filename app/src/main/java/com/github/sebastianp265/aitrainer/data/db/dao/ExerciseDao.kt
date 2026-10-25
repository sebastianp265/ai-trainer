package com.github.sebastianp265.aitrainer.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.github.sebastianp265.aitrainer.data.db.entity.ExerciseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercise ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise WHERE id = :id")
    suspend fun getById(id: Long): ExerciseEntity?

    /** IGNORE keeps the user's preferences on existing rows when re-seeding. */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAllIgnoringExisting(exercises: List<ExerciseEntity>)

    @Update
    suspend fun update(exercise: ExerciseEntity)
}
