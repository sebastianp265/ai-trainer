package com.github.sebastianp265.aitrainer.feature.app.impl

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import com.github.sebastianp265.aitrainer.R
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
