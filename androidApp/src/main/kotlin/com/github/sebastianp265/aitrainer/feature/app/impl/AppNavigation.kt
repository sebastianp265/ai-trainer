package com.github.sebastianp265.aitrainer.feature.app.impl

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import org.koin.compose.viewmodel.koinViewModel
import androidx.navigation.toRoute
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.state.LocalCoachEnabled
import com.github.sebastianp265.aitrainer.shared.state.LocalOpenCoach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.coach.ApiKeyScreen
import com.github.sebastianp265.aitrainer.feature.coach.CoachMemoryScreen
import com.github.sebastianp265.aitrainer.feature.coach.CoachProposalScreen
import com.github.sebastianp265.aitrainer.feature.coach.CoachScreen
import com.github.sebastianp265.aitrainer.feature.coach.ModelPickerScreen
import com.github.sebastianp265.aitrainer.feature.coach.WeeklyReviewOnForeground
import com.github.sebastianp265.aitrainer.feature.exercise.ExerciseHistoryScreen
import com.github.sebastianp265.aitrainer.feature.exercise.ExercisePickerScreen
import com.github.sebastianp265.aitrainer.feature.exercise.PICKED_EXERCISE_KEY
import com.github.sebastianp265.aitrainer.feature.health.HealthConnectScreen
import com.github.sebastianp265.aitrainer.feature.health.SyncHealthOnForeground
import com.github.sebastianp265.aitrainer.feature.history.ExternalWorkoutScreen
import com.github.sebastianp265.aitrainer.feature.history.HistoryScreen
import com.github.sebastianp265.aitrainer.feature.plan.NEW_PLAN
import com.github.sebastianp265.aitrainer.feature.plan.PlanDetailScreen
import com.github.sebastianp265.aitrainer.feature.plan.PlanEditScreen
import com.github.sebastianp265.aitrainer.feature.plan.PlanScreen
import com.github.sebastianp265.aitrainer.feature.progress.DayDetailScreen
import com.github.sebastianp265.aitrainer.feature.progress.ProgressMetricScreen
import com.github.sebastianp265.aitrainer.feature.progress.ProgressScreen
import com.github.sebastianp265.aitrainer.feature.settings.BackupScreen
import com.github.sebastianp265.aitrainer.feature.settings.SettingsScreen
import com.github.sebastianp265.aitrainer.feature.volume.VolumeTargetsScreen
import com.github.sebastianp265.aitrainer.feature.workout.ActiveWorkoutRedirect
import com.github.sebastianp265.aitrainer.feature.workout.ActiveWorkoutScreen
import com.github.sebastianp265.aitrainer.feature.workout.LogWorkoutScreen
import com.github.sebastianp265.aitrainer.feature.workout.WorkoutDetailScreen
import com.github.sebastianp265.aitrainer.feature.workout.WorkoutSummaryScreen
import kotlinx.datetime.LocalDate
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons

