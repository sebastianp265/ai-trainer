package com.github.sebastianp265.aitrainer.feature.coach.impl

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.integration.coach.CoachReadiness
import com.github.sebastianp265.aitrainer.integration.coach.CoachService
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

internal class WeeklyReviewViewModel(
    private val coachService: CoachService,
    private val settingsService: SettingsService,
) : ViewModel() {

    fun reviewIfDue() {
        viewModelScope.launch {
            withContext(NonCancellable) {
                if (settingsService.observeCoachEnabled().first() && coachService.observeReadiness().first() == CoachReadiness.READY) {
                    coachService.reviewLastWeekIfDue()
                }
            }
        }
    }
}
