package com.github.sebastianp265.aitrainer.feature.history.impl

import com.github.sebastianp265.aitrainer.integration.coach.TopicKind
import com.github.sebastianp265.aitrainer.integration.coach.CoachTopic
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
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
import com.github.sebastianp265.aitrainer.shared.design.CoachButton
import com.github.sebastianp265.aitrainer.shared.design.InfoNote
import com.github.sebastianp265.aitrainer.shared.design.StatTile
import com.github.sebastianp265.aitrainer.shared.state.LocalCoachEnabled
import com.github.sebastianp265.aitrainer.shared.state.LocalOpenCoach
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
    val stats = listOfNotNull(
        stringResource(R.string.value_min, workout.durationMinutes.toString()) to stringResource(R.string.stat_duration),
        workout.distanceM?.let { stringResource(R.string.value_km, formatNumber(it / METERS_PER_KM, 1)) to stringResource(R.string.stat_distance) },
        workout.kcal?.let { stringResource(R.string.value_kcal, formatNumber(it.toDouble())) to stringResource(R.string.stat_burned) },
        workout.averageHeartRate?.let { stringResource(R.string.value_bpm, formatNumber(it.toDouble())) to stringResource(R.string.stat_heart_rate) },
    )
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .testTag(HistoryTags.EXTERNAL_WORKOUT),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.heightIn(min = 56.dp), verticalArrangement = Arrangement.Center) {
            Text(externalWorkoutTitle(workout), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(
                    R.string.external_from_health_connect,
                    formatDateLong(workout.startedAt.toLocalDateTime(zone).date),
                    formatTime(workout.startedAt),
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        stats.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (value, label) -> StatTile(value, label, Modifier.weight(1f)) }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        InfoNote(stringResource(R.string.external_read_only_title), stringResource(R.string.external_read_only_text))
        Text(stringResource(R.string.external_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (LocalCoachEnabled.current) {
            val openCoach = LocalOpenCoach.current
            val topic = CoachTopic(TopicKind.EXTERNAL_WORKOUT, workout.startedAt.toString(), externalWorkoutTitle(workout))
            CoachButton(stringResource(R.string.detail_ask_coach), { openCoach(topic) })
        }
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
