package com.github.sebastianp265.aitrainer.feature.health

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import org.koin.compose.koinInject

@Composable
internal fun ReloadHealthOnForeground() {
    val healthService: HealthService = koinInject()
    LifecycleResumeEffect(healthService) {
        healthService.reload()
        onPauseOrDispose {}
    }
}
