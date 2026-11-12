package com.github.sebastianp265.aitrainer.shared.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.datetime.LocalDate
import kotlinx.datetime.Month
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlin.time.Instant
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun formatDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    return format.format(date.toJavaLocalDate())
}

@Composable
internal fun formatNumber(value: Double, fractionDigits: Int = 0, signed: Boolean = false): String {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale, fractionDigits, signed) {
        (NumberFormat.getNumberInstance(locale) as DecimalFormat).apply {
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
            if (signed) positivePrefix = "+"
        }
    }
    return format.format(value)
}

@Composable
internal fun formatDateLong(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale) }
    return format.format(date.toJavaLocalDate())
}

@Composable
internal fun formatWeekdayDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { DateTimeFormatter.ofPattern("EEE d MMM", locale) }
    return format.format(date.toJavaLocalDate())
}

@Composable
internal fun formatMonth(year: Int, month: Month): String {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { DateTimeFormatter.ofPattern("LLLL yyyy", locale) }
    return format.format(YearMonth.of(year, month.ordinal + 1)).replaceFirstChar { it.titlecase(locale) }
}

@Composable
internal fun formatMonthName(month: Month): String {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { DateTimeFormatter.ofPattern("LLLL", locale) }
    return format.format(java.time.Month.of(month.ordinal + 1)).replaceFirstChar { it.titlecase(locale) }
}

@Composable
internal fun formatTime(instant: Instant): String {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale) }
    return format.format(instant.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime())
}

@Composable
internal fun formatWeight(kg: Double): String {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 } }
    return format.format(kg)
}

@Composable
internal fun formatLongWeekdayDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { DateTimeFormatter.ofPattern("EEEE d MMM", locale) }
    return format.format(date.toJavaLocalDate())
}

@Composable
internal fun formatDayMonth(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
    return format.format(date.toJavaLocalDate())
}

@Composable
internal fun formatWeekdayShort(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { DateTimeFormatter.ofPattern("EEE", locale) }
    return format.format(date.toJavaLocalDate())
}

@Composable
internal fun formatWeekRange(first: LocalDate, last: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    val dayMonth = remember(locale) { DateTimeFormatter.ofPattern("d MMM", locale) }
    val month = remember(locale) { DateTimeFormatter.ofPattern("MMM", locale) }
    return if (first.month == last.month) "${first.day}–${last.day} ${month.format(last.toJavaLocalDate())}"
    else "${dayMonth.format(first.toJavaLocalDate())} – ${dayMonth.format(last.toJavaLocalDate())}"
}

