package com.github.sebastianp265.aitrainer.feature.volume

import com.github.sebastianp265.aitrainer.feature.volume.impl.VolumeTargetsViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

internal val volumeUiModule = module {
    viewModelOf(::VolumeTargetsViewModel)
}
