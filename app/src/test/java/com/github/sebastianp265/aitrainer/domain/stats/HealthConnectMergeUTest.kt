package com.github.sebastianp265.aitrainer.domain.stats

import com.github.sebastianp265.aitrainer.domain.model.DailyEnergy
import com.github.sebastianp265.aitrainer.domain.model.EnergySource
import com.github.sebastianp265.aitrainer.domain.model.HealthDayTotals
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HealthConnectMergeUTest {

    private val date = LocalDate(2026, 10, 20)

    @Test
    fun `Polar burned calories win over Health Connect, intake comes from Health Connect`() {
        val existing = DailyEnergy(date, 2900, EnergySource.POLAR, null, null, null, null)
        val health = HealthDayTotals(date, kcalEaten = 2100, proteinG = 140.0, carbsG = 230.0, fatG = 70.0, kcalBurned = 2600)

        assertEquals(
            DailyEnergy(date, 2900, EnergySource.POLAR, 2100, 140.0, 230.0, 70.0),
            applyHealthConnectDay(date, existing, health),
        )
    }

    @Test
    fun `Health Connect burned calories are used when no other source has them`() {
        val health = HealthDayTotals(date, kcalEaten = 2100, proteinG = null, carbsG = null, fatG = null, kcalBurned = 2600)

        assertEquals(
            DailyEnergy(date, 2600, EnergySource.HEALTH_CONNECT, 2100, null, null, null),
            applyHealthConnectDay(date, null, health),
        )
    }

    @Test
    fun `values removed from Health Connect are removed from the day`() {
        val existing = DailyEnergy(date, 2500, EnergySource.HEALTH_CONNECT, 2000, 120.0, 210.0, 60.0)
        val health = HealthDayTotals(date, kcalEaten = null, proteinG = null, carbsG = null, fatG = null, kcalBurned = 2550)

        assertEquals(
            DailyEnergy(date, 2550, EnergySource.HEALTH_CONNECT, null, null, null, null),
            applyHealthConnectDay(date, existing, health),
        )
    }

    @Test
    fun `day without Health Connect data keeps only the Polar burned calories`() {
        val existing = DailyEnergy(date, 2900, EnergySource.POLAR, 2100, 140.0, 230.0, 70.0)

        assertEquals(
            DailyEnergy(date, 2900, EnergySource.POLAR, null, null, null, null),
            applyHealthConnectDay(date, existing, null),
        )
    }

    @Test
    fun `day that only had Health Connect data disappears when Health Connect has none`() {
        val existing = DailyEnergy(date, 2500, EnergySource.HEALTH_CONNECT, 2000, 120.0, 210.0, 60.0)

        assertNull(applyHealthConnectDay(date, existing, null))
    }
}
