package com.github.sebastianp265.aitrainer.feature.health

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthSyncViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SyncHealthOnForeground() {
    val viewModel: HealthSyncViewModel = koinViewModel()
    LifecycleResumeEffect(viewModel) {
        viewModel.sync()
        onPauseOrDispose {}
    }
}
