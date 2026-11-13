package com.github.sebastianp265.aitrainer.feature.exercise.impl

import com.github.sebastianp265.aitrainer.integration.exercise.DEFAULT_EXERCISE_LANGUAGE
import com.github.sebastianp265.aitrainer.integration.exercise.Equipment
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup
import com.github.sebastianp265.aitrainer.integration.exercise.NewExercise
import com.github.sebastianp265.aitrainer.shared.exercise.EquipmentEntity
import com.github.sebastianp265.aitrainer.shared.exercise.EquipmentTranslationEntity
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseDao
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseEntity
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseMuscleEntity
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseTranslationEntity
import com.github.sebastianp265.aitrainer.shared.exercise.MuscleGroupEntity
import com.github.sebastianp265.aitrainer.shared.exercise.MuscleGroupTranslationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

internal class DefaultExerciseService(private val dao: ExerciseDao, private val clock: Clock) : ExerciseService {

    private companion object {
        const val CUSTOM_EQUIPMENT_KEY = "OTHER"
        const val CUSTOM_KEY_PREFIX = "CUSTOM_"
    }

    private data class Tables(
        val exercises: List<ExerciseEntity>,
        val exerciseTranslations: List<ExerciseTranslationEntity>,
        val exerciseMuscles: List<ExerciseMuscleEntity>,
        val muscleGroups: List<MuscleGroupEntity>,
        val muscleGroupTranslations: List<MuscleGroupTranslationEntity>,
        val equipment: List<EquipmentEntity>,
        val equipmentTranslations: List<EquipmentTranslationEntity>,
    )

    private val tables: Flow<Tables> = combine(
        combine(dao.observeExercises(), dao.observeExerciseTranslations(), dao.observeExerciseMuscles()) { a, b, c -> Triple(a, b, c) },
        combine(dao.observeMuscleGroups(), dao.observeMuscleGroupTranslations()) { a, b -> a to b },
        combine(dao.observeEquipment(), dao.observeEquipmentTranslations()) { a, b -> a to b },
    ) { exercises, groups, equipment ->
        Tables(exercises.first, exercises.second, exercises.third, groups.first, groups.second, equipment.first, equipment.second)
    }

    override fun observeExercises(): Flow<List<Exercise>> = tables.map { it.toExercises() }

    override fun observeExercisesByKey(): Flow<Map<String, Exercise>> = observeExercises().map { list -> list.associateBy { it.key } }

    override fun observeMuscleGroups(): Flow<List<MuscleGroup>> = tables.map { it.toMuscleGroups() }

    override suspend fun exercises(): List<Exercise> = observeExercises().first()

    override suspend fun createExercise(newExercise: NewExercise): String {
        val key = CUSTOM_KEY_PREFIX + clock.now().toEpochMilliseconds()
        dao.insertExerciseWithMuscles(
            exercise = ExerciseEntity(key, CUSTOM_EQUIPMENT_KEY),
            translation = ExerciseTranslationEntity(key, DEFAULT_EXERCISE_LANGUAGE, newExercise.name.trim()),
            muscles = listOf(ExerciseMuscleEntity(key, newExercise.primaryGroupKey, isPrimary = true)) +
                (newExercise.secondaryGroupKeys - newExercise.primaryGroupKey).distinct()
                    .map { ExerciseMuscleEntity(key, it, isPrimary = false) },
        )
        return key
    }

    private fun Tables.toMuscleGroups(): List<MuscleGroup> {
        val muscleNames = muscleGroupTranslations.groupBy({ it.muscleGroupKey }, { it.language to it.name }).mapValues { it.value.toMap() }
        return muscleGroups.map { MuscleGroup(it.key, muscleNames.getValue(it.key)) }
    }

    private fun Tables.toExercises(): List<Exercise> {
        val groupsByKey = toMuscleGroups().associateBy { it.key }
        val equipmentNames = equipmentTranslations.groupBy({ it.equipmentKey }, { it.language to it.name }).mapValues { it.value.toMap() }
        val equipmentByKey = equipment.associate { it.key to Equipment(it.key, equipmentNames.getValue(it.key)) }
        val exerciseNames = exerciseTranslations.groupBy({ it.exerciseKey }, { it.language to it.name }).mapValues { it.value.toMap() }
        val musclesByExercise = exerciseMuscles.groupBy { it.exerciseKey }
        return exercises.mapNotNull { exercise ->
            val muscles = musclesByExercise[exercise.key] ?: return@mapNotNull null
            val names = exerciseNames[exercise.key] ?: return@mapNotNull null
            val (primary, secondary) = muscles.partition { it.isPrimary }
            Exercise(
                key = exercise.key,
                namesByLanguage = names,
                equipment = equipmentByKey.getValue(exercise.equipmentKey),
                primaryMuscles = primary.map { groupsByKey.getValue(it.muscleGroupKey) },
                secondaryMuscles = secondary.map { groupsByKey.getValue(it.muscleGroupKey) },
            )
        }
    }
}
