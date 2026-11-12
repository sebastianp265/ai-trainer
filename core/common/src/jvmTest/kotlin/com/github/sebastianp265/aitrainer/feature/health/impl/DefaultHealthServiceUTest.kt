package com.github.sebastianp265.aitrainer.feature.health.impl

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.github.sebastianp265.aitrainer.integration.health.DailyAmount
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAccess
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAvailability
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType
import com.github.sebastianp265.aitrainer.integration.health.NutritionTotals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DefaultHealthServiceUTest {

    private val today = LocalDate(2026, 11, 10)
    private val yesterday = LocalDate(2026, 11, 9)
    private val preferences = PreferenceDataStoreFactory.create(
        produceFile = { Files.createTempDirectory("health").resolve("health.preferences_pb").toFile() },
    )
    private val source = FakeHealthDataSource()
    private val service = DefaultHealthService(source, preferences, HealthReloads(), TimeZone.UTC)

    @Test
    fun `days are read from Health Connect, newest first, one per day with data`() = runBlocking {
        source.nutrition = listOf(nutrition(yesterday), NutritionTotals(today, null, null, null, null, null, null, null, null))
        source.steps = listOf(DailyAmount(today, 9000))

        val days = service.observeDays(yesterday, today).first()

        assertEquals(listOf(today, yesterday), days.map { it.date })
        assertEquals(9000, days[0].steps)
        assertEquals(2000, days[1].kcalEaten)
    }

    @Test
    fun `a change in Health Connect shows after a reload`() = runBlocking {
        source.steps = listOf(DailyAmount(today, 9000))
        val days = service.observeDays(today, today)
        assertEquals(9000, days.first().single().steps)

        source.steps = emptyList()
        service.reload()

        assertEquals(emptyList(), days.first())
    }

    @Test
    fun `a type without permission is not read and the others still are`() = runBlocking {
        source.granted = setOf(HealthDataType.STEPS)
        source.nutrition = listOf(nutrition(today))
        source.steps = listOf(DailyAmount(today, 9000))

        val day = service.observeDays(today, today).first().single()

        assertEquals(9000, day.steps)
        assertEquals(null, day.kcalEaten)
    }

    @Test
    fun `a type switched off in the app is not read until it is switched on again`() = runBlocking {
        source.nutrition = listOf(nutrition(today))

        service.disable(HealthDataType.NUTRITION)
        val whileOff = service.observeDays(today, today).first()
        service.enable(HealthDataType.NUTRITION)

        assertEquals(emptyList(), whileOff)
        assertEquals(2000, service.observeDays(today, today).first().single().kcalEaten)
    }

    @Test
    fun `the status leaves out a type switched off in the app`() = runBlocking {
        source.granted = setOf(HealthDataType.STEPS)

        service.disable(HealthDataType.STEPS)

        val status = service.status()
        assertEquals(HealthConnectAccess.NOT_CONNECTED, status.access)
        assertFalse(HealthDataType.STEPS in status.enabledTypes)
    }

    @Test
    fun `enabling a type asks only for permissions that are missing`() = runBlocking {
        source.granted = setOf(HealthDataType.STEPS)

        assertEquals(emptySet(), service.enable(HealthDataType.STEPS))
        assertEquals(setOf("read:WEIGHT"), service.enable(HealthDataType.WEIGHT))
    }

    @Test
    fun `history access is asked for only while it is missing`() = runBlocking {
        assertEquals(setOf("read:history"), service.missingHistoryPermissions())
        assertFalse(service.status().hasHistoryAccess)

        source.historyGranted = true

        assertEquals(emptySet(), service.missingHistoryPermissions())
        assertTrue(service.status().hasHistoryAccess)
    }

    @Test
    fun `nothing is read while Health Connect is unavailable`() = runBlocking {
        source.availability = HealthConnectAvailability.UNAVAILABLE
        source.steps = listOf(DailyAmount(today, 9000))

        assertEquals(emptyList(), service.observeDays(today, today).first())
        assertEquals(HealthConnectAccess.UNAVAILABLE, service.status().access)
    }

    private fun nutrition(date: LocalDate) = NutritionTotals(date, 2000, null, null, null, null, null, null, null)
}
