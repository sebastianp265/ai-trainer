package com.github.sebastianp265.aitrainer.ui.progress

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import com.github.sebastianp265.aitrainer.R
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.domain.stats.DailyWeight
import com.github.sebastianp265.aitrainer.domain.stats.WeeklyBalance
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.Zoom
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.data.lineModel
import com.patrykandpatrick.vico.compose.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.compose.common.Fill
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.floor

private const val WEIGHT_AXIS_PADDING_KG = 0.5

@Composable
fun WeightChart(chartStart: LocalDate, dailyWeights: List<DailyWeight>, movingAverage: List<DailyWeight>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(chartStart, dailyWeights, movingAverage) {
        modelProducer.runTransaction {
            lineModel {
                series(dailyWeights.map { chartStart.daysUntil(it.date) }, dailyWeights.map { it.weightKg })
                series(movingAverage.map { chartStart.daysUntil(it.date) }, movingAverage.map { it.weightKg })
            }
        }
    }
    val allWeights = dailyWeights.map { it.weightKg }
    val rangeProvider = remember(allWeights) {
        CartesianLayerRangeProvider.fixed(
            minY = floor(allWeights.min() - WEIGHT_AXIS_PADDING_KG),
            maxY = ceil(allWeights.max() + WEIGHT_AXIS_PADDING_KG),
        )
    }
    val dailyColor = MaterialTheme.colorScheme.outline
    val averageColor = MaterialTheme.colorScheme.primary
    val lines = LineCartesianLayer.LineProvider.series(
        LineCartesianLayer.rememberLine(
            fill = remember(dailyColor) { LineCartesianLayer.LineFill.single(Fill(dailyColor)) },
            stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 1.dp),
        ),
        LineCartesianLayer.rememberLine(
            fill = remember(averageColor) { LineCartesianLayer.LineFill.single(Fill(averageColor)) },
            stroke = LineCartesianLayer.LineStroke.Continuous(thickness = 3.dp),
        ),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        FullWidthChart(modelProducer, chartStart, layer = { rememberLineCartesianLayer(lineProvider = lines, rangeProvider = rangeProvider) })
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendItem(dailyColor, R.string.progress_daily_weight)
            LegendItem(averageColor, R.string.progress_seven_day_average)
        }
    }
}

@Composable
private fun LegendItem(color: Color, @StringRes label: Int) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(Modifier.size(width = 16.dp, height = 3.dp).background(color))
        Text(stringResource(label), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun DailyBalanceChart(chartStart: LocalDate, balances: List<DailyBalance>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(chartStart, balances) {
        modelProducer.runTransaction {
            columnModel { series(balances.map { chartStart.daysUntil(it.date) }, balances.map { it.balanceKcal }) }
        }
    }
    FullWidthChart(modelProducer, chartStart, layer = { rememberColumnCartesianLayer() })
}

@Composable
fun WeeklyBalanceChart(chartStart: LocalDate, balances: List<WeeklyBalance>) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(chartStart, balances) {
        modelProducer.runTransaction {
            columnModel { series(balances.map { chartStart.daysUntil(it.weekStart) }, balances.map { it.balanceKcal }) }
        }
    }
    FullWidthChart(modelProducer, chartStart, layer = { rememberColumnCartesianLayer() })
}

@Composable
private fun FullWidthChart(
    modelProducer: CartesianChartModelProducer,
    chartStart: LocalDate,
    layer: @Composable () -> com.patrykandpatrick.vico.compose.cartesian.layer.CartesianLayer<*>,
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateLabels = remember(chartStart, locale) {
        val format = DateTimeFormatter.ofPattern("d MMM", locale)
        CartesianValueFormatter { _, daysFromStart, _ ->
            chartStart.plus(DatePeriod(days = daysFromStart.toInt())).toJavaLocalDate().format(format)
        }
    }
    ProvideVicoTheme(rememberM3VicoTheme()) {
        CartesianChartHost(
            chart = rememberCartesianChart(
                layer(),
                startAxis = VerticalAxis.rememberStart(),
                bottomAxis = HorizontalAxis.rememberBottom(valueFormatter = dateLabels),
            ),
            modelProducer = modelProducer,
            scrollState = rememberVicoScrollState(scrollEnabled = false),
            zoomState = rememberVicoZoomState(zoomEnabled = false, initialZoom = Zoom.Content),
            modifier = Modifier.fillMaxWidth().height(200.dp),
        )
    }
}
