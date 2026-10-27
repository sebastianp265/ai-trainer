package com.github.sebastianp265.aitrainer.domain.stats

import com.github.sebastianp265.aitrainer.domain.model.WeightMeasurement
import com.github.sebastianp265.aitrainer.domain.model.WeightSource
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Instant

class WeightTrendUTest {

    private fun measurement(isoInstant: String, weightKg: Double) =
        WeightMeasurement(0, Instant.parse(isoInstant), weightKg, WeightSource.MANUAL)

    private fun day(isoDate: String, weightKg: Double) = DailyWeight(LocalDate.parse(isoDate), weightKg)

    private fun assertWeights(expected: List<DailyWeight>, actual: List<DailyWeight>) {
        assertEquals(expected.map { it.date }, actual.map { it.date })
        expected.zip(actual).forEach { (e, a) -> assertEquals(e.weightKg, a.weightKg, absoluteTolerance = 1e-9, "on ${e.date}") }
    }

    @Test
    fun `measurements on the same day are averaged`() {
        val daily = dailyAverageWeights(
            listOf(
                measurement("2026-10-02T07:00:00Z", 81.9),
                measurement("2026-10-01T07:00:00Z", 82.0),
                measurement("2026-10-01T20:00:00Z", 82.6),
            ),
            TimeZone.UTC,
        )

        assertWeights(listOf(day("2026-10-01", 82.3), day("2026-10-02", 81.9)), daily)
    }

    @Test
    fun `moving average covers the last seven calendar days, including gaps`() {
        val daily = listOf(
            day("2026-10-01", 80.0),
            day("2026-10-02", 82.0),
            day("2026-10-03", 84.0),
            day("2026-10-09", 90.0),
        )

        assertWeights(
            listOf(day("2026-10-01", 80.0), day("2026-10-02", 81.0), day("2026-10-03", 82.0), day("2026-10-09", 87.0)),
            sevenDayMovingAverage(daily),
        )
    }

    @Test
    fun `weekly change is the fitted daily slope times seven`() {
        val daily = (1..15).map { dayOfMonth -> DailyWeight(LocalDate(2026, 10, dayOfMonth), 80.0 - 0.1 * (dayOfMonth - 1)) }

        val change = weeklyWeightChangeKg(daily)

        assertNotNull(change)
        assertEquals(-0.7, change, absoluteTolerance = 1e-9)
    }

    @Test
    fun `no weekly change when the data spans less than a week`() {
        val daily = listOf(day("2026-10-01", 80.0), day("2026-10-07", 79.0))

        assertNull(weeklyWeightChangeKg(daily))
    }
}
