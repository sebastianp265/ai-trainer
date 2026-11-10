package com.github.sebastianp265.aitrainer.shared.localization

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import kotlin.time.Instant
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun formatDate(date: LocalDate): String {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    return format.format(date.toJavaLocalDate())
}

@Composable
internal fun formatDateTime(instant: Instant): String {
    val locale = LocalConfiguration.current.locales[0]
    val format = remember(locale) { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT).withLocale(locale) }
    return format.format(instant.toLocalDateTime(TimeZone.currentSystemDefault()).toJavaLocalDateTime())
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
