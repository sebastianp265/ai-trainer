package com.github.sebastianp265.aitrainer.shared.coach.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.coach.CoachProposal
import com.github.sebastianp265.aitrainer.integration.coach.CoachService
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal class PendingProposalViewModel(coachService: CoachService, settingsService: SettingsService) : ViewModel() {

    val pending: StateFlow<List<CoachProposal>> = combine(
        coachService.observePendingProposals(),
        settingsService.observeCoachEnabled(),
    ) { proposals, enabled -> if (enabled) proposals else emptyList() }
        .stateIn(viewModelScope, WhileUiSubscribed, emptyList())
}
