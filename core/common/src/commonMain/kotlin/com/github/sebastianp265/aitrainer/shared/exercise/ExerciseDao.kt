package com.github.sebastianp265.aitrainer.shared.exercise

import androidx.room.Dao
import androidx.room.Query

@Dao
internal interface ExerciseDao {
    @Query("SELECT * FROM exercise")
    suspend fun exercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercise_translation")
    suspend fun exerciseTranslations(): List<ExerciseTranslationEntity>

    @Query("SELECT * FROM exercise_muscle")
    suspend fun exerciseMuscles(): List<ExerciseMuscleEntity>

    @Query("SELECT * FROM muscle_group")
    suspend fun muscleGroups(): List<MuscleGroupEntity>

    @Query("SELECT * FROM muscle_group_translation")
    suspend fun muscleGroupTranslations(): List<MuscleGroupTranslationEntity>

    @Query("SELECT * FROM equipment")
    suspend fun equipment(): List<EquipmentEntity>

    @Query("SELECT * FROM equipment_translation")
    suspend fun equipmentTranslations(): List<EquipmentTranslationEntity>
}
