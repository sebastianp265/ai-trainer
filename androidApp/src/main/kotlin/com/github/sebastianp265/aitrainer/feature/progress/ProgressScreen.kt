package com.github.sebastianp265.aitrainer.feature.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.progress.impl.ProgressContent
import com.github.sebastianp265.aitrainer.feature.progress.impl.ProgressViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ProgressScreen() {
    val viewModel: ProgressViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressContent(state, onRangeSelected = viewModel::selectRange, onHealthConnectChanged = viewModel::syncAfterConnecting)
}
