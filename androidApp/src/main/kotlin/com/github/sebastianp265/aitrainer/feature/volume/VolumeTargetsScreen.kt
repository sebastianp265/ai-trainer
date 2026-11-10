package com.github.sebastianp265.aitrainer.feature.volume

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.volume.impl.VolumeTargetsContent
import com.github.sebastianp265.aitrainer.feature.volume.impl.VolumeTargetsViewModel
import com.github.sebastianp265.aitrainer.shared.coach.PendingProposalBanner
import com.github.sebastianp265.aitrainer.shared.coach.ProposalScope
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun VolumeTargetsScreen(onOpenCoach: () -> Unit) {
    val viewModel: VolumeTargetsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    VolumeTargetsContent(
        state = state,
        onShowTargetsChange = viewModel::setShowTargets,
        onTargetChange = viewModel::setTarget,
        onRestoreDefaults = viewModel::restoreDefaults,
        banner = { PendingProposalBanner(ProposalScope.TARGETS, onOpenPlanProposal = {}, onOpenCoach = onOpenCoach) },
    )
}
