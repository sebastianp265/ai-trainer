package com.github.sebastianp265.aitrainer.shared.charts.impl

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

private const val TARGET_STEPS = 3
private const val FLAT_SPREAD_FRACTION = 0.01
private val NICE_MULTIPLIERS = listOf(1.0, 2.0, 2.5, 5.0, 10.0)

internal data class ChartScale(val min: Double, val max: Double, val step: Double) {
    val ticks: List<Double>
        get() = generateSequence(min) { it + step }.takeWhile { it <= max + step / 2 }.toList()
}

internal fun chartScale(min: Double, max: Double): ChartScale {
    val spread = (max - min).takeIf { it > 0 } ?: (abs(max) * FLAT_SPREAD_FRACTION).takeIf { it > 0 } ?: 1.0
    val step = niceStep(spread / TARGET_STEPS)
    val scaleMin = floor(min / step) * step
    val scaleMax = ceil(max / step) * step
    return ChartScale(scaleMin, if (scaleMax > scaleMin) scaleMax else scaleMin + step, step)
}

private fun niceStep(raw: Double): Double {
    val magnitude = 10.0.pow(floor(log10(raw)))
    return NICE_MULTIPLIERS.map { it * magnitude }.first { it >= raw }
}
