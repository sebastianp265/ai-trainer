package com.github.sebastianp265.aitrainer.data.db

import androidx.room.TypeConverter
import com.github.sebastianp265.aitrainer.domain.model.Equipment
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

class Converters {
    // ISO text so date ranges can be compared with BETWEEN.
    @TypeConverter
    fun localDateToString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun stringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::fromEpochMilliseconds)

    @TypeConverter
    fun muscleGroupsToString(value: List<MuscleGroup>): String = value.joinToString(SEPARATOR) { it.name }

    @TypeConverter
    fun stringToMuscleGroups(value: String): List<MuscleGroup> = splitEnums(value, MuscleGroup::valueOf)

    @TypeConverter
    fun equipmentListToString(value: List<Equipment>): String = value.joinToString(SEPARATOR) { it.name }

    @TypeConverter
    fun stringToEquipmentList(value: String): List<Equipment> = splitEnums(value, Equipment::valueOf)

    private fun <T> splitEnums(value: String, parse: (String) -> T): List<T> =
        if (value.isEmpty()) emptyList() else value.split(SEPARATOR).map(parse)

    private companion object {
        const val SEPARATOR = ","
    }
}
