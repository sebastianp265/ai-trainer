package com.github.sebastianp265.aitrainer.core.ui

import android.content.res.Configuration
import android.content.res.Resources
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import java.text.DecimalFormat
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@Composable
fun rememberEnglishResources(): Resources {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(context, configuration) {
        val english = Configuration(configuration).apply { setLocale(Locale.ENGLISH) }
        context.createConfigurationContext(english).resources
    }
}

@Composable
@ReadOnlyComposable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

@Composable
fun formatWeight(kg: Double): String {
    val locale = currentLocale()
    val format = remember(locale) { NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 2 } }
    return format.format(kg)
}

@Composable
fun formatKcal(kcal: Int): String = rememberNumberFormat(fractionDigits = 0..0).format(kcal)

@Composable
fun formatGrams(grams: Double): String = rememberNumberFormat(fractionDigits = 0..0).format(grams)

@Composable
private fun rememberNumberFormat(fractionDigits: IntRange): NumberFormat {
    val locale = currentLocale()
    return remember(locale, fractionDigits) {
        (NumberFormat.getNumberInstance(locale) as DecimalFormat).apply {
            minimumFractionDigits = fractionDigits.first
            maximumFractionDigits = fractionDigits.last
        }
    }
}

@Composable
fun formatDate(date: LocalDate): String {
    val locale = currentLocale()
    val format = remember(locale) { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale) }
    return format.format(date.toJavaLocalDate())
}
