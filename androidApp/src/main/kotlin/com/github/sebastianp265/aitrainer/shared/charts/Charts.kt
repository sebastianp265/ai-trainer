package com.github.sebastianp265.aitrainer.shared.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.charts.impl.ChartScale
import com.github.sebastianp265.aitrainer.shared.charts.impl.chartScale
import com.github.sebastianp265.aitrainer.shared.localization.formatNumber
import com.github.sebastianp265.aitrainer.shared.localization.rememberDayMonthFormatter
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.Axis
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLabelComponent
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberAxisLineComponent
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.marker.CartesianMarkerController
import com.patrykandpatrick.vico.compose.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.Insets
import com.patrykandpatrick.vico.compose.common.component.ShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.common.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.common.data.ExtraStore
import kotlinx.datetime.LocalDate
import kotlin.math.abs
import kotlin.math.ceil

private const val X_LABEL_COUNT = 3

internal data class DailyPoint(val date: LocalDate, val value: Double)

@Composable
internal fun LineChart(
    points: List<DailyPoint>,
    average: List<DailyPoint>,
    modifier: Modifier = Modifier,
    valueLabel: (@Composable (Double) -> String)? = null,
) {
    val colors = MaterialTheme.colorScheme
    val scale = remember(points, average) { (points + average).let { all -> chartScale(all.minOf { it.value }, all.maxOf { it.value }) } }
    val model = remember(points, average) {
        CartesianChartModel(
            LineCartesianLayerModel.build {
                if (average.isNotEmpty()) series(x = average.map { it.date.toEpochDays() }, y = average.map { it.value })
                series(x = points.map { it.date.toEpochDays() }, y = points.map { it.value })
            },
        )
    }
    val dots = LineCartesianLayer.rememberLine(
        fill = remember { LineCartesianLayer.LineFill.single(Fill.Transparent) },
        pointProvider = remember(colors.inversePrimary) {
            LineCartesianLayer.PointProvider.single(LineCartesianLayer.Point(ShapeComponent(Fill(colors.inversePrimary), CircleShape), 6.dp))
        },
    )
    val averageLine = LineCartesianLayer.rememberLine(
        fill = remember(colors.primary) { LineCartesianLayer.LineFill.single(Fill(colors.primary)) },
        stroke = remember { LineCartesianLayer.LineStroke.Continuous(thickness = 3.dp) },
    )
    val layer = rememberLineCartesianLayer(
        lineProvider = remember(dots, averageLine, average.isEmpty()) {
            LineCartesianLayer.LineProvider.series(if (average.isEmpty()) listOf(dots) else listOf(averageLine, dots))
        },
        rangeProvider = remember(scale) { scale.rangeProvider() },
    )
    val chart = if (valueLabel == null) {
        rememberCartesianChart(layer, getXStep = { _, _, _ -> 1.0 })
    } else {
        val days = (points + average).map { it.date.toEpochDays() }
        val labelSpacing = ceil((days.max() - days.min()).toDouble() / X_LABEL_COUNT).toInt().coerceAtLeast(1)
        rememberCartesianChart(
            layer,
            startAxis = rememberValueAxis(scale, valueLabel),
            bottomAxis = rememberDayAxis(labelSpacing),
            marker = rememberPointMarker(points, average, valueLabel),
            getXStep = { _, _, _ -> 1.0 },
            markerController = CartesianMarkerController.rememberToggleOnTap(),
        )
    }
    CartesianChartHost(
        chart = chart,
        model = model,
        modifier = modifier.fillMaxWidth().height(if (valueLabel == null) 160.dp else 200.dp),
        scrollState = rememberVicoScrollState(scrollEnabled = false),
        zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
    )
}

private fun ChartScale.rangeProvider() = object : CartesianLayerRangeProvider {
    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore) = min

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore) = max
}

@Composable
private fun rememberAxisLabel() = rememberAxisLabelComponent(
    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
)

@Composable
private fun rememberValueAxis(scale: ChartScale, valueLabel: @Composable (Double) -> String): VerticalAxis<Axis.Position.Vertical.Start> {
    val tickLabels = scale.ticks.associateWith { valueLabel(it) }
    return VerticalAxis.rememberStart(
        line = null,
        label = rememberAxisLabel(),
        tick = null,
        guideline = rememberAxisGuidelineComponent(fill = Fill(MaterialTheme.colorScheme.outlineVariant)),
        itemPlacer = remember(scale) { VerticalAxis.ItemPlacer.step({ scale.step }) },
        valueFormatter = remember(tickLabels) {
            CartesianValueFormatter { _, value, _ -> tickLabels.minBy { abs(it.key - value) }.value }
        },
    )
}

@Composable
private fun rememberDayAxis(labelSpacing: Int): HorizontalAxis<Axis.Position.Horizontal.Bottom> {
    val dayLabel = rememberDayMonthFormatter()
    return HorizontalAxis.rememberBottom(
        line = rememberAxisLineComponent(fill = Fill(MaterialTheme.colorScheme.outlineVariant)),
        label = rememberAxisLabel(),
        tick = null,
        guideline = null,
        itemPlacer = remember(labelSpacing) { HorizontalAxis.ItemPlacer.aligned(spacing = { labelSpacing }) },
        valueFormatter = remember(dayLabel) { CartesianValueFormatter { _, x, _ -> dayLabel(LocalDate.fromEpochDays(x.toLong())) } },
    )
}

@Composable
private fun rememberPointMarker(points: List<DailyPoint>, average: List<DailyPoint>, valueLabel: @Composable (Double) -> String): DefaultCartesianMarker {
    val colors = MaterialTheme.colorScheme
    val dayLabel = rememberDayMonthFormatter()
    val labels = (average + points).associate { it.date.toEpochDays() to "${dayLabel(it.date)} · ${valueLabel(it.value)}" }
    return rememberDefaultCartesianMarker(
        label = rememberTextComponent(
            style = MaterialTheme.typography.labelMedium.copy(color = colors.onSurface),
            padding = Insets(horizontal = 8.dp, vertical = 4.dp),
            background = rememberShapeComponent(Fill(colors.surfaceContainerHighest), RoundedCornerShape(8.dp)),
        ),
        valueFormatter = remember(labels) { DefaultCartesianMarker.ValueFormatter { _, targets -> labels.getValue(targets.first().x.toLong()) } },
        indicator = remember(colors.surface) { { color: Color -> ShapeComponent(Fill(color), CircleShape, strokeFill = Fill(colors.surface), strokeThickness = 2.dp) } },
        guideline = rememberAxisGuidelineComponent(fill = Fill(colors.outline)),
    )
}

@Composable
internal fun BarChart(values: List<Int>, modifier: Modifier = Modifier) {
    val barColor = MaterialTheme.colorScheme.tertiary
    val axisColor = MaterialTheme.colorScheme.outline
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
            drawRoundRect(barColor, Offset(slot * index + (slot - barWidth) / 2, top), Size(barWidth, barHeight.coerceAtLeast(1f)), CornerRadius(4.dp.toPx()))
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
                LineChart(days, days, valueLabel = { stringResource(R.string.value_kg, formatNumber(it, 1)) })
                BarChart(listOf(-200, -320, -320, 150))
            }
        }
    }
}
