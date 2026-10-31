package com.github.sebastianp265.aitrainer.feature.exercise.data

import com.github.sebastianp265.aitrainer.feature.exercise.domain.ExerciseRepository
import com.github.sebastianp265.aitrainer.feature.exercise.domain.model.Exercise
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomExerciseRepository(
    private val dao: ExerciseDao,
) : ExerciseRepository {
    override fun observeExercises(): Flow<List<Exercise>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }
}

internal fun ExerciseEntity.toDomain() = Exercise(id, key, primaryMuscles, secondaryMuscles, equipment)
