package com.github.sebastianp265.aitrainer.shared.workout

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.workout.LastTimeGroup
import java.text.NumberFormat

internal class LastTimeLabels(
    val kg: String,
    val rpe: String,
    val bodyweight: String,
    val formatNumber: (Double) -> String,
)

internal fun LastTimeGroup.text(labels: LastTimeLabels): String {
    val sets = if (setCount > 1) "$setCount×" else ""
    val load = if (weightKg > 0) "$reps×${labels.formatNumber(weightKg)} ${labels.kg}" else "$reps · ${labels.bodyweight}"
    val rpeText = rpeText(labels)
    return "$sets$load$rpeText"
}

private fun LastTimeGroup.rpeText(labels: LastTimeLabels): String {
    val from = rpeFrom ?: return ""
    val to = rpeTo ?: return ""
    val range = if (from == to) labels.formatNumber(from) else "${labels.formatNumber(from)}–${labels.formatNumber(to)}"
    return " ${labels.rpe} $range"
}

internal fun hiddenSetCount(groups: List<LastTimeGroup>, shownGroupCount: Int): Int =
    groups.drop(shownGroupCount).sumOf { it.setCount }

@Composable
internal fun rememberLastTimeLabels(): LastTimeLabels {
    val numberFormat = NumberFormat.getNumberInstance(LocalConfiguration.current.locales[0]).apply { maximumFractionDigits = 2 }
    return LastTimeLabels(
        kg = stringResource(R.string.unit_kg),
        rpe = stringResource(R.string.rpe_label),
        bodyweight = stringResource(R.string.bodyweight_label),
        formatNumber = { numberFormat.format(it) },
    )
}
