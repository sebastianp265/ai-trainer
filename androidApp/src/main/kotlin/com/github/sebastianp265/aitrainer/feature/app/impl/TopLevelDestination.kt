package com.github.sebastianp265.aitrainer.feature.app.impl

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.feature.progress.ProgressMetric
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable

internal enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    LOG(LogRoute, LogRoute::class, R.string.nav_log, Icons.Default.Edit),
    PLAN(PlanRoute, PlanRoute::class, R.string.nav_plan, Icons.Default.DateRange),
    PROGRESS(ProgressRoute, ProgressRoute::class, R.string.nav_progress, Icons.Default.Star),
}

@Serializable
internal object LogRoute

@Serializable
internal object PlanRoute

@Serializable
internal object ProgressRoute

@Serializable
internal object SettingsRoute

@Serializable
internal object HealthConnectRoute

@Serializable
internal data class ProgressMetricRoute(val metric: ProgressMetric)

@Serializable
internal data class DayDetailRoute(val date: String)

@Serializable
internal object ActiveWorkoutRoute

@Serializable
internal data class WorkoutSummaryRoute(val sessionId: Long)

@Serializable
internal data class WorkoutDetailRoute(val sessionId: Long)

@Serializable
internal object ExercisePickerRoute

@Serializable
internal data class ExerciseHistoryRoute(val exerciseKey: String)

@Serializable
internal data class PlanDetailRoute(val planId: Long)

@Serializable
internal data class PlanEditRoute(val planId: Long)

@Serializable
internal object VolumeTargetsRoute

@Serializable
internal object HistoryRoute

@Serializable
internal data class ExternalWorkoutRoute(val startedAtMillis: Long)

internal val fullScreenRoutes = listOf(ActiveWorkoutRoute::class, ExercisePickerRoute::class, PlanDetailRoute::class, PlanEditRoute::class,
    WorkoutSummaryRoute::class, WorkoutDetailRoute::class,
)

internal val nestedRoutes = listOf(
    SettingsRoute::class,
    HealthConnectRoute::class,
    ProgressMetricRoute::class,
    DayDetailRoute::class,
    HistoryRoute::class,
    VolumeTargetsRoute::class,
    ExerciseHistoryRoute::class,
    ExternalWorkoutRoute::class,
)
