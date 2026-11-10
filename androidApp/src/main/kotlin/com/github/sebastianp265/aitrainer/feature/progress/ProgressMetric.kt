package com.github.sebastianp265.aitrainer.feature.progress

import kotlinx.serialization.Serializable

@Serializable
internal enum class ProgressMetric {
    WEIGHT,
    CALORIE_BALANCE,
    STEPS,
    SLEEP,
    RESTING_HEART_RATE,
    HEART_RATE_VARIABILITY,
}
