package com.github.sebastianp265.aitrainer.feature.exercise.data

import androidx.room.TypeConverter
import com.github.sebastianp265.aitrainer.feature.exercise.domain.model.MuscleGroup

class ExerciseConverters {
    @TypeConverter
    fun muscleGroupsToString(value: List<MuscleGroup>): String = value.joinToString(SEPARATOR) { it.name }

    @TypeConverter
    fun stringToMuscleGroups(value: String): List<MuscleGroup> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR).map(MuscleGroup::valueOf)

    private companion object {
        const val SEPARATOR = ","
    }
}
