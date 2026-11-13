package com.github.sebastianp265.aitrainer.shared.exercise

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
internal interface ExerciseDao {
    @Query("SELECT * FROM exercise")
    fun observeExercises(): Flow<List<ExerciseEntity>>

    @Query("SELECT * FROM exercise_translation")
    fun observeExerciseTranslations(): Flow<List<ExerciseTranslationEntity>>

    @Query("SELECT * FROM exercise_muscle")
    fun observeExerciseMuscles(): Flow<List<ExerciseMuscleEntity>>

    @Query("SELECT * FROM muscle_group")
    fun observeMuscleGroups(): Flow<List<MuscleGroupEntity>>

    @Query("SELECT * FROM muscle_group_translation")
    fun observeMuscleGroupTranslations(): Flow<List<MuscleGroupTranslationEntity>>

    @Query("SELECT * FROM equipment")
    fun observeEquipment(): Flow<List<EquipmentEntity>>

    @Query("SELECT * FROM equipment_translation")
    fun observeEquipmentTranslations(): Flow<List<EquipmentTranslationEntity>>

    @Insert
    suspend fun insertExercise(exercise: ExerciseEntity)

    @Insert
    suspend fun insertTranslation(translation: ExerciseTranslationEntity)

    @Insert
    suspend fun insertMuscles(muscles: List<ExerciseMuscleEntity>)

    @Transaction
    suspend fun insertExerciseWithMuscles(
        exercise: ExerciseEntity,
        translation: ExerciseTranslationEntity,
        muscles: List<ExerciseMuscleEntity>,
    ) {
        insertExercise(exercise)
        insertTranslation(translation)
        insertMuscles(muscles)
    }
}
