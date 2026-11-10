package com.github.sebastianp265.aitrainer.feature.progress.impl

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.feature.progress.ProgressMetric
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import com.github.sebastianp265.aitrainer.shared.localization.formatDate
import com.github.sebastianp265.aitrainer.shared.localization.formatNumber
import kotlinx.datetime.LocalDate
import kotlin.math.roundToInt

@Composable
internal fun MetricContent(metric: ProgressMetric, days: List<HealthDay>?, today: LocalDate, onOpenDay: (LocalDate) -> Unit) {
    if (days == null) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }
    var range by rememberSaveable { mutableStateOf(ProgressRange.THREE_MONTHS) }
    val detail = remember(days, metric, range, today) { MetricCalculator.calculate(days, metric, range, today) }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().testTag(MetricTags.METRIC),
    ) {
        item { Text(stringResource(metric.titleRes()), style = MaterialTheme.typography.headlineSmall) }
        item { RangeSwitch(range) { range = it } }
        if (detail == null) {
            item { Text(stringResource(R.string.metric_no_data), style = MaterialTheme.typography.bodyMedium) }
            return@LazyColumn
        }
        item { Headline(metric, detail, range) }
        item { LineChart(detail.points, detail.averageSeries) }
        item { Extremes(metric, detail) }
        item { Text(stringResource(R.string.metric_values), style = MaterialTheme.typography.titleMedium) }
        items(detail.newestFirst, key = { it.date.toString() }) { point ->
            Column(Modifier.fillMaxWidth().clickable { onOpenDay(point.date) }) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatDate(point.date))
                    Text(formatMetricValue(metric, point.value), style = MaterialTheme.typography.titleSmall)
                }
                HorizontalDivider()
            }
        }
        item { Text(stringResource(R.string.metric_footnote), style = MaterialTheme.typography.bodySmall) }
    }
}

@Composable
private fun Headline(metric: ProgressMetric, detail: MetricDetail, range: ProgressRange) {
    Column {
        Text(stringResource(R.string.metric_average), style = MaterialTheme.typography.labelMedium)
        Text(formatMetricValue(metric, detail.average), style = MaterialTheme.typography.headlineMedium)
        detail.change?.let {
            Text(
                stringResource(R.string.progress_change, formatMetricDelta(metric, it), stringResource(range.inRangeRes())),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}

@Composable
private fun Extremes(metric: ProgressMetric, detail: MetricDetail) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Stat(formatMetricValue(metric, detail.lowest), R.string.metric_lowest)
        Stat(formatMetricValue(metric, detail.highest), R.string.metric_highest)
        detail.changePerWeek?.let { Stat(stringResource(R.string.metric_per_week, formatMetricDelta(metric, it)), null) }
    }
}

@Composable
private fun Stat(value: String, @StringRes label: Int?) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium)
        label?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall) }
    }
}

@StringRes
internal fun ProgressMetric.titleRes(): Int = when (this) {
    ProgressMetric.WEIGHT -> R.string.hc_type_weight
    ProgressMetric.CALORIE_BALANCE -> R.string.metric_calorie_balance
    ProgressMetric.STEPS -> R.string.progress_steps
    ProgressMetric.SLEEP -> R.string.progress_sleep
    ProgressMetric.RESTING_HEART_RATE -> R.string.progress_rhr
    ProgressMetric.HEART_RATE_VARIABILITY -> R.string.progress_hrv
}

@Composable
internal fun formatMetricValue(metric: ProgressMetric, value: Double): String = when (metric) {
    ProgressMetric.WEIGHT -> stringResource(R.string.value_kg, formatNumber(value, 1))
    ProgressMetric.CALORIE_BALANCE -> stringResource(R.string.value_kcal, formatNumber(value, 0, signed = true))
    ProgressMetric.STEPS -> formatNumber(value)
    ProgressMetric.SLEEP -> formatDuration(value.roundToInt())
    ProgressMetric.RESTING_HEART_RATE -> stringResource(R.string.value_bpm, formatNumber(value, 1))
    ProgressMetric.HEART_RATE_VARIABILITY -> stringResource(R.string.value_ms, formatNumber(value))
}

@Composable
internal fun formatMetricDelta(metric: ProgressMetric, value: Double): String = when (metric) {
    ProgressMetric.WEIGHT -> stringResource(R.string.value_kg, formatNumber(value, 2, signed = true))
    ProgressMetric.CALORIE_BALANCE -> stringResource(R.string.value_kcal, formatNumber(value, 0, signed = true))
    ProgressMetric.STEPS -> formatNumber(value, 0, signed = true)
    ProgressMetric.SLEEP -> stringResource(R.string.value_min, formatNumber(value, 0, signed = true))
    ProgressMetric.RESTING_HEART_RATE -> stringResource(R.string.value_bpm, formatNumber(value, 1, signed = true))
    ProgressMetric.HEART_RATE_VARIABILITY -> stringResource(R.string.value_ms, formatNumber(value, 0, signed = true))
}

internal object MetricTags {
    const val METRIC = "progress_metric"
}

@PreviewLightDark
@Composable
private fun MetricContentPreview() {
    MaterialTheme {
        Surface {
            val today = LocalDate(2026, 11, 10)
            val days = (1..30).map { offset ->
                HealthDay(
                    LocalDate.fromEpochDays(today.toEpochDays() - offset), null, null, null, null, null, null, null, null,
                    null, 83.0 - offset * 0.05, null, null, null, null,
                )
            }
            MetricContent(ProgressMetric.WEIGHT, days, today, onOpenDay = {})
        }
    }
}
