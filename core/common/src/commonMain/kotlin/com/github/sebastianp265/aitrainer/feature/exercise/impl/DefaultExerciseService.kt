package com.github.sebastianp265.aitrainer.feature.exercise.impl

import com.github.sebastianp265.aitrainer.shared.exercise.EquipmentEntity
import com.github.sebastianp265.aitrainer.shared.exercise.EquipmentTranslationEntity
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseDao
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseEntity
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseMuscleEntity
import com.github.sebastianp265.aitrainer.shared.exercise.ExerciseTranslationEntity
import com.github.sebastianp265.aitrainer.shared.exercise.MuscleGroupEntity
import com.github.sebastianp265.aitrainer.shared.exercise.MuscleGroupTranslationEntity
import com.github.sebastianp265.aitrainer.integration.exercise.Equipment
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup

internal class DefaultExerciseService(private val dao: ExerciseDao) : ExerciseService {

    override suspend fun exercises(): List<Exercise> {
        val muscleNames = dao.muscleGroupTranslations().muscleNamesByKey()
        val muscleGroups = dao.muscleGroups().associate { it.key to it.toModel(muscleNames) }
        val equipmentNames = dao.equipmentTranslations().equipmentNamesByKey()
        val equipment = dao.equipment().associate { it.key to it.toModel(equipmentNames) }
        val exerciseNames = dao.exerciseTranslations().exerciseNamesByKey()
        val musclesByExercise: Map<String, List<ExerciseMuscleEntity>> = dao.exerciseMuscles().groupBy { it.exerciseKey }
        return dao.exercises().map { exercise ->
            val (primary, secondary) = musclesByExercise.getValue(exercise.key).partition { it.isPrimary }
            exercise.toModel(
                names = exerciseNames.getValue(exercise.key),
                equipment = equipment.getValue(exercise.equipmentKey),
                primaryMuscles = primary.map { muscleGroups.getValue(it.muscleGroupKey) },
                secondaryMuscles = secondary.map { muscleGroups.getValue(it.muscleGroupKey) },
            )
        }
    }
}

private fun List<MuscleGroupTranslationEntity>.muscleNamesByKey(): Map<String, Map<String, String>> =
    groupBy({ it.muscleGroupKey }, { it.language to it.name }).mapValues { it.value.toMap() }

private fun List<EquipmentTranslationEntity>.equipmentNamesByKey(): Map<String, Map<String, String>> =
    groupBy({ it.equipmentKey }, { it.language to it.name }).mapValues { it.value.toMap() }

private fun List<ExerciseTranslationEntity>.exerciseNamesByKey(): Map<String, Map<String, String>> =
    groupBy({ it.exerciseKey }, { it.language to it.name }).mapValues { it.value.toMap() }

private fun MuscleGroupEntity.toModel(names: Map<String, Map<String, String>>) = MuscleGroup(key, names.getValue(key))

private fun EquipmentEntity.toModel(names: Map<String, Map<String, String>>) = Equipment(key, names.getValue(key))

private fun ExerciseEntity.toModel(
    names: Map<String, String>,
    equipment: Equipment,
    primaryMuscles: List<MuscleGroup>,
    secondaryMuscles: List<MuscleGroup>,
) = Exercise(key, names, equipment, primaryMuscles, secondaryMuscles)
