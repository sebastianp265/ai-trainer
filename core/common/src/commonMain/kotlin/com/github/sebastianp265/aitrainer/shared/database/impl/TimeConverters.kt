package com.github.sebastianp265.aitrainer.shared.database.impl

import androidx.room.TypeConverter
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

internal class TimeConverters {
    @TypeConverter
    fun localDateToIsoString(value: LocalDate?): String? = value?.toString()

    @TypeConverter
    fun isoStringToLocalDate(value: String?): LocalDate? = value?.let(LocalDate::parse)

    @TypeConverter
    fun instantToEpochMillis(value: Instant?): Long? = value?.toEpochMilliseconds()

    @TypeConverter
    fun epochMillisToInstant(value: Long?): Instant? = value?.let(Instant::fromEpochMilliseconds)
}
