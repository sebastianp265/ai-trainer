package com.github.sebastianp265.aitrainer.feature.history

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.history.impl.HistoryContent
import com.github.sebastianp265.aitrainer.feature.history.impl.HistoryViewModel
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.getValue

@Composable
internal fun HistoryScreen(onOpenWorkout: (sessionId: Long) -> Unit) {
    val viewModel: HistoryViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    HistoryContent(state, onOpenWorkout = onOpenWorkout)
}
