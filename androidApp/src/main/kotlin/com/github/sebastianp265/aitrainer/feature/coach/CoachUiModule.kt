package com.github.sebastianp265.aitrainer.feature.coach

import com.github.sebastianp265.aitrainer.feature.coach.impl.ApiKeyViewModel
import com.github.sebastianp265.aitrainer.feature.coach.impl.CoachViewModel
import com.github.sebastianp265.aitrainer.feature.coach.impl.MemoryViewModel
import com.github.sebastianp265.aitrainer.feature.coach.impl.ModelPickerViewModel
import com.github.sebastianp265.aitrainer.feature.coach.impl.ProposalPreviewViewModel
import com.github.sebastianp265.aitrainer.feature.coach.impl.WeeklyReviewViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val coachUiModule = module {
    viewModelOf(::CoachViewModel)
    viewModelOf(::MemoryViewModel)
    viewModelOf(::ProposalPreviewViewModel)
    viewModelOf(::ApiKeyViewModel)
    viewModelOf(::ModelPickerViewModel)
    viewModelOf(::WeeklyReviewViewModel)
}
