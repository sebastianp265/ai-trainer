package com.github.sebastianp265.aitrainer.domain.stats

import com.github.sebastianp265.aitrainer.domain.model.DailyEnergy
import com.github.sebastianp265.aitrainer.domain.model.EnergySource
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class EnergyBalanceUTest {

    private fun day(isoDate: String, eaten: Int?, burned: Int?) = DailyEnergy(
        date = LocalDate.parse(isoDate),
        kcalBurned = burned,
        kcalBurnedSource = burned?.let { EnergySource.HEALTH_CONNECT },
        kcalEaten = eaten,
        proteinG = null,
        carbsG = null,
        fatG = null,
    )

    private val days = listOf(
        day("2026-10-05", eaten = 2000, burned = 2500),
        day("2026-10-06", eaten = 2200, burned = null),
        day("2026-10-11", eaten = null, burned = 2700),
        day("2026-10-12", eaten = 2400, burned = 2600),
        day("2026-10-13", eaten = 2500, burned = 2400),
    )

    @Test
    fun `averages use only the days that have the value`() {
        assertEquals(
            EnergyAverages(averageIntakeKcal = 2275, averageBurnKcal = 2550, averageBalanceKcal = -200),
            energyAverages(days),
        )
    }

    @Test
    fun `weekly balance sums the days with both intake and burn, weeks starting on Monday`() {
        assertEquals(
            listOf(
                WeeklyBalance(LocalDate.parse("2026-10-05"), -500),
                WeeklyBalance(LocalDate.parse("2026-10-12"), -100),
            ),
            weeklyBalances(days),
        )
    }

    @Test
    fun `no averages without data`() {
        assertEquals(EnergyAverages(null, null, null), energyAverages(emptyList()))
    }
}