@Composable
internal fun AppNavigation() {
    val navController = rememberNavController()
    SyncHealthOnForeground()
    WeeklyReviewOnForeground()
    ActiveWorkoutRedirect(
        isActiveWorkoutOpen = { runCatching { navController.getBackStackEntry<ActiveWorkoutRoute>() }.isSuccess },
        onSessionStarted = { navController.navigate(ActiveWorkoutRoute) },
        onSessionEnded = { navController.popBackStack<ActiveWorkoutRoute>(inclusive = true) },
    )
    val backStackEntry by navController.currentBackStackEntryAsState()
    val destination = backStackEntry?.destination
    val isFullScreen = destination != null && fullScreenRoutes.any { destination.hasRoute(it) }
    val isNested = isFullScreen || destination != null && nestedRoutes.any { destination.hasRoute(it) }
    val coachEnabledViewModel: CoachEnabledViewModel = koinViewModel()
    val coachEnabled by coachEnabledViewModel.enabled.collectAsStateWithLifecycle()
    val isCoachTab = destination?.hasRoute(CoachRoute::class) == true
    val title = when {
        destination?.hasRoute(SettingsRoute::class) == true -> R.string.nav_settings
        destination?.hasRoute(HealthConnectRoute::class) == true -> R.string.health_connect
        destination?.hasRoute(CoachMemoryRoute::class) == true -> R.string.memory_title
        destination?.hasRoute(ApiKeyRoute::class) == true -> R.string.settings_claude_api_key
        destination?.hasRoute(ModelRoute::class) == true -> R.string.model_title
        destination?.hasRoute(BackupRoute::class) == true -> R.string.settings_backup
        destination?.hasRoute(HistoryRoute::class) == true -> R.string.history_title
        else -> TopLevelDestination.entries.firstOrNull { destination?.hasRoute(it.routeClass) == true }?.label
    }
    Scaffold(
        topBar = {
            if (!isFullScreen) {
                AppTopBar(isNested, title, onOpenSettings = { navController.navigate(SettingsRoute) }, onBack = navController::popBackStack, onOpenMemory = if (isCoachTab) ({ navController.navigate(CoachMemoryRoute) }) else null)
            }
        },
        bottomBar = { if (!isNested) AppBottomBar(navController, coachEnabled) },
    ) { padding ->
        CompositionLocalProvider(
            LocalCoachEnabled provides coachEnabled,
            LocalOpenCoach provides { navController.navigate(CoachRoute) },
        ) {
        NavHost(
            navController = navController,
            startDestination = LogRoute,
            modifier = Modifier.padding(padding).consumeWindowInsets(padding),
        ) {
            composable<LogRoute> { LogWorkoutScreen(
                    onOpenHistory = { navController.navigate(HistoryRoute) },
                    onOpenWorkout = { navController.navigate(WorkoutDetailRoute(it)) },
                    onOpenProposal = { navController.navigate(CoachProposalRoute(it)) },
                    onOpenCoach = { navController.navigate(CoachRoute) },
                ) }
            composable<CoachRoute> {
                CoachScreen(
                    onOpenPreview = { navController.navigate(CoachProposalRoute(it)) },
                    onOpenTargets = { navController.navigate(VolumeTargetsRoute) },
                    onOpenApiKey = { navController.navigate(ApiKeyRoute) },
                )
            }
            composable<CoachMemoryRoute> { CoachMemoryScreen(onOpenTargets = { navController.navigate(VolumeTargetsRoute) }) }
            composable<CoachProposalRoute> { entry ->
                CoachProposalScreen(
                    proposalId = entry.toRoute<CoachProposalRoute>().proposalId,
                    onAccepted = navController::popBackStack,
                    onReply = navController::popBackStack,
                )
            }
            composable<ApiKeyRoute> { ApiKeyScreen() }
            composable<ModelRoute> { ModelPickerScreen() }
            composable<BackupRoute> { BackupScreen() }
            composable<ActiveWorkoutRoute> { entry ->
                val picked by entry.savedStateHandle.getStateFlow<String?>(PICKED_EXERCISE_KEY, null).collectAsStateWithLifecycle()
                ActiveWorkoutScreen(
                    onFinished = { id ->
                        navController.navigate(WorkoutSummaryRoute(id)) { popUpTo<ActiveWorkoutRoute> { inclusive = true } }
                    },
                    onOpenExerciseHistory = { navController.navigate(ExerciseHistoryRoute(it)) },
                    onOpenExercisePicker = { navController.navigate(ExercisePickerRoute) },
                    pickedExerciseKey = picked,
                    onPickedExerciseHandled = { entry.savedStateHandle[PICKED_EXERCISE_KEY] = null },
                )
            }
            composable<ExerciseHistoryRoute> { entry ->
                ExerciseHistoryScreen(entry.toRoute<ExerciseHistoryRoute>().exerciseKey, onOpenWorkout = { navController.navigate(WorkoutDetailRoute(it)) })
            }
            composable<WorkoutSummaryRoute> { entry ->
                val sessionId = entry.toRoute<WorkoutSummaryRoute>().sessionId
                WorkoutSummaryScreen(
                    sessionId = sessionId,
                    onDone = { navController.popBackStack<LogRoute>(inclusive = false) },
                    onShowAllSets = { navController.navigate(WorkoutDetailRoute(sessionId)) },
                )
            }
            composable<WorkoutDetailRoute> { entry ->
                WorkoutDetailScreen(
                    sessionId = entry.toRoute<WorkoutDetailRoute>().sessionId,
                    onBack = navController::popBackStack,
                    onDeleted = navController::popBackStack,
                )
            }
            composable<ExercisePickerRoute> {
                ExercisePickerScreen(
                    onClose = navController::popBackStack,
                    onPicked = { key ->
                        navController.previousBackStackEntry?.savedStateHandle?.set(PICKED_EXERCISE_KEY, key)
                        navController.popBackStack()
                    },
                )
            }
            composable<VolumeTargetsRoute> { VolumeTargetsScreen(onOpenCoach = { navController.navigate(CoachRoute) }) }
            composable<HistoryRoute> {
                HistoryScreen(
                    onOpenWorkout = { navController.navigate(WorkoutDetailRoute(it)) },
                    onOpenExternalWorkout = { navController.navigate(ExternalWorkoutRoute(it)) },
                )
            }
            composable<ExternalWorkoutRoute> { entry -> ExternalWorkoutScreen(entry.toRoute<ExternalWorkoutRoute>().startedAtMillis) }
            composable<PlanRoute> {
                PlanScreen(
                    onOpenPlan = { navController.navigate(PlanDetailRoute(it)) },
                    onNewPlan = { navController.navigate(PlanEditRoute(NEW_PLAN)) },
                    onOpenTargets = { navController.navigate(VolumeTargetsRoute) },
                    onOpenProposal = { navController.navigate(CoachProposalRoute(it)) },
                    onOpenCoach = { navController.navigate(CoachRoute) },
                )
            }
            composable<PlanDetailRoute> { entry ->
                PlanDetailScreen(
                    planId = entry.toRoute<PlanDetailRoute>().planId,
                    onBack = navController::popBackStack,
                    onEdit = { navController.navigate(PlanEditRoute(entry.toRoute<PlanDetailRoute>().planId)) },
                    onStarted = navController::popBackStack,
                    onDeleted = navController::popBackStack,
                )
            }
            composable<PlanEditRoute> { entry ->
                val route = entry.toRoute<PlanEditRoute>()
                val picked by entry.savedStateHandle.getStateFlow<String?>(PICKED_EXERCISE_KEY, null).collectAsStateWithLifecycle()
                PlanEditScreen(
                    planId = route.planId,
                    onOpenExercisePicker = { navController.navigate(ExercisePickerRoute) },
                    pickedExerciseKey = picked,
                    onPickedExerciseHandled = { entry.savedStateHandle[PICKED_EXERCISE_KEY] = null },
                    onClose = navController::popBackStack,
                    onSaved = { id ->
                        if (route.planId == NEW_PLAN) {
                            navController.navigate(PlanDetailRoute(id)) { popUpTo<PlanEditRoute> { inclusive = true } }
                        } else {
                            navController.popBackStack()
                        }
                    },
                    onDeleted = { navController.popBackStack<PlanRoute>(inclusive = false) },
                )
            }
            composable<ProgressRoute> {
                ProgressScreen(
                    onOpenMetric = { navController.navigate(ProgressMetricRoute(it)) },
                    onOpenDay = { navController.navigate(DayDetailRoute(it.toString())) },
                    onOpenExercise = { navController.navigate(ExerciseHistoryRoute(it)) },
                    onOpenTargets = { navController.navigate(VolumeTargetsRoute) },
                )
            }
            composable<ProgressMetricRoute> { entry ->
                ProgressMetricScreen(
                    metric = entry.toRoute<ProgressMetricRoute>().metric,
                    onOpenDay = { navController.navigate(DayDetailRoute(it.toString())) },
                )
            }
            composable<DayDetailRoute> { entry -> DayDetailScreen(LocalDate.parse(entry.toRoute<DayDetailRoute>().date)) }
            composable<SettingsRoute> {
                SettingsScreen(
                    onOpenHealthConnect = { navController.navigate(HealthConnectRoute) },
                    onOpenApiKey = { navController.navigate(ApiKeyRoute) },
                    onOpenModel = { navController.navigate(ModelRoute) },
                    onOpenMemory = { navController.navigate(CoachMemoryRoute) },
                    onOpenBackup = { navController.navigate(BackupRoute) },
                )
            }
            composable<HealthConnectRoute> { HealthConnectScreen() }
        }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppTopBar(isNested: Boolean, @StringRes title: Int?, onOpenSettings: () -> Unit, onBack: () -> Unit, onOpenMemory: (() -> Unit)? = null) {
    TopAppBar(
        title = { if (title != null) Text(stringResource(title)) },
        navigationIcon = {
            if (isNested) {
                IconButton(onClick = onBack) {
                    Icon(DesignIcons.Back, contentDescription = stringResource(R.string.back))
                }
            }
        },
        actions = {
            if (onOpenMemory != null) {
                IconButton(onClick = onOpenMemory) {
                    Icon(DesignIcons.Person, contentDescription = stringResource(R.string.memory_title))
                }
            }
            if (!isNested) {
                IconButton(onClick = onOpenSettings) {
                    Icon(DesignIcons.Settings, contentDescription = stringResource(R.string.nav_settings))
                }
            }
        },
    )
}

@Composable
internal fun AppBottomBar(navController: NavHostController, coachEnabled: Boolean) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    NavigationBar {
        TopLevelDestination.entries.filter { coachEnabled || it != TopLevelDestination.COACH }.forEach { destination ->
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
