package com.github.sebastianp265.aitrainer.shared.state

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.SharingStarted

private const val KEEP_QUERIES_ALIVE_AFTER_UI_STOPS_MS = 5_000L

internal val WhileUiSubscribed: SharingStarted = SharingStarted.WhileSubscribed(KEEP_QUERIES_ALIVE_AFTER_UI_STOPS_MS)

internal val LocalCoachEnabled = staticCompositionLocalOf { false }

internal val LocalOpenCoach = staticCompositionLocalOf { {} }
