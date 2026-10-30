package com.github.sebastianp265.aitrainer.data.healthconnect

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.NutritionRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toKotlinLocalDate
import java.time.Period
import kotlin.math.roundToInt

class HealthConnectDataSource(private val context: Context) : HealthDataSource {

    override val requiredPermissions: Set<String> = setOf(
        HealthPermission.getReadPermission(NutritionRecord::class),
    )

    private val client by lazy { HealthConnectClient.getOrCreate(context) }

    override fun availability(): HealthConnectAvailability = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectAvailability.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectAvailability.UPDATE_REQUIRED
        else -> HealthConnectAvailability.UNAVAILABLE
    }

    override suspend fun hasAllPermissions(): Boolean =
        availability() == HealthConnectAvailability.AVAILABLE &&
            client.permissionController.getGrantedPermissions().containsAll(requiredPermissions)

    override suspend fun readDailyTotals(from: LocalDate, toInclusive: LocalDate): List<HealthConnectDayDto> {
        val dayAfterLast = toInclusive.plus(DatePeriod(days = 1))
        val buckets = client.aggregateGroupByPeriod(
            AggregateGroupByPeriodRequest(
                metrics = setOf(
                    NutritionRecord.ENERGY_TOTAL,
                    NutritionRecord.PROTEIN_TOTAL,
                    NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL,
                    NutritionRecord.TOTAL_FAT_TOTAL,
                ),
                timeRangeFilter = TimeRangeFilter.between(
                    from.toJavaLocalDate().atStartOfDay(),
                    dayAfterLast.toJavaLocalDate().atStartOfDay(),
                ),
                timeRangeSlicer = Period.ofDays(1),
            ),
        )
        return buckets.map { bucket ->
            HealthConnectDayDto(
                date = bucket.startTime.toLocalDate().toKotlinLocalDate(),
                kcalEaten = bucket.result[NutritionRecord.ENERGY_TOTAL]?.inKilocalories?.roundToInt(),
                proteinG = bucket.result[NutritionRecord.PROTEIN_TOTAL]?.inGrams,
                carbsG = bucket.result[NutritionRecord.TOTAL_CARBOHYDRATE_TOTAL]?.inGrams,
                fatG = bucket.result[NutritionRecord.TOTAL_FAT_TOTAL]?.inGrams,
                sourcePackages = bucket.result.dataOrigins.map { it.packageName }.sorted(),
            )
        }
    }
}
