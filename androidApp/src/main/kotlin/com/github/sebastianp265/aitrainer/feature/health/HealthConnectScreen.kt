package com.github.sebastianp265.aitrainer.feature.health

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthConnectActions
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthConnectContent
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthConnectSettingsRowContent
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthConnectViewModel
import com.github.sebastianp265.aitrainer.feature.health.impl.openHealthConnectInPlayStore
import com.github.sebastianp265.aitrainer.feature.health.impl.openHealthConnectSettings
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HealthConnectScreen() {
    val viewModel: HealthConnectViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        viewModel.refresh()
        viewModel.syncNow()
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    val actions = remember(context, permissionLauncher, viewModel) {
        object : HealthConnectActions {
            override fun connect(permissions: Set<String>) = permissionLauncher.launch(permissions)
            override fun update() = context.openHealthConnectInPlayStore()
            override fun syncNow() = viewModel.syncNow()
            override fun managePermissions() = context.openHealthConnectSettings()
        }
    }
    HealthConnectContent(state, actions)
}

@Composable
internal fun HealthConnectSettingsRow(onOpen: () -> Unit) {
    val viewModel: HealthConnectViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    HealthConnectSettingsRowContent(state, onOpen)
}
