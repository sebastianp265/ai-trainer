package com.github.sebastianp265.aitrainer.domain.usecase

import com.github.sebastianp265.aitrainer.data.healthconnect.HealthConnectAvailability
import com.github.sebastianp265.aitrainer.data.healthconnect.HealthDataSource

enum class HealthConnectAccess { UNAVAILABLE, UPDATE_REQUIRED, NOT_CONNECTED, CONNECTED }

class CheckHealthConnectAccessUseCase(private val healthDataSource: HealthDataSource) {

    suspend operator fun invoke(): HealthConnectAccess = when (healthDataSource.availability()) {
        HealthConnectAvailability.UNAVAILABLE -> HealthConnectAccess.UNAVAILABLE
        HealthConnectAvailability.UPDATE_REQUIRED -> HealthConnectAccess.UPDATE_REQUIRED
        HealthConnectAvailability.AVAILABLE ->
            if (healthDataSource.hasAllPermissions()) HealthConnectAccess.CONNECTED else HealthConnectAccess.NOT_CONNECTED
    }
}
