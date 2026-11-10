package com.github.sebastianp265.aitrainer.feature.history.impl

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
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
import com.github.sebastianp265.aitrainer.integration.health.HealthWorkout
import com.github.sebastianp265.aitrainer.shared.health.externalWorkoutTitle
import com.github.sebastianp265.aitrainer.shared.localization.formatDateLong
import com.github.sebastianp265.aitrainer.shared.localization.formatNumber
import com.github.sebastianp265.aitrainer.shared.localization.formatTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

@Composable
internal fun ExternalWorkoutContent(state: ExternalWorkoutUiState) {
    if (state.isLoading) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }
    val workout = state.workout
    if (workout == null) {
        Text(stringResource(R.string.external_missing), Modifier.padding(16.dp))
        return
    }
    val zone = TimeZone.currentSystemDefault()
    Column(
        Modifier.fillMaxSize().padding(16.dp).testTag(HistoryTags.EXTERNAL_WORKOUT),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(externalWorkoutTitle(workout), style = MaterialTheme.typography.headlineSmall)
        Text(
            stringResource(
                R.string.external_from_health_connect,
                formatDateLong(workout.startedAt.toLocalDateTime(zone).date),
                formatTime(workout.startedAt),
            ),
            style = MaterialTheme.typography.bodyMedium,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat(stringResource(R.string.value_min, workout.durationMinutes.toString()), R.string.stat_duration)
            workout.distanceM?.let { Stat(stringResource(R.string.value_km, formatNumber(it / METERS_PER_KM, 1)), R.string.stat_distance) }
            workout.kcal?.let { Stat(stringResource(R.string.value_kcal, formatNumber(it.toDouble())), R.string.stat_burned) }
            workout.averageHeartRate?.let { Stat(stringResource(R.string.value_bpm, formatNumber(it.toDouble())), R.string.stat_heart_rate) }
        }
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(stringResource(R.string.external_read_only_title), style = MaterialTheme.typography.titleSmall)
                Text(stringResource(R.string.external_read_only_text), style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(stringResource(R.string.external_note), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Stat(value: String, @StringRes label: Int) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(stringResource(label), style = MaterialTheme.typography.bodySmall)
    }
}

private const val METERS_PER_KM = 1000.0

@PreviewLightDark
@Composable
private fun ExternalWorkoutContentPreview() {
    MaterialTheme {
        Surface {
            ExternalWorkoutContent(
                ExternalWorkoutUiState(
                    isLoading = false,
                    workout = HealthWorkout("running", Instant.parse("2026-11-08T07:40:00Z"), 42, 6800.0, 480, 148),
                ),
            )
        }
    }
}
