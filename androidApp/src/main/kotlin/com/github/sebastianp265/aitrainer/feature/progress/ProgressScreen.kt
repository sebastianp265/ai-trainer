package com.github.sebastianp265.aitrainer.feature.progress

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.progress.impl.DayDetailContent
import com.github.sebastianp265.aitrainer.feature.progress.impl.DayDetailViewModel
import com.github.sebastianp265.aitrainer.feature.progress.impl.MetricContent
import com.github.sebastianp265.aitrainer.feature.progress.impl.MetricViewModel
import com.github.sebastianp265.aitrainer.feature.progress.impl.ProgressContent
import com.github.sebastianp265.aitrainer.feature.progress.impl.ProgressViewModel
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun ProgressScreen(onOpenMetric: (ProgressMetric) -> Unit, onOpenDay: (LocalDate) -> Unit) {
    val viewModel: ProgressViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ProgressContent(
        state = state,
        onRangeSelected = viewModel::selectRange,
        onHealthConnectChanged = viewModel::syncAfterConnecting,
        onOpenMetric = onOpenMetric,
        onOpenDay = onOpenDay,
    )
}

@Composable
internal fun ProgressMetricScreen(metric: ProgressMetric, onOpenDay: (LocalDate) -> Unit) {
    val viewModel: MetricViewModel = koinViewModel()
    val days by viewModel.days.collectAsStateWithLifecycle()
    MetricContent(metric, days, viewModel.today, onOpenDay)
}

@Composable
internal fun DayDetailScreen(date: LocalDate) {
    val viewModel: DayDetailViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(date) { viewModel.show(date) }
    DayDetailContent(state)
}
