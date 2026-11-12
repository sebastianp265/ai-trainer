package com.github.sebastianp265.aitrainer.feature.app.impl

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.feature.progress.ProgressMetric
import kotlin.reflect.KClass
import kotlinx.serialization.Serializable
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons

internal enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    LOG(LogRoute, LogRoute::class, R.string.nav_log, DesignIcons.Training),
    COACH(CoachRoute, CoachRoute::class, R.string.nav_coach, DesignIcons.Coach),
    PROGRESS(ProgressRoute, ProgressRoute::class, R.string.nav_progress, DesignIcons.Progress),
    SETTINGS(SettingsRoute, SettingsRoute::class, R.string.nav_settings, DesignIcons.Settings),
}

@Serializable
internal object LogRoute

@Serializable
internal object CoachRoute

@Serializable
internal object ProgressRoute

@Serializable
internal object SettingsRoute

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
internal object CoachMemoryRoute

@Serializable
internal data class CoachProposalRoute(val proposalId: Long)

@Serializable
internal object ApiKeyRoute

@Serializable
internal object ModelRoute

@Serializable
internal object BackupRoute

@Serializable
internal object VolumeTargetsRoute

@Serializable
internal object HistoryRoute

@Serializable
internal data class CoachAboutRoute(val topicKind: String, val topicId: String, val topicLabel: String)

internal val fullScreenRoutes = listOf(ActiveWorkoutRoute::class, ExercisePickerRoute::class, PlanDetailRoute::class, PlanEditRoute::class,
    WorkoutSummaryRoute::class, WorkoutDetailRoute::class, CoachProposalRoute::class,
)

internal val nestedRoutes = listOf(
    CoachAboutRoute::class,
    ProgressMetricRoute::class,
    DayDetailRoute::class,
    HistoryRoute::class,
    VolumeTargetsRoute::class,
    CoachMemoryRoute::class,
    ApiKeyRoute::class,
    ModelRoute::class,
    BackupRoute::class,
    ExerciseHistoryRoute::class,
)
