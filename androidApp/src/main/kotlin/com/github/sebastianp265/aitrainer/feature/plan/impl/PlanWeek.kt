package com.github.sebastianp265.aitrainer.feature.plan.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.localization.formatLongWeekdayDate
import com.github.sebastianp265.aitrainer.shared.localization.formatWeekRange
import com.github.sebastianp265.aitrainer.shared.localization.formatWeekdayShort
import kotlinx.datetime.LocalDate

internal data class WeekDay(val date: LocalDate, val isToday: Boolean, val done: Boolean, val planned: Boolean)

internal data class WeekUi(val days: List<WeekDay>, val doneCount: Int, val plannedCount: Int)

@Composable
internal fun TrainingWeekContent(state: TrainingWeekUiState) {
    val week = state.week ?: return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        WeekHeader(week)
        WeekStrip(week)
    }
}

@Composable
internal fun WeekHeader(week: WeekUi) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.week_title, formatWeekRange(week.days.first().date, week.days.last().date)),
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            stringResource(
                R.string.week_counts,
                pluralStringResource(R.plurals.week_done_count, week.doneCount, week.doneCount),
                pluralStringResource(R.plurals.week_planned_count, week.plannedCount, week.plannedCount),
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun WeekStrip(week: WeekUi) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().testTag(PlanTags.WEEK_STRIP), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            week.days.forEach { DayCell(it, Modifier.weight(1f)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            LegendMarker(MarkerKind.DONE, stringResource(R.string.week_legend_done))
            LegendMarker(MarkerKind.PLANNED, stringResource(R.string.week_legend_planned))
        }
    }
}

private enum class MarkerKind { DONE, PLANNED }

@Composable
private fun DayCell(day: WeekDay, modifier: Modifier) {
    val description = dayDescription(day)
    Surface(
        modifier = modifier.semantics { contentDescription = description }.testTag(PlanTags.weekDay(day.date)),
        shape = RoundedCornerShape(12.dp),
        color = if (day.isToday) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(formatWeekdayShort(day.date), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(day.date.day.toString(), style = MaterialTheme.typography.titleMedium.copy(fontSize = 16.sp))
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp, Alignment.CenterHorizontally), modifier = Modifier.height(10.dp)) {
                if (day.done) Marker(MarkerKind.DONE)
                if (day.planned) Marker(MarkerKind.PLANNED)
            }
        }
    }
}

@Composable
private fun dayDescription(day: WeekDay): String {
    val parts = buildList {
        add(formatLongWeekdayDate(day.date))
        if (day.isToday) add(stringResource(R.string.week_today))
        if (day.done) add(stringResource(R.string.week_legend_done))
        if (day.planned) add(stringResource(R.string.week_legend_planned))
        if (!day.done && !day.planned) add(stringResource(R.string.week_free))
    }
    return parts.joinToString(", ")
}

@Composable
private fun Marker(kind: MarkerKind) {
    val color = markerColor(kind)
    val modifier = Modifier.size(10.dp)
    when (kind) {
        MarkerKind.DONE -> Box(modifier.background(color, CircleShape))
        MarkerKind.PLANNED -> Box(modifier.border(2.dp, color, CircleShape))
    }
}

@Composable
private fun markerColor(kind: MarkerKind): Color = when (kind) {
    MarkerKind.DONE -> MaterialTheme.colorScheme.primary
    MarkerKind.PLANNED -> MaterialTheme.colorScheme.primary
}

@Composable
private fun LegendMarker(kind: MarkerKind, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Marker(kind)
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
