package com.github.sebastianp265.aitrainer.feature.workout.impl

import com.github.sebastianp265.aitrainer.integration.workout.LastTimeGroup

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

internal fun formatElapsed(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}
