package com.github.sebastianp265.aitrainer.feature.app.impl

import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.feature.plan.PlanScreen
import com.github.sebastianp265.aitrainer.feature.progress.ProgressScreen
import com.github.sebastianp265.aitrainer.feature.settings.SettingsScreen
import com.github.sebastianp265.aitrainer.feature.workout.LogWorkoutScreen

@Composable
internal fun AppNavigation() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val isSettingsOpen = backStackEntry?.destination?.hasRoute(SettingsRoute::class) == true
    Scaffold(
        topBar = { AppTopBar(isSettingsOpen, onOpenSettings = { navController.navigate(SettingsRoute) }, onBack = navController::popBackStack) },
        bottomBar = { if (!isSettingsOpen) AppBottomBar(navController) },
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = LogRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<LogRoute> { LogWorkoutScreen() }
            composable<PlanRoute> { PlanScreen() }
            composable<ProgressRoute> { ProgressScreen() }
            composable<SettingsRoute> { SettingsScreen() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppTopBar(isSettingsOpen: Boolean, onOpenSettings: () -> Unit, onBack: () -> Unit) {
    TopAppBar(
        title = { if (isSettingsOpen) Text(stringResource(R.string.nav_settings)) },
        navigationIcon = {
            if (isSettingsOpen) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
            }
        },
        actions = {
            if (!isSettingsOpen) {
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.nav_settings))
                }
            }
        },
    )
}

@Composable
internal fun AppBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
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
}
