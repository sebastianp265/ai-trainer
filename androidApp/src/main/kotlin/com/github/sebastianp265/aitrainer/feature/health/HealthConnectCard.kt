package com.github.sebastianp265.aitrainer.feature.health

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthConnectCardActions
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthConnectCardContent
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthConnectViewModel
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAccess
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HealthConnectCard(onConnectionChanged: () -> Unit) {
    val viewModel: HealthConnectViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        viewModel.refresh()
        onConnectionChanged()
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    val actions = remember(context, permissionLauncher) {
        object : HealthConnectCardActions {
            override fun connect(permissions: Set<String>) = permissionLauncher.launch(permissions)
            override fun update() = context.openHealthConnectInPlayStore()
        }
    }
    val access = state.access
    if (access != null && access != HealthConnectAccess.CONNECTED) {
        HealthConnectCardContent(access, state.requiredPermissions, actions)
    }
}

private const val HEALTH_CONNECT_PLAY_STORE_URI =
    "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"

private fun Context.openHealthConnectInPlayStore() {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, HEALTH_CONNECT_PLAY_STORE_URI.toUri()))
    } catch (_: ActivityNotFoundException) {
    }
}
