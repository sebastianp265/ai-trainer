package com.github.sebastianp265.aitrainer.feature.history

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.history.impl.ExternalWorkoutContent
import com.github.sebastianp265.aitrainer.feature.history.impl.ExternalWorkoutViewModel
import com.github.sebastianp265.aitrainer.feature.history.impl.HistoryContent
import com.github.sebastianp265.aitrainer.feature.history.impl.HistoryViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun HistoryScreen(onOpenWorkout: (sessionId: Long) -> Unit, onOpenExternalWorkout: (startedAtMillis: Long) -> Unit) {
    val viewModel: HistoryViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(state, onOpenWorkout = onOpenWorkout, onOpenExternal = { onOpenExternalWorkout(it.startedAt.toEpochMilliseconds()) })
}

@Composable
internal fun ExternalWorkoutScreen(startedAtMillis: Long) {
    val viewModel: ExternalWorkoutViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(startedAtMillis) { viewModel.show(startedAtMillis) }
    ExternalWorkoutContent(state)
}
