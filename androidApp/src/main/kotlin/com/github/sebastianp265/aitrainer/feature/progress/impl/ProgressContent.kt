package com.github.sebastianp265.aitrainer.feature.progress.impl

import com.github.sebastianp265.aitrainer.shared.charts.LineChart
import com.github.sebastianp265.aitrainer.shared.charts.BarChart
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import com.github.sebastianp265.aitrainer.shared.design.CardLabel
import com.github.sebastianp265.aitrainer.shared.design.Tag
import com.github.sebastianp265.aitrainer.shared.design.extendedColors
import com.github.sebastianp265.aitrainer.shared.localization.formatWeekdayDate
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
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.feature.health.HealthConnectCard
import com.github.sebastianp265.aitrainer.feature.progress.ProgressMetric
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import com.github.sebastianp265.aitrainer.shared.localization.formatNumber
import kotlinx.datetime.LocalDate

@Composable
internal fun ProgressContent(
    state: ProgressUiState,
    onRangeSelected: (ProgressRange) -> Unit,
    onHealthConnectChanged: () -> Unit,
    onOpenMetric: (ProgressMetric) -> Unit,
    onOpenDay: (LocalDate) -> Unit,
    onOpenExercise: (String) -> Unit,
    onOpenTargets: () -> Unit,
) {
    val cards = state.cards
    if (cards == null) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }
    val tiles = listOfNotNull(
        cards.steps?.let { MiniTile(R.string.progress_steps, formatNumber(it), stringResource(R.string.progress_steps_hint), ProgressMetric.STEPS) },
        cards.sleepMinutes?.let { MiniTile(R.string.progress_sleep, formatDuration(it.toInt()), stringResource(R.string.progress_sleep_hint), ProgressMetric.SLEEP) },
        cards.restingHeartRate?.let { card ->
            MiniTile(
                R.string.progress_rhr,
                stringResource(R.string.value_bpm, formatNumber(card.current)),
                card.change?.let { stringResource(R.string.progress_change, formatNumber(it, signed = true), stringResource(state.range.inRangeRes())) }.orEmpty(),
                ProgressMetric.RESTING_HEART_RATE,
            )
        },
        cards.hrvMs?.let { MiniTile(R.string.progress_hrv, stringResource(R.string.value_ms, formatNumber(it)), stringResource(R.string.progress_sleep_hint), ProgressMetric.HEART_RATE_VARIABILITY) },
    )
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { RangeSwitch(state.range, onRangeSelected) }
        item { HealthConnectCard(onConnectionChanged = onHealthConnectChanged) }
        cards.weight?.let { item { WeightCard(it, state.range) { onOpenMetric(ProgressMetric.WEIGHT) } } }
        cards.balance?.let { item { BalanceCard(it) { onOpenMetric(ProgressMetric.CALORIE_BALANCE) } } }
        cards.macros?.let { item { MacrosCard(it) } }
        items(tiles.chunked(2).size) { index ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                val pair = tiles.chunked(2)[index]
                pair.forEach { tile -> MiniTileCard(tile, Modifier.weight(1f)) { onOpenMetric(tile.metric) } }
                if (pair.size == 1) Box(Modifier.weight(1f))
            }
        }
        if (state.strength.isNotEmpty()) {
            item { StrengthCard(state.strength, cards.workouts, state.range, onOpenExercise) }
        } else {
            cards.workouts?.let { item { WorkoutsCard(it, state.range) } }
        }
        if (state.volumeRows.isNotEmpty()) item { VolumeCard(state.volumeRows, state.showTargets, onOpenTargets) }
        if (cards.recentDays.isNotEmpty()) item { DaysCard(cards.recentDays, onOpenDay) }
    }
}

