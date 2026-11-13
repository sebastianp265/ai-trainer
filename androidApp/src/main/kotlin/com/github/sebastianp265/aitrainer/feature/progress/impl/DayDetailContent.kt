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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.design.CardLabel
import androidx.compose.material3.Card
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import com.github.sebastianp265.aitrainer.integration.workout.SessionSummary
import com.github.sebastianp265.aitrainer.shared.localization.formatDateLong
import com.github.sebastianp265.aitrainer.shared.localization.formatNumber
import kotlinx.datetime.LocalDate

@Composable
internal fun DayDetailContent(state: DayDetailUiState?) {
    if (state == null) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }
    val day = state.day
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize().testTag(DayDetailTags.DAY),
    ) {
        item {
            Column(Modifier.padding(vertical = 4.dp)) {
                Text(formatDateLong(state.date), style = MaterialTheme.typography.titleLarge)
                Text(stringResource(R.string.day_detail_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        val calories = day?.calorieRows().orEmpty()
        if (calories.isNotEmpty()) section(R.string.day_section_calories, calories)
        val nutrition = day?.nutritionRows().orEmpty()
        if (nutrition.isNotEmpty()) section(R.string.hc_type_nutrition, nutrition)
        val body = day?.bodyRows().orEmpty()
        if (body.isNotEmpty()) section(R.string.day_section_body, body)
        val activity = day?.stepsRow().orEmpty()
        if (activity.isNotEmpty() || state.gymSessions.isNotEmpty()) {
            item {
                DetailCard(R.string.day_section_activity) {
                    activity.forEach { DetailRow(it) }
                    state.gymSessions.forEach { session -> GymRow(session) }
                }
            }
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.section(@StringRes title: Int, rows: List<DetailLine>) {
    item { DetailCard(title) { rows.forEach { DetailRow(it) } } }
}

internal data class DetailLine(@StringRes val label: Int, val value: @Composable () -> String, val indented: Boolean = false)

private fun HealthDay.calorieRows(): List<DetailLine> = listOfNotNull(
    kcalEaten?.let { DetailLine(R.string.label_eaten, { kcal(it) }) },
    kcalBurned?.let { DetailLine(R.string.label_burned, { kcal(it) }) },
    if (kcalEaten != null && kcalBurned != null) DetailLine(R.string.label_balance, { kcal(kcalEaten!! - kcalBurned!!, signed = true) }) else null,
)

private fun HealthDay.nutritionRows(): List<DetailLine> = listOfNotNull(
    proteinG?.let { DetailLine(R.string.macro_protein, { grams(it) }) },
    carbsG?.let { DetailLine(R.string.macro_carbs, { grams(it) }) },
    sugarG?.let { DetailLine(R.string.label_sugars, { grams(it) }, indented = true) },
    fatG?.let { DetailLine(R.string.macro_fat, { grams(it) }) },
    saturatedFatG?.let { DetailLine(R.string.label_saturated, { grams(it) }, indented = true) },
    fiberG?.let { DetailLine(R.string.macro_fiber, { grams(it) }) },
    sodiumMg?.let { DetailLine(R.string.label_sodium, { stringResource(R.string.value_grams_decimal, formatNumber(it / MG_PER_G, 1)) }) },
)

private fun HealthDay.bodyRows(): List<DetailLine> = listOfNotNull(
    weightKg?.let { DetailLine(R.string.hc_type_weight, { stringResource(R.string.value_kg, formatNumber(it, 1)) }) },
    sleepMinutes?.let { DetailLine(R.string.hc_type_sleep, { formatDuration(it) }) },
    restingHeartRate?.let { DetailLine(R.string.hc_type_resting_heart_rate, { stringResource(R.string.value_bpm, formatNumber(it)) }) },
    hrvMs?.let { DetailLine(R.string.hc_type_hrv, { stringResource(R.string.value_ms, formatNumber(it)) }) },
)

private fun HealthDay.stepsRow(): List<DetailLine> = listOfNotNull(
    steps?.let { DetailLine(R.string.progress_steps, { formatNumber(it.toDouble()) }) },
)

@Composable
private fun kcal(value: Int, signed: Boolean = false) =
    stringResource(R.string.value_kcal, formatNumber(value.toDouble(), 0, signed))

@Composable
private fun grams(value: Double) = stringResource(R.string.value_grams, formatNumber(value))

@Composable
private fun DetailCard(@StringRes title: Int, content: @Composable () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CardLabel(stringResource(title))
            content()
        }
    }
}

@Composable
private fun DetailRow(row: DetailLine) {
    val size = if (row.indented) 14.sp else 15.sp
    val color = if (row.indented) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
    Row(Modifier.fillMaxWidth().padding(start = if (row.indented) 14.dp else 0.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(stringResource(row.label), style = MaterialTheme.typography.bodyLarge.copy(fontSize = size), color = color)
        Text(
            row.value(),
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = size, fontWeight = if (row.indented) FontWeight.Normal else FontWeight.Medium),
            color = color,
        )
    }
}

@Composable
private fun GymRow(session: SessionSummary) {
    Column {
        Text(stringResource(R.string.day_gym_workout), style = MaterialTheme.typography.bodyMedium)
        Text(
            stringResource(
                R.string.session_summary,
                pluralStringResource(R.plurals.exercise_count, session.exerciseCount, session.exerciseCount),
                pluralStringResource(R.plurals.set_count, session.setCount, session.setCount),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

internal object DayDetailTags {
    const val DAY = "day_detail"
}

private const val MG_PER_G = 1000.0

@PreviewLightDark
@Composable
private fun DayDetailContentPreview() {
    MaterialTheme {
        Surface {
            DayDetailContent(
                DayDetailUiState(
                    date = LocalDate(2026, 11, 8),
                    day = HealthDay(
                        LocalDate(2026, 11, 8), 2410, 172.0, 240.0, 58.0, 72.0, 21.0, 26.0, 2100.0,
                        2580, 82.5, 9120, 460, 53.0, 64.0,
                    ),
                    gymSessions = emptyList(),
                ),
            )
        }
    }
}
