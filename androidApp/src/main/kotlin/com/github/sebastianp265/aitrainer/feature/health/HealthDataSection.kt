package com.github.sebastianp265.aitrainer.feature.health

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthConnectActions
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthConnectViewModel
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthDataSectionContent
import com.github.sebastianp265.aitrainer.feature.health.impl.openHealthConnectInPlayStore
import com.github.sebastianp265.aitrainer.feature.health.impl.openHealthConnectSettings
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HealthDataSection() {
    val viewModel: HealthConnectViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        viewModel.permissionsAnswered()
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    LaunchedEffect(state.permissionsToRequest) {
        if (state.permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(state.permissionsToRequest)
            viewModel.permissionsRequested()
        }
    }
    val actions = remember(context, viewModel) {
        object : HealthConnectActions {
            override fun turnOn(type: HealthDataType) = viewModel.turnOn(type)
            override fun turnOff(type: HealthDataType) = viewModel.turnOff(type)
            override fun allowHistory() = viewModel.allowHistory()
            override fun update() = context.openHealthConnectInPlayStore()
            override fun managePermissions() = context.openHealthConnectSettings()
        }
    }
    HealthDataSectionContent(state, actions)
}
