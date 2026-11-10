package com.github.sebastianp265.aitrainer.shared.health.impl

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregateMetric
import androidx.health.connect.client.aggregate.AggregationResultGroupedByPeriod
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.github.sebastianp265.aitrainer.integration.health.DailyAmount
import com.github.sebastianp265.aitrainer.integration.health.ExternalWorkoutReading
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAvailability
import com.github.sebastianp265.aitrainer.integration.health.HealthDataSource
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType
import com.github.sebastianp265.aitrainer.integration.health.NutritionTotals
import com.github.sebastianp265.aitrainer.integration.health.SleepSession
import com.github.sebastianp265.aitrainer.integration.health.TimedValue
import kotlinx.datetime.LocalDate
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.Duration
import java.time.Period
import java.time.ZoneId
import kotlin.math.roundToInt
import kotlin.reflect.KClass
import kotlin.time.Instant

internal class HealthConnectDataSource(private val context: Context) : HealthDataSource {

    private val permissionByType: Map<HealthDataType, String> = mapOf(
        HealthDataType.NUTRITION to read(NutritionRecord::class),
        HealthDataType.CALORIES_BURNED to read(TotalCaloriesBurnedRecord::class),
        HealthDataType.WEIGHT to read(WeightRecord::class),
        HealthDataType.STEPS to read(StepsRecord::class),
        HealthDataType.SLEEP to read(SleepSessionRecord::class),
        HealthDataType.RESTING_HEART_RATE to read(RestingHeartRateRecord::class),
        HealthDataType.HEART_RATE_VARIABILITY to read(HeartRateVariabilityRmssdRecord::class),
        HealthDataType.WORKOUTS to read(ExerciseSessionRecord::class),
    )

    private val workoutDetailPermissions = setOf(read(DistanceRecord::class), read(HeartRateRecord::class))