@Composable
internal fun RangeSwitch(selected: ProgressRange, onSelected: (ProgressRange) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().height(40.dp)) {
        ProgressRange.entries.forEachIndexed { index, range ->
            SegmentedButton(
                selected = range == selected,
                onClick = { onSelected(range) },
                shape = SegmentedButtonDefaults.itemShape(index, ProgressRange.entries.size),
                colors = SegmentedButtonDefaults.colors(inactiveBorderColor = MaterialTheme.colorScheme.outline, activeBorderColor = MaterialTheme.colorScheme.outline),
                icon = {},
                modifier = Modifier.testTag(ProgressTags.range(range)),
            ) { Text(stringResource(range.labelRes())) }
        }
    }
}

@Composable
internal fun ProgressCard(
    @StringRes title: Int,
    modifier: Modifier = Modifier,
    onDetails: (() -> Unit)? = null,
    content: @Composable () -> Unit,
) {
    Card(modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth().heightIn(min = 20.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                CardLabel(stringResource(title), Modifier.weight(1f))
                onDetails?.let {
                    Text(
                        stringResource(R.string.progress_details),
                        Modifier.clickable(onClick = it).padding(vertical = 2.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            content()
        }
    }
}

@Composable
private fun BigValue(value: String, detail: (@Composable () -> Unit)? = null) {
    Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(value, style = MaterialTheme.typography.headlineMedium.copy(fontSize = 28.sp))
        detail?.let { Box(Modifier.padding(bottom = 4.dp)) { it() } }
    }
}

@Composable
private fun SmallDetail(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
internal fun ChartLegend(vararg items: Pair<Color, String>) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
        items.forEach { (color, text) ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(Modifier.size(10.dp).background(color, CircleShape))
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun WeightCard(card: TrendCard, range: ProgressRange, onDetails: () -> Unit) {
    ProgressCard(R.string.progress_weight_title, Modifier.testTag(ProgressTags.WEIGHT_CARD), onDetails) {
        BigValue(stringResource(R.string.value_kg, formatNumber(card.current, 1))) {
            card.change?.let { change ->
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        stringResource(R.string.value_kg, formatNumber(change, 1, signed = true)),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = extendedColors.success,
                    )
                    SmallDetail(stringResource(range.inRangeRes()))
                }
            }
        }
        LineChart(card.points, card.average)
        ChartLegend(
            MaterialTheme.colorScheme.inversePrimary to stringResource(R.string.progress_legend_daily),
            MaterialTheme.colorScheme.primary to stringResource(R.string.progress_legend_average),
        )
    }
}

@Composable
private fun BalanceCard(card: BalanceCard, onDetails: () -> Unit) {
    ProgressCard(R.string.progress_balance_title, Modifier.testTag(ProgressTags.BALANCE_CARD), onDetails) {
        BigValue(stringResource(R.string.value_kcal, formatNumber(card.averageBalanceKcal.toDouble(), signed = true))) {
            SmallDetail(
                stringResource(
                    R.string.progress_balance_detail,
                    formatNumber(card.averageEatenKcal.toDouble()),
                    formatNumber(card.averageBurnedKcal.toDouble()),
                ),
            )
        }
        BarChart(card.weeks.map { it.balanceKcal })
        Text(stringResource(R.string.progress_balance_weeks), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun MacrosCard(card: MacrosCard) {
    ProgressCard(R.string.progress_macros_title, Modifier.testTag(ProgressTags.MACROS_CARD)) {
        val macros = listOf(
            Triple(R.string.macro_protein, card.proteinG, MaterialTheme.colorScheme.primary),
            Triple(R.string.macro_carbs, card.carbsG, MaterialTheme.colorScheme.tertiary),
            Triple(R.string.macro_fat, card.fatG, extendedColors.warning),
            Triple(R.string.macro_fiber, card.fiberG, MaterialTheme.colorScheme.outline),
        )
        val scale = (macros.mapNotNull { it.second }.maxOrNull() ?: 1.0) * MACRO_SCALE_MARGIN
        macros.forEach { (label, grams, color) ->
            grams?.let { MacroRow(stringResource(label), it, (it / scale).toFloat(), color) }
        }
    }
}

private const val MACRO_SCALE_MARGIN = 1.2

@Composable
private fun MacroRow(label: String, grams: Double, fraction: Float, color: Color) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(90.dp), style = MaterialTheme.typography.bodyMedium)
        Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.surfaceVariant)) {
            Box(Modifier.fillMaxWidth(fraction).height(8.dp).clip(RoundedCornerShape(4.dp)).background(color))
        }
        Text(stringResource(R.string.value_grams, formatNumber(grams)), Modifier.width(64.dp), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.End)
    }
}

