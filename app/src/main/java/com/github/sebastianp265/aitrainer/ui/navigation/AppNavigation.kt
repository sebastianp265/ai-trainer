package com.github.sebastianp265.aitrainer.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.ui.log.LogWorkoutRoute
import com.github.sebastianp265.aitrainer.ui.plan.PlanScreen
import com.github.sebastianp265.aitrainer.ui.progress.ProgressScreen
import com.github.sebastianp265.aitrainer.ui.settings.SettingsScreen
import kotlinx.serialization.Serializable
import kotlin.reflect.KClass

@Serializable object LogRoute
@Serializable object PlanRoute
@Serializable object ProgressRoute
@Serializable object SettingsRoute

private enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    @StringRes val label: Int,
    val icon: ImageVector,
) {
    LOG(LogRoute, LogRoute::class, R.string.nav_log, Icons.Default.Edit),
    PLAN(PlanRoute, PlanRoute::class, R.string.nav_plan, Icons.Default.DateRange),
    PROGRESS(ProgressRoute, ProgressRoute::class, R.string.nav_progress, Icons.Default.Star),
    SETTINGS(SettingsRoute, SettingsRoute::class, R.string.nav_settings, Icons.Default.Settings),
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.hasRoute(destination.routeClass) } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(stringResource(destination.label)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = LogRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<LogRoute> { LogWorkoutRoute() }
            composable<PlanRoute> { PlanScreen() }
            composable<ProgressRoute> { ProgressScreen() }
            composable<SettingsRoute> { SettingsScreen() }
        }
    }
}
