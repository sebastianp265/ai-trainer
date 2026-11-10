package com.github.sebastianp265.aitrainer.feature.progress.impl

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.feature.health.HealthConnectCard
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import com.github.sebastianp265.aitrainer.shared.localization.formatDate
import com.github.sebastianp265.aitrainer.shared.localization.formatNumber
import kotlinx.datetime.LocalDate

@Composable
internal fun ProgressContent(
    state: ProgressUiState,
    onRangeSelected: (ProgressRange) -> Unit,
    onHealthConnectChanged: () -> Unit,
) {
    val cards = state.cards
    if (cards == null) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { HealthConnectCard(onConnectionChanged = onHealthConnectChanged) }
        item { RangeSwitch(state.range, onRangeSelected) }
        cards.weight?.let { item { WeightCard(it, state.range) } }
        cards.balance?.let { item { BalanceCard(it) } }
        cards.macros?.let { item { MacrosCard(it) } }
        cards.steps?.let { item { AverageCard(R.string.progress_steps, R.string.progress_steps_hint, formatNumber(it)) } }
        cards.sleepMinutes?.let {
            item { AverageCard(R.string.progress_sleep, R.string.progress_sleep_hint, formatDuration(it.toInt())) }
        }
        cards.restingHeartRate?.let { item { RestingHeartRateCard(it, state.range) } }
        cards.hrvMs?.let {
            item { AverageCard(R.string.progress_hrv, R.string.progress_sleep_hint, stringResource(R.string.value_ms, formatNumber(it))) }
        }
        if (cards.recentDays.isNotEmpty()) item { DaysCard(cards.recentDays) }
    }
}

@Composable
private fun RangeSwitch(selected: ProgressRange, onSelected: (ProgressRange) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        ProgressRange.entries.forEachIndexed { index, range ->
            SegmentedButton(
                selected = range == selected,
                onClick = { onSelected(range) },
                shape = SegmentedButtonDefaults.itemShape(index, ProgressRange.entries.size),
                modifier = Modifier.testTag(ProgressTags.range(range)),
            ) { Text(stringResource(range.labelRes())) }
        }
    }
}

