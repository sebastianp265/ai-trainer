package com.github.sebastianp265.aitrainer.feature.coach

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.github.sebastianp265.aitrainer.feature.coach.impl.WeeklyReviewViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun WeeklyReviewOnForeground() {
    val viewModel: WeeklyReviewViewModel = koinViewModel()
    LifecycleResumeEffect(viewModel) {
        viewModel.reviewIfDue()
        onPauseOrDispose {}
    }
}