private data class MiniTile(@StringRes val title: Int, val value: String, val hint: String, val metric: ProgressMetric)

@Composable
private fun MiniTileCard(tile: MiniTile, modifier: Modifier, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = modifier.testTag(ProgressTags.card(tile.title))) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            CardLabel(stringResource(tile.title))
            Text(tile.value, style = MaterialTheme.typography.titleLarge.copy(fontSize = 20.sp, fontWeight = FontWeight.Medium))
            if (tile.hint.isNotEmpty()) Text(tile.hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun StrengthCard(rows: List<StrengthRow>, workouts: WorkoutsCard?, range: ProgressRange, onOpenExercise: (String) -> Unit) {
    val locale = LocalConfiguration.current.locales[0]
    ProgressCard(R.string.progress_strength_title, Modifier.testTag(ProgressTags.STRENGTH_CARD)) {
        Column {
            rows.forEach { row ->
                Row(
                    Modifier.fillMaxWidth().clickable { onOpenExercise(row.exercise.key) }.padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(row.exercise.displayName(locale), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp))
                        if (row.isRecord) {
                            Tag(stringResource(R.string.progress_record), containerColor = extendedColors.warningContainer, contentColor = extendedColors.onWarningContainer)
                        }
                    }
                    Text(
                        stringResource(R.string.value_kg, formatNumber(row.estimatedOneRepMaxKg)),
                        style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium),
                    )
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
            }
        }
        workouts?.let { WorkoutsLine(it, range) }
    }
}

@Composable
private fun WorkoutsLine(card: WorkoutsCard, range: ProgressRange) {
    Text(
        stringResource(R.string.progress_workouts_line, stringResource(R.string.progress_workouts_summary, card.count, stringResource(range.inRangeRes()), formatNumber(card.perWeek, 1))),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun WorkoutsCard(card: WorkoutsCard, range: ProgressRange) {
    ProgressCard(R.string.progress_workouts, Modifier.testTag(ProgressTags.WORKOUTS_CARD)) {
        Text(
            stringResource(
                R.string.progress_workouts_summary,
                card.count,
                stringResource(range.inRangeRes()),
                formatNumber(card.perWeek, 1),
            ),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun DaysCard(days: List<HealthDay>, onOpenDay: (LocalDate) -> Unit) {
    ProgressCard(R.string.progress_days, Modifier.testTag(ProgressTags.DAYS_CARD)) {
        Column {
            days.forEachIndexed { index, day -> DayRow(day, divider = index < days.lastIndex) { onOpenDay(day.date) } }
        }
    }
}

@Composable
private fun DayRow(day: HealthDay, divider: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(formatWeekdayDate(day.date), Modifier.width(72.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(day.summary(), Modifier.weight(1f).testTag(ProgressTags.healthDay(day.date)), style = MaterialTheme.typography.bodyMedium)
    }
    if (divider) HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)
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
    const val STRENGTH_CARD = "progress_strength_card"
    const val VOLUME_CARD = "progress_volume_card"
    const val VOLUME_TARGETS_LINK = "progress_volume_targets_link"
    const val WORKOUTS_CARD = "progress_workouts_card"
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
                state = ProgressUiState(cards = ProgressCalculator.calculate(days, 14, ProgressRange.FOUR_WEEKS, today)),
                onRangeSelected = {},
                onHealthConnectChanged = {},
                onOpenMetric = {},
                onOpenDay = {},
                onOpenExercise = {},
                onOpenTargets = {},
            )
        }
    }
}
