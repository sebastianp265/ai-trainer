package com.github.sebastianp265.aitrainer.feature.progress.impl

import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ProgressCalculatorUTest {

    private val today = LocalDate(2026, 11, 10)

    private fun day(
        date: LocalDate,
        kcalEaten: Int? = null,
        kcalBurned: Int? = null,
        proteinG: Double? = null,
        fiberG: Double? = null,
        weightKg: Double? = null,
        steps: Int? = null,
        sleepMinutes: Int? = null,
        restingHeartRate: Double? = null,
        hrvMs: Double? = null,
    ) = HealthDay(
        date, kcalEaten, proteinG, null, null, null, null, fiberG, null,
        kcalBurned, weightKg, steps, sleepMinutes, restingHeartRate, hrvMs,
    )

    private fun daysAgo(days: Int) = LocalDate.fromEpochDays(today.toEpochDays() - days)

    private fun cards(vararg days: HealthDay, range: ProgressRange = ProgressRange.FOUR_WEEKS) =
        ProgressCalculator.calculate(days.toList(), range, today)

    @Test
    fun `without data every card is missing`() {
        val cards = cards()

        assertNull(cards.weight)
        assertNull(cards.balance)
        assertNull(cards.macros)
        assertNull(cards.steps)
        assertNull(cards.sleepMinutes)
        assertNull(cards.restingHeartRate)
        assertNull(cards.hrvMs)
        assertTrue(cards.isEmpty)
    }

    @Test
    fun `weight card shows the 7 day average and the change over the range`() {
        val weight = assertNotNull(
            cards(
                day(daysAgo(20), weightKg = 84.0),
                day(daysAgo(2), weightKg = 82.0),
                day(daysAgo(1), weightKg = 83.0),
            ).weight,
        )

        assertEquals(82.5, weight.current)
        assertEquals(-1.5, weight.change)
        assertEquals(3, weight.points.size)
    }

    @Test
    fun `weight change is missing with a single measurement`() {
        val weight = assertNotNull(cards(day(daysAgo(1), weightKg = 82.0)).weight)

        assertEquals(82.0, weight.current)
        assertNull(weight.change)
    }

    @Test
    fun `days outside the range are ignored`() {
        val cards = cards(day(daysAgo(40), weightKg = 90.0), range = ProgressRange.FOUR_WEEKS)

        assertNull(cards.weight)
        assertNotNull(cards(day(daysAgo(40), weightKg = 90.0), range = ProgressRange.THREE_MONTHS).weight)
    }

    @Test
    fun `calorie balance is eaten minus burned averaged over finished days`() {
        val balance = assertNotNull(
            cards(
                day(daysAgo(2), kcalEaten = 2000, kcalBurned = 2500),
                day(daysAgo(1), kcalEaten = 2400, kcalBurned = 2500),
            ).balance,
        )

        assertEquals(-300, balance.averageBalanceKcal)
        assertEquals(2200, balance.averageEatenKcal)
        assertEquals(2500, balance.averageBurnedKcal)
    }

    @Test
    fun `today is left out of the calorie balance because the day is not over`() {
        val balance = cards(day(today, kcalEaten = 300, kcalBurned = 2500)).balance

        assertNull(balance)
    }

    @Test
    fun `calorie balance needs both eaten and burned`() {
        assertNull(cards(day(daysAgo(1), kcalEaten = 2000)).balance)
    }

    @Test
    fun `calorie balance bars are weekly averages starting on Monday`() {
        val monday = LocalDate(2026, 11, 2)
        val lastMonday = LocalDate(2026, 10, 26)
        val balance = assertNotNull(
            cards(
                day(lastMonday, kcalEaten = 2000, kcalBurned = 2500),
                day(LocalDate(2026, 10, 28), kcalEaten = 2100, kcalBurned = 2500),
                day(monday, kcalEaten = 2300, kcalBurned = 2500),
            ).balance,
        )

        assertEquals(listOf(WeekBar(lastMonday, -450), WeekBar(monday, -200)), balance.weeks)
    }

    @Test
    fun `macros average only the days that have the value`() {
        val macros = assertNotNull(
            cards(
                day(daysAgo(3), proteinG = 150.0, fiberG = 20.0),
                day(daysAgo(2), proteinG = 170.0),
            ).macros,
        )

        assertEquals(160.0, macros.proteinG)
        assertEquals(20.0, macros.fiberG)
        assertNull(macros.carbsG)
    }

    @Test
    fun `steps are averaged over finished days`() {
        val cards = cards(day(daysAgo(2), steps = 8000), day(daysAgo(1), steps = 9000), day(today, steps = 100))

        assertEquals(8500.0, cards.steps)
    }

    @Test
    fun `sleep is averaged over days with sleep`() {
        val cards = cards(day(daysAgo(2), sleepMinutes = 420), day(daysAgo(1), sleepMinutes = 480), day(today))

        assertEquals(450.0, cards.sleepMinutes)
    }

    @Test
    fun `resting heart rate shows its trend`() {
        val rate = assertNotNull(
            cards(
                day(daysAgo(25), restingHeartRate = 58.0),
                day(daysAgo(1), restingHeartRate = 54.0),
            ).restingHeartRate,
        )

        assertEquals(54.0, rate.current)
        assertEquals(-4.0, rate.change)
    }

    @Test
    fun `HRV is averaged`() {
        val cards = cards(day(daysAgo(2), hrvMs = 60.0), day(daysAgo(1), hrvMs = 64.0))

        assertEquals(62.0, cards.hrvMs)
    }

    @Test
    fun `recent days list the newest first and at most seven`() {
        val days = (0..9).map { day(daysAgo(it), steps = 1000) }.toTypedArray()

        val recent = cards(*days).recentDays

        assertEquals((0..6).map { daysAgo(it) }, recent.map { it.date })
    }
}
