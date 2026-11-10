package com.github.sebastianp265.aitrainer.feature.progress.impl

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDate
import kotlin.math.abs

@Composable
internal fun LineChart(points: List<DailyPoint>, average: List<DailyPoint>, modifier: Modifier = Modifier) {
    val pointColor = MaterialTheme.colorScheme.outline
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier.fillMaxWidth().height(160.dp)) {
        val all = points + average
        val firstDay = all.minOf { it.date.toEpochDays() }
        val lastDay = all.maxOf { it.date.toEpochDays() }
        val minValue = all.minOf { it.value }
        val maxValue = all.maxOf { it.value }
        val padding = 8.dp.toPx()
        fun x(date: LocalDate): Float {
            val span = (lastDay - firstDay).coerceAtLeast(1)
            return padding + (size.width - 2 * padding) * (date.toEpochDays() - firstDay) / span
        }
        fun y(value: Double): Float {
            val span = (maxValue - minValue).takeIf { it > 0 } ?: 1.0
            return (size.height - padding - (size.height - 2 * padding) * ((value - minValue) / span)).toFloat()
        }
        points.forEach { drawCircle(pointColor, radius = 3.dp.toPx(), center = Offset(x(it.date), y(it.value))) }
        val path = Path()
        average.forEachIndexed { index, point ->
            val offset = Offset(x(point.date), y(point.value))
            if (index == 0) path.moveTo(offset.x, offset.y) else path.lineTo(offset.x, offset.y)
        }
        drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
internal fun BarChart(values: List<Int>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outlineVariant
    Canvas(modifier.fillMaxWidth().height(120.dp)) {
        val minimum = minOf(values.min(), 0)
        val maximum = maxOf(values.max(), 0)
        val span = (maximum - minimum).coerceAtLeast(1)
        val zeroY = size.height * maximum / span
        val slot = size.width / values.size
        val barWidth = slot * 0.6f
        values.forEachIndexed { index, value ->
            val barHeight = size.height * abs(value) / span
            val top = if (value >= 0) zeroY - barHeight else zeroY
            drawRect(barColor, Offset(slot * index + (slot - barWidth) / 2, top), Size(barWidth, barHeight.coerceAtLeast(1f)))
        }
        drawLine(axisColor, Offset(0f, zeroY), Offset(size.width, zeroY), strokeWidth = 1.dp.toPx())
    }
}

@PreviewLightDark
@Composable
private fun ChartsPreview() {
    MaterialTheme {
        Surface {
            val days = (0..9).map { DailyPoint(LocalDate(2026, 11, 1 + it), 82.0 + (it % 3) * 0.4 - it * 0.1) }
            Column {
                LineChart(days, days)
                BarChart(listOf(-200, -320, -320, 150))
            }
        }
    }
}