    override val permissionsToRequest: Set<String> =
        permissionByType.values.toSet() + workoutDetailPermissions + HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    override fun availability(): HealthConnectAvailability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
        else -> HealthConnectAvailability.UNAVAILABLE
    }

    override suspend fun grantedTypes(): Set<HealthDataType> {
        val granted = grantedPermissions()
        return permissionByType.filterValues { it in granted }.keys
    }

    override suspend fun hasHistoryPermission(): Boolean =
        HealthPermission.PERMISSION_READ_HEALTH_DATA_HISTORY in grantedPermissions()

    override suspend fun readNutrition(from: LocalDate, toInclusive: LocalDate): List<NutritionTotals> =
        dailyBuckets(
            from,
            toInclusive,
            setOf(
                NutritionRecord.ENERGY_TOTAL,
                NutritionRecord.PROTEIN_TOTAL,
                NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL,
                NutritionRecord.SUGAR_TOTAL,
                NutritionRecord.TOTAL_FAT_TOTAL,
                NutritionRecord.SATURATED_FAT_TOTAL,
                NutritionRecord.DIETARY_FIBER_TOTAL,
                NutritionRecord.SODIUM_TOTAL,
            ),
        ).map { (date, bucket) ->
            val result = bucket.result
            NutritionTotals(
                date = date,
                kcal = result[NutritionRecord.ENERGY_TOTAL]?.inKilocalories?.roundToInt(),
                proteinG = result[NutritionRecord.PROTEIN_TOTAL]?.inGrams,
                carbsG = result[NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL]?.inGrams,
                sugarG = result[NutritionRecord.SUGAR_TOTAL]?.inGrams,
                fatG = result[NutritionRecord.TOTAL_FAT_TOTAL]?.inGrams,
                saturatedFatG = result[NutritionRecord.SATURATED_FAT_TOTAL]?.inGrams,
                fiberG = result[NutritionRecord.DIETARY_FIBER_TOTAL]?.inGrams,
                sodiumMg = result[NutritionRecord.SODIUM_TOTAL]?.inMilligrams,
            )
        }

    override suspend fun readCaloriesBurned(from: LocalDate, toInclusive: LocalDate): List<DailyAmount> =
        dailyBuckets(from, toInclusive, setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL)).mapNotNull { (date, bucket) ->
            bucket.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.roundToInt()?.let { DailyAmount(date, it) }
        }

    override suspend fun readSteps(from: LocalDate, toInclusive: LocalDate): List<DailyAmount> =
        dailyBuckets(from, toInclusive, setOf(StepsRecord.COUNT_TOTAL)).mapNotNull { (date, bucket) ->
            bucket.result[StepsRecord.COUNT_TOTAL]?.toInt()?.let { DailyAmount(date, it) }
        }

    override suspend fun readWeights(from: LocalDate, toInclusive: LocalDate): List<TimedValue> =
        readAll<WeightRecord>(from, toInclusive).map { TimedValue(it.time.toKotlin(), it.weight.inKilograms) }

    override suspend fun readSleep(from: LocalDate, toInclusive: LocalDate): List<SleepSession> =
        readAll<SleepSessionRecord>(from, toInclusive).map {
            SleepSession(it.endTime.toKotlin(), Duration.between(it.startTime, it.endTime).toMinutes().toInt())
        }

    override suspend fun readRestingHeartRates(from: LocalDate, toInclusive: LocalDate): List<TimedValue> =
        readAll<RestingHeartRateRecord>(from, toInclusive).map { TimedValue(it.time.toKotlin(), it.beatsPerMinute.toDouble()) }

    override suspend fun readHeartRateVariabilities(from: LocalDate, toInclusive: LocalDate): List<TimedValue> =
        readAll<HeartRateVariabilityRmssdRecord>(from, toInclusive)
            .map { TimedValue(it.time.toKotlin(), it.heartRateVariabilityMillis) }

    override suspend fun readWorkouts(from: LocalDate, toInclusive: LocalDate): List<ExternalWorkoutReading> {
        val granted = grantedPermissions()
        return readAll<ExerciseSessionRecord>(from, toInclusive).map { session ->
            val details = workoutDetails(session, granted)
            ExternalWorkoutReading(
                type = ExerciseSessionRecord.EXERCISE_TYPE_INT_TO_STRING_MAP[session.exerciseType] ?: "other",
                startedAt = session.startTime.toKotlin(),
                durationMinutes = Duration.between(session.startTime, session.endTime).toMinutes().toInt(),
                distanceM = details.distanceM,
                kcal = details.kcal,
                averageHeartRate = details.averageHeartRate,
                writtenByThisApp = session.metadata.dataOrigin.packageName == context.packageName,
            )
        }
    }

    private class WorkoutDetails(val distanceM: Double?, val kcal: Int?, val averageHeartRate: Int?)

    private suspend fun workoutDetails(session: ExerciseSessionRecord, granted: Set<String>): WorkoutDetails {
        val metrics = buildSet<AggregateMetric<*>> {
            if (read(DistanceRecord::class) in granted) add(DistanceRecord.DISTANCE_TOTAL)
            if (read(TotalCaloriesBurnedRecord::class) in granted) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
            if (read(HeartRateRecord::class) in granted) add(HeartRateRecord.BPM_AVG)
        }
        if (metrics.isEmpty()) return WorkoutDetails(null, null, null)
        val result = client.aggregate(
            AggregateRequest(
                metrics = metrics,
                timeRangeFilter = TimeRangeFilter.between(session.startTime, session.endTime),
                dataOriginFilter = setOf(session.metadata.dataOrigin),
            ),
        )
        return WorkoutDetails(
            distanceM = result[DistanceRecord.DISTANCE_TOTAL]?.inMeters,
            kcal = result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories?.roundToInt(),
            averageHeartRate = result[HeartRateRecord.BPM_AVG]?.toInt(),
        )
    }

    private suspend fun grantedPermissions(): Set<String> =
        if (availability() == HealthConnectAvailability.AVAILABLE) client.permissionController.getGrantedPermissions() else emptySet()

    private suspend fun dailyBuckets(
        from: LocalDate,
        toInclusive: LocalDate,
        metrics: Set<AggregateMetric<*>>,
    ): List<Pair<LocalDate, AggregationResultGroupedByPeriod>> {
        val buckets = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = metrics,
                timeRangeFilter = TimeRangeFilter.between(
                    from.toJavaLocalDate().atStartOfDay(),
                    toInclusive.toJavaLocalDate().plusDays(1).atStartOfDay(),
                ),
                timeRangeSlicer = Period.ofDays(1),
            ),
        )
        return buckets.map { it.startTime.toLocalDate().toKotlinLocalDate() to it }
    }

    private suspend inline fun <reified T : Record> readAll(from: LocalDate, toInclusive: LocalDate): List<T> {
        val zone = ZoneId.systemDefault()
        val range = TimeRangeFilter.between(
            from.toJavaLocalDate().atStartOfDay(zone).toInstant(),
            toInclusive.toJavaLocalDate().plusDays(1).atStartOfDay(zone).toInstant(),
        )
        val records = mutableListOf<T>()
        var pageToken: String? = null
        do {
            val response = client.readRecords(ReadRecordsRequest(T::class, range, pageSize = PAGE_SIZE, pageToken = pageToken))
            records += response.records
            pageToken = response.pageToken
        } while (pageToken != null)
        return records
    }

    private fun java.time.Instant.toKotlin(): Instant = Instant.fromEpochMilliseconds(toEpochMilli())

    private fun read(record: KClass<out Record>): String = HealthPermission.getReadPermission(record)

    private companion object {
        const val PAGE_SIZE = 5000
    }
}
