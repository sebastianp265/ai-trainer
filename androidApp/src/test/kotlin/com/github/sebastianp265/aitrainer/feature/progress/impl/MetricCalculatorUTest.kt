package com.github.sebastianp265.aitrainer.feature.progress.impl

import com.github.sebastianp265.aitrainer.feature.progress.ProgressMetric
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class MetricCalculatorUTest {

    private val today = LocalDate(2026, 11, 10)

    private fun daysAgo(days: Int) = LocalDate.fromEpochDays(today.toEpochDays() - days)

    private fun day(date: LocalDate, weightKg: Double? = null, steps: Int? = null, eaten: Int? = null, burned: Int? = null) =
        HealthDay(date, eaten, null, null, null, null, null, null, null, burned, weightKg, steps, null, null, null)

    private fun detail(metric: ProgressMetric, vararg days: HealthDay, range: ProgressRange = ProgressRange.FOUR_WEEKS) =
        MetricCalculator.calculate(days.toList(), metric, range, today)

    @Test
    fun `a metric without values has no detail`() {
        assertNull(detail(ProgressMetric.WEIGHT, day(daysAgo(1), steps = 100)))
    }

    @Test
    fun `detail has the lowest and highest daily value`() {
        val detail = assertNotNull(
            detail(ProgressMetric.WEIGHT, day(daysAgo(10), weightKg = 84.0), day(daysAgo(5), weightKg = 81.9), day(daysAgo(1), weightKg = 83.0)),
        )

        assertEquals(81.9, detail.lowest)
        assertEquals(84.0, detail.highest)
    }

    @Test
    fun `change per week follows the 7 day average over the span of the data`() {
        val detail = assertNotNull(
            detail(ProgressMetric.WEIGHT, day(daysAgo(15), weightKg = 85.0), day(daysAgo(1), weightKg = 83.0)),
        )

        assertEquals(-2.0, detail.change)
        assertEquals(-1.0, detail.changePerWeek)
    }

    @Test
    fun `change per week is missing when the data spans less than a week`() {
        val detail = assertNotNull(
            detail(ProgressMetric.WEIGHT, day(daysAgo(3), weightKg = 85.0), day(daysAgo(1), weightKg = 83.0)),
        )

        assertNull(detail.changePerWeek)
    }

    @Test
    fun `values are listed newest first`() {
        val detail = assertNotNull(
            detail(ProgressMetric.WEIGHT, day(daysAgo(5), weightKg = 82.0), day(daysAgo(2), weightKg = 83.0)),
        )

        assertEquals(listOf(daysAgo(2), daysAgo(5)), detail.newestFirst.map { it.date })
    }

    @Test
    fun `calorie balance detail uses finished days with eaten and burned`() {
        val detail = assertNotNull(
            detail(
                ProgressMetric.CALORIE_BALANCE,
                day(daysAgo(2), eaten = 2000, burned = 2500),
                day(daysAgo(1), eaten = 2400),
                day(today, eaten = 100, burned = 2500),
            ),
        )

        assertEquals(listOf(-500.0), detail.points.map { it.value })
    }
}