@Composable
internal fun ProgressCard(@StringRes title: Int, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    OutlinedCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun WeightCard(card: TrendCard, range: ProgressRange) {
    ProgressCard(R.string.progress_weight_title, Modifier.testTag(ProgressTags.WEIGHT_CARD)) {
        Text(stringResource(R.string.value_kg, formatNumber(card.current, 1)), style = MaterialTheme.typography.headlineMedium)
        card.change?.let { change ->
            Text(
                stringResource(R.string.progress_change, stringResource(R.string.value_kg, formatNumber(change, 1, signed = true)), stringResource(range.inRangeRes())),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        LineChart(card.points, card.average)
    }
}

@Composable
private fun BalanceCard(card: BalanceCard) {
    ProgressCard(R.string.progress_balance_title, Modifier.testTag(ProgressTags.BALANCE_CARD)) {
        Text(
            stringResource(R.string.value_kcal, formatNumber(card.averageBalanceKcal.toDouble(), signed = true)),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            stringResource(
                R.string.progress_balance_detail,
                formatNumber(card.averageEatenKcal.toDouble()),
                formatNumber(card.averageBurnedKcal.toDouble()),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        BarChart(card.weeks.map { it.balanceKcal })
        Text(stringResource(R.string.progress_balance_weeks), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun MacrosCard(card: MacrosCard) {
    ProgressCard(R.string.progress_macros_title, Modifier.testTag(ProgressTags.MACROS_CARD)) {
        listOf(
            R.string.macro_protein to card.proteinG,
            R.string.macro_carbs to card.carbsG,
            R.string.macro_fat to card.fatG,
            R.string.macro_fiber to card.fiberG,
        ).forEach { (label, grams) ->
            grams?.let { LabeledValue(stringResource(label), stringResource(R.string.value_grams, formatNumber(it))) }
        }
    }
}

@Composable
private fun AverageCard(@StringRes title: Int, @StringRes hint: Int, value: String) {
    ProgressCard(title, Modifier.testTag(ProgressTags.card(title))) {
        LabeledValue(stringResource(hint), value)
    }
}

@Composable
private fun RestingHeartRateCard(card: TrendCard, range: ProgressRange) {
    ProgressCard(R.string.progress_rhr, Modifier.testTag(ProgressTags.card(R.string.progress_rhr))) {
        val value = stringResource(R.string.value_bpm, formatNumber(card.current))
        val change = card.change?.let {
            stringResource(R.string.progress_change, formatNumber(it, signed = true), stringResource(range.inRangeRes()))
        }
        LabeledValue(change.orEmpty(), value)
    }
}

@Composable
private fun DaysCard(days: List<HealthDay>) {
    ProgressCard(R.string.progress_days, Modifier.testTag(ProgressTags.DAYS_CARD)) {
        days.forEach { day -> DayRow(day) }
    }
}

@Composable
private fun DayRow(day: HealthDay) {
    Column(Modifier.fillMaxWidth()) {
        Text(formatDate(day.date), style = MaterialTheme.typography.labelLarge)
        Text(day.summary(), Modifier.testTag(ProgressTags.healthDay(day.date)), style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun HealthDay.summary(): String {
    val parts = listOfNotNull(
        kcalEaten?.let { stringResource(R.string.day_eaten, stringResource(R.string.value_kcal, formatNumber(it.toDouble()))) },
        kcalBurned?.let { stringResource(R.string.day_burned, stringResource(R.string.value_kcal, formatNumber(it.toDouble()))) },
        weightKg?.let { stringResource(R.string.value_kg, formatNumber(it, 1)) },
        sleepMinutes?.let { stringResource(R.string.day_sleep, formatDuration(it)) },
        steps?.let { stringResource(R.string.day_steps, formatNumber(it.toDouble())) },
    )
    return parts.joinToString(" · ")
}

@Composable
private fun LabeledValue(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
internal fun formatDuration(minutes: Int): String = stringResource(R.string.duration_hours_minutes, minutes / 60, minutes % 60)

@StringRes
private fun ProgressRange.labelRes() = when (this) {
    ProgressRange.FOUR_WEEKS -> R.string.progress_range_4w
    ProgressRange.THREE_MONTHS -> R.string.progress_range_3m
    ProgressRange.YEAR -> R.string.progress_range_year
}

@StringRes
internal fun ProgressRange.inRangeRes() = when (this) {
    ProgressRange.FOUR_WEEKS -> R.string.progress_in_4w
    ProgressRange.THREE_MONTHS -> R.string.progress_in_3m
    ProgressRange.YEAR -> R.string.progress_in_year
}

internal object ProgressTags {
    const val WEIGHT_CARD = "progress_weight_card"
    const val BALANCE_CARD = "progress_balance_card"
    const val MACROS_CARD = "progress_macros_card"
    const val DAYS_CARD = "progress_days_card"
    fun card(@StringRes title: Int) = "progress_card:$title"
    fun range(range: ProgressRange) = "progress_range:${range.name}"
    fun healthDay(date: LocalDate) = "progress_health_day:$date"
}

@PreviewLightDark
@Composable
private fun ProgressContentPreview() {
    MaterialTheme {
        Surface {
            val today = LocalDate(2026, 11, 10)
            val days = (1..12).map { offset ->
                HealthDay(
                    LocalDate.fromEpochDays(today.toEpochDays() - offset), 2300, 160.0, 250.0, 60.0, 70.0, 20.0, 24.0, 2000.0,
                    2600, 82.0 - offset * 0.05, 8400, 440, 54.0, 62.0,
                )
            }
            ProgressContent(
                state = ProgressUiState(cards = ProgressCalculator.calculate(days, ProgressRange.FOUR_WEEKS, today)),
                onRangeSelected = {},
                onHealthConnectChanged = {},
            )
        }
    }
}
