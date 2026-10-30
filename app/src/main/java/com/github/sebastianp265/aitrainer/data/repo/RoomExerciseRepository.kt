package com.github.sebastianp265.aitrainer.data.repo

import com.github.sebastianp265.aitrainer.data.db.dao.ExerciseDao
import com.github.sebastianp265.aitrainer.data.db.entity.ExerciseEntity
import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.ExercisePreference
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomExerciseRepository(
    private val dao: ExerciseDao,
) : ExerciseRepository {
    override fun observeExercises(): Flow<List<Exercise>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun setPreference(exerciseId: Long, preference: ExercisePreference) =
        dao.setPreference(exerciseId, preference)
}

internal fun ExerciseEntity.toDomain() = Exercise(id, key, primaryMuscles, secondaryMuscles, equipment, preference)
