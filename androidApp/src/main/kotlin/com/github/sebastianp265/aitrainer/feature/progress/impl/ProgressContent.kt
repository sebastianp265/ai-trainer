package com.github.sebastianp265.aitrainer.feature.progress.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
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
import com.github.sebastianp265.aitrainer.shared.localization.formatDate
import com.github.sebastianp265.aitrainer.feature.health.HealthConnectCard
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import kotlinx.datetime.LocalDate

@Composable
internal fun ProgressContent(state: ProgressUiState, onHealthConnectChanged: () -> Unit) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { HealthConnectCard(onConnectionChanged = onHealthConnectChanged) }
        if (state.healthDays.isNotEmpty()) healthDaysSection(state.healthDays)
    }
}

internal fun LazyListScope.healthDaysSection(days: List<HealthDay>) {
    item { HealthDaysHeader() }
    items(days, key = { it.date.toString() }) { day -> HealthDayItem(day) }
}

@Composable
internal fun HealthDaysHeader() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(stringResource(R.string.health_data_title), style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
internal fun HealthDayItem(day: HealthDay) {
    ListItem(
        headlineContent = { Text(formatDate(day.date)) },
        supportingContent = {
            Column {
                Text(day.summary(), Modifier.testTag(ProgressTags.healthDay(day.date)))
            }
        },
    )
}

@Composable
private fun HealthDay.summary(): String = stringResource(
    R.string.health_data_day,
    kcal(kcalEaten),
    grams(proteinG),
    grams(carbsG),
    grams(fatG),
)

@Composable
private fun kcal(value: Int?): String =
    value?.let { stringResource(R.string.value_kcal, formatKcal(it)) } ?: stringResource(R.string.value_missing)

@Composable
private fun grams(value: Double?): String =
    value?.let { stringResource(R.string.value_grams, formatGrams(it)) } ?: stringResource(R.string.value_missing)

internal object ProgressTags {
    fun healthDay(date: LocalDate) = "progress_health_day:$date"
}

@PreviewLightDark
@Composable
private fun HealthDayItemPreview() {
    MaterialTheme {
        Surface {
            HealthDayItem(
                HealthDay(
                    date = LocalDate(2026, 1, 5),
                    kcalEaten = 2350,
                    proteinG = 160.0,
                    carbsG = 260.5,
                    sugarG = null,
                    fatG = 70.0,
                    saturatedFatG = null,
                    fiberG = null,
                    sodiumMg = null,
                    kcalBurned = null,
                    weightKg = null,
                    steps = null,
                    sleepMinutes = null,
                    restingHeartRate = null,
                    hrvMs = null,
                ),
            )
        }
    }
}
