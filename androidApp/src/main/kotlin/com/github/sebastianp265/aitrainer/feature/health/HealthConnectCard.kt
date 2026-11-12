package com.github.sebastianp265.aitrainer.feature.health

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthConnectCardActions
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthConnectCardContent
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthConnectViewModel
import com.github.sebastianp265.aitrainer.feature.health.impl.openHealthConnectInPlayStore
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAccess
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.getValue

@Composable
internal fun HealthConnectCard(onOpenSettings: () -> Unit) {
    val viewModel: HealthConnectViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    val actions = remember(context, onOpenSettings) {
        object : HealthConnectCardActions {
            override fun chooseData() = onOpenSettings()
            override fun update() = context.openHealthConnectInPlayStore()
        }
    }
    val access = state.access
    if (access != null && access != HealthConnectAccess.CONNECTED) {
        HealthConnectCardContent(access, actions)
    }
}
