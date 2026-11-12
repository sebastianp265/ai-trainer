package com.github.sebastianp265.aitrainer.shared.volume

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import com.github.sebastianp265.aitrainer.shared.design.extendedColors
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.integration.exercise.MuscleGroup
import com.github.sebastianp265.aitrainer.shared.localization.formatWeight

private const val SCALE_MARGIN = 1.25f

@Composable
internal fun VolumeBars(rows: List<VolumeRow>, showTargets: Boolean, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        rows.forEach { VolumeBar(it, showTargets) }
    }
}

@Composable
private fun VolumeBar(row: VolumeRow, showTargets: Boolean) {
    val locale = LocalConfiguration.current.locales[0]
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val bandColor = MaterialTheme.colorScheme.inversePrimary
    val fillColor = if (!showTargets || row.status() == TargetStatus.IN_TARGET) MaterialTheme.colorScheme.primary else extendedColors.warning
    val scale = maxOf(row.maxSets * SCALE_MARGIN, row.done.toFloat() * 1.05f, 1f)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 28.dp).testTag("volume_row:${row.group.key}"),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(row.group.displayName(locale), Modifier.width(70.dp), style = MaterialTheme.typography.bodyMedium)
        Canvas(Modifier.weight(1f).height(14.dp).clip(RoundedCornerShape(7.dp))) {
            drawRect(trackColor, size = size)
            if (showTargets) {
                drawRect(bandColor, Offset(size.width * row.minSets / scale, 0f), Size(size.width * (row.maxSets - row.minSets) / scale, size.height))
            }
            val barTop = 4.dp.toPx()
            val barHeight = 6.dp.toPx()
            val doneWidth = size.width * row.done.toFloat() / scale
            drawRoundRect(fillColor, Offset(0f, barTop), Size(doneWidth, barHeight), CornerRadius(barHeight / 2))
        }
        Row(Modifier.widthIn(min = 100.dp), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Bottom) {
            Text(formatWeight(row.done), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium))
            if (showTargets) {
                Text(
                    stringResource(R.string.volume_target, row.minSets, row.maxSets),
                    Modifier.padding(start = 4.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
internal fun VolumeLegend() {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        LegendItem(MaterialTheme.colorScheme.inversePrimary, stringResource(R.string.volume_legend_target))
        LegendItem(MaterialTheme.colorScheme.primary, stringResource(R.string.volume_legend_sets))
        LegendItem(extendedColors.warning, stringResource(R.string.volume_legend_off_target))
    }
}

@Composable
private fun LegendItem(color: Color, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(Modifier.size(10.dp).background(color, CircleShape))
        Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@PreviewLightDark
@Composable
private fun VolumeBarsPreview() {
    fun group(key: String) = MuscleGroup(key, mapOf("en" to key.lowercase().replaceFirstChar { it.uppercase() }))
    MaterialTheme {
        Column {
            VolumeBars(
                rows = listOf(
                    VolumeRow(group("BACK"), 16.5, 12, 18),
                    VolumeRow(group("CHEST"), 13.0, 10, 16),
                    VolumeRow(group("ABS"), 2.0, 6, 10),
                ),
                showTargets = true,
            )
            VolumeLegend()
        }
    }
}
