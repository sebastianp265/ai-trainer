package com.github.sebastianp265.aitrainer.feature.plan.impl

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.shared.localization.formatLongWeekdayDate
import com.github.sebastianp265.aitrainer.shared.localization.formatWeekRange
import com.github.sebastianp265.aitrainer.shared.localization.formatWeekdayShort
import com.github.sebastianp265.aitrainer.shared.localization.formatWeight
import com.github.sebastianp265.aitrainer.shared.volume.TargetStatus
import com.github.sebastianp265.aitrainer.shared.volume.VolumeBars
import com.github.sebastianp265.aitrainer.shared.volume.VolumeLegend
import com.github.sebastianp265.aitrainer.shared.volume.VolumeRow
import com.github.sebastianp265.aitrainer.shared.volume.status
import kotlinx.datetime.LocalDate

internal data class WeekDay(val date: LocalDate, val isToday: Boolean, val done: Boolean, val planned: Boolean, val external: Boolean)

internal data class WeekUi(val days: List<WeekDay>, val doneCount: Int, val plannedCount: Int)

@Composable
internal fun WeekHeader(week: WeekUi) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.week_title, formatWeekRange(week.days.first().date, week.days.last().date)),
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            stringResource(
                R.string.week_counts,
                pluralStringResource(R.plurals.week_done_count, week.doneCount, week.doneCount),
                pluralStringResource(R.plurals.week_planned_count, week.plannedCount, week.plannedCount),
            ),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
internal fun WeekStrip(week: WeekUi) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth().testTag(PlanTags.WEEK_STRIP), horizontalArrangement = Arrangement.SpaceBetween) {
            week.days.forEach { DayCell(it, Modifier.weight(1f)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendMarker(MarkerKind.DONE, stringResource(R.string.week_legend_done))
            LegendMarker(MarkerKind.PLANNED, stringResource(R.string.week_legend_planned))
            LegendMarker(MarkerKind.EXTERNAL, stringResource(R.string.week_legend_external))
        }
    }
}

private enum class MarkerKind { DONE, PLANNED, EXTERNAL }

@Composable
private fun DayCell(day: WeekDay, modifier: Modifier) {
    val description = dayDescription(day)
    Surface(
        modifier = modifier.padding(horizontal = 2.dp).semantics { contentDescription = description }.testTag(PlanTags.weekDay(day.date)),
        shape = MaterialTheme.shapes.medium,
        color = if (day.isToday) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface,
    ) {
        Column(Modifier.padding(vertical = 8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(formatWeekdayShort(day.date), style = MaterialTheme.typography.labelSmall)
            Text(day.date.day.toString(), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), modifier = Modifier.size(width = 30.dp, height = 10.dp)) {
                if (day.done) Marker(MarkerKind.DONE)
                if (day.planned) Marker(MarkerKind.PLANNED)
                if (day.external) Marker(MarkerKind.EXTERNAL)
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
        if (day.external) add(stringResource(R.string.week_legend_external))
        if (!day.done && !day.planned && !day.external) add(stringResource(R.string.week_free))
    }
    return parts.joinToString(", ")
}

@Composable
private fun Marker(kind: MarkerKind) {
    val color = markerColor(kind)
    val modifier = Modifier.size(8.dp)
    when (kind) {
        MarkerKind.DONE -> Box(modifier.background(color, CircleShape))
        MarkerKind.PLANNED -> Box(modifier.drawBehind { drawCircle(color, style = Stroke(width = 1.5.dp.toPx())) })
        MarkerKind.EXTERNAL -> Box(modifier.drawBehind { drawLine(color, Offset(0f, size.height / 2), Offset(size.width, size.height / 2), strokeWidth = 3.dp.toPx()) })
    }
}

@Composable
private fun markerColor(kind: MarkerKind): Color = when (kind) {
    MarkerKind.DONE -> MaterialTheme.colorScheme.primary
    MarkerKind.PLANNED -> MaterialTheme.colorScheme.primary
    MarkerKind.EXTERNAL -> MaterialTheme.colorScheme.tertiary
}

@Composable
private fun LegendMarker(kind: MarkerKind, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Marker(kind)
        Text(text, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
internal fun WeekVolumeCard(rows: List<VolumeRow>, showTargets: Boolean, onOpenTargets: () -> Unit) {
    Card(Modifier.fillMaxWidth().testTag(PlanTags.WEEK_VOLUME)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(stringResource(R.string.week_volume_title), Modifier.weight(1f), style = MaterialTheme.typography.labelLarge)
                TextButton(onClick = onOpenTargets, Modifier.testTag(PlanTags.WEEK_TARGETS_LINK)) { Text(stringResource(R.string.volume_targets_link)) }
            }
            VolumeBars(rows, showTargets)
            if (showTargets) {
                VolumeLegend(showPlanned = true)
                val below = rows.filter { it.status() == TargetStatus.BELOW }
                if (below.isNotEmpty()) Text(belowText(below), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun belowText(rows: List<VolumeRow>): String {
    val locale = LocalConfiguration.current.locales[0]
    val items = rows.map { row ->
        stringResource(R.string.week_volume_below_item, row.group.displayName(locale), formatWeight(row.total), row.minSets, row.maxSets)
    }
    return stringResource(R.string.week_volume_below, items.joinToString(", "))
}
