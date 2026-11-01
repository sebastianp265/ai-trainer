package com.github.sebastianp265.aitrainer.feature.progress.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import java.text.DecimalFormat
import java.text.NumberFormat

@Composable
internal fun formatKcal(kcal: Int): String = rememberWholeNumberFormat().format(kcal)

@Composable
internal fun formatGrams(grams: Double): String = rememberWholeNumberFormat().format(grams)

@Composable
private fun rememberWholeNumberFormat(): NumberFormat {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) {
        (NumberFormat.getNumberInstance(locale) as DecimalFormat).apply {
            minimumFractionDigits = 0
            maximumFractionDigits = 0
        }
    }
}
