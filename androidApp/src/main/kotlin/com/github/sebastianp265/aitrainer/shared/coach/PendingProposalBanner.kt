package com.github.sebastianp265.aitrainer.shared.coach

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.coach.CoachProposal
import com.github.sebastianp265.aitrainer.integration.coach.ProposalType
import com.github.sebastianp265.aitrainer.shared.coach.impl.PendingProposalViewModel
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val pendingProposalUiModule = module {
    viewModelOf(::PendingProposalViewModel)
}

internal enum class ProposalScope { PLANS, TARGETS }

@Composable
internal fun PendingProposalBanner(scope: ProposalScope, onOpenPlanProposal: (Long) -> Unit, onOpenCoach: () -> Unit) {
    val viewModel: PendingProposalViewModel = koinViewModel()
    val pending by viewModel.pending.collectAsStateWithLifecycle()
    val proposal = pending.lastOrNull {
        when (scope) {
            ProposalScope.PLANS -> it.plans.isNotEmpty()
            ProposalScope.TARGETS -> it.type == ProposalType.TARGET
        }
    } ?: return
    PendingProposalCard(
        proposal,
        onClick = {
            when (scope) {
                ProposalScope.PLANS -> onOpenPlanProposal(proposal.id)
                ProposalScope.TARGETS -> onOpenCoach()
            }
        },
    )
}

@Composable
private fun PendingProposalCard(proposal: CoachProposal, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().clickable(onClick = onClick).testTag("pending_proposal"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.pending_proposal_title), style = MaterialTheme.typography.labelMedium)
                Text(proposal.summary, style = MaterialTheme.typography.bodyMedium)
            }
            Text(stringResource(R.string.pending_proposal_open), style = MaterialTheme.typography.labelLarge)
        }
    }
}
