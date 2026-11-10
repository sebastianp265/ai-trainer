package com.github.sebastianp265.aitrainer.feature.health.impl

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.github.sebastianp265.aitrainer.integration.health.DailyAmount
import com.github.sebastianp265.aitrainer.integration.health.ExternalWorkoutReading
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType
import com.github.sebastianp265.aitrainer.integration.health.NutritionTotals
import com.github.sebastianp265.aitrainer.shared.database.impl.AppDatabase
import com.github.sebastianp265.aitrainer.shared.database.impl.AppDatabaseConstructor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import java.nio.file.Files
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Clock
import kotlin.time.Instant

class DefaultHealthServiceUTest {

    private val zone = TimeZone.UTC
    private val now = Instant.parse("2026-11-10T12:00:00Z")
    private val today = LocalDate(2026, 11, 10)
    private val clock = object : Clock {
        override fun now() = now
    }
    private val database = Room.inMemoryDatabaseBuilder<AppDatabase>(factory = { AppDatabaseConstructor.initialize() })
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Unconfined)
        .build()
    private val preferences = PreferenceDataStoreFactory.create(
        produceFile = { Files.createTempDirectory("health").resolve("health.preferences_pb").toFile() },
    )
    private val source = FakeHealthDataSource()
    private val service = DefaultHealthService(source, database.healthDao(), preferences, clock, zone)

    @AfterTest
    fun close() = database.close()

    @Test
    fun `first sync reads 30 days without the history permission`() = runBlocking {
        service.sync()

        assertEquals(listOf(today.minus(DatePeriod(days = 29))), source.readFrom)
    }

    @Test
    fun `first sync reads one year with the history permission`() = runBlocking {
        source.historyGranted = true

        service.sync()

        assertEquals(listOf(today.minus(DatePeriod(days = 364))), source.readFrom)
    }

    @Test
    fun `later syncs read the last 14 days`() = runBlocking {
        service.sync()
        source.readFrom.clear()

        service.sync()

        assertEquals(listOf(today.minus(DatePeriod(days = 13))), source.readFrom)
    }

    @Test
    fun `sync stores the time it finished`() = runBlocking {
        assertNull(service.observeLastSync().first())

        service.sync()

        assertEquals(now, service.observeLastSync().first())
    }

    @Test
    fun `re-sync overwrites a day edited in the source`() = runBlocking {
        source.nutrition = listOf(nutrition(today, kcal = 2000))
        service.sync()

        source.nutrition = listOf(nutrition(today, kcal = 2300))
        service.sync()

        assertEquals(2300, storedDay(today)?.kcalEaten)
    }

    @Test
    fun `re-sync removes a day deleted in the source`() = runBlocking {
        source.nutrition = listOf(nutrition(today, kcal = 2000))
        service.sync()

        source.nutrition = emptyList()
        service.sync()

        assertNull(storedDay(today))
    }

    @Test
    fun `days older than the re-sync window stay when the source forgets them`() = runBlocking {
        val old = today.minus(DatePeriod(days = 20))
        source.historyGranted = true
        source.nutrition = listOf(nutrition(old, kcal = 1800))
        service.sync()

        source.nutrition = emptyList()
        service.sync()

        assertEquals(1800, storedDay(old)?.kcalEaten)
    }

    @Test
    fun `a type without permission is skipped and the others still sync`() = runBlocking {
        source.granted = setOf(HealthDataType.STEPS)
        source.nutrition = listOf(nutrition(today, kcal = 2000))
        source.steps = listOf(DailyAmount(today, 9000))

        service.sync()

        val day = assertNotNull(storedDay(today))
        assertEquals(9000, day.steps)
        assertNull(day.kcalEaten)
    }

    @Test
    fun `values of a type that lost its permission are kept`() = runBlocking {
        source.nutrition = listOf(nutrition(today, kcal = 2000))
        service.sync()

        source.granted = setOf(HealthDataType.STEPS)
        source.nutrition = emptyList()
        service.sync()

        assertEquals(2000, storedDay(today)?.kcalEaten)
    }

    @Test
    fun `workouts written by this app are ignored`() = runBlocking {
        source.workouts = listOf(workout("running", fromThisApp = false), workout("weightlifting", fromThisApp = true))

        service.sync()

        assertEquals(listOf("running"), service.observeWorkouts().first().map { it.type })
    }

    private suspend fun storedDay(date: LocalDate) = service.observeDays(date, date).first().singleOrNull()

    private fun nutrition(date: LocalDate, kcal: Int) = NutritionTotals(date, kcal, null, null, null, null, null, null, null)

    private fun workout(type: String, fromThisApp: Boolean) = ExternalWorkoutReading(
        type = type,
        startedAt = Instant.parse("2026-11-09T10:00:00Z"),
        durationMinutes = 45,
        distanceM = null,
        kcal = null,
        averageHeartRate = null,
        writtenByThisApp = fromThisApp,
    )
}
