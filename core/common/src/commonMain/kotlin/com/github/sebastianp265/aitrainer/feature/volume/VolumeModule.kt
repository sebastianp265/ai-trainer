package com.github.sebastianp265.aitrainer.feature.volume

import com.github.sebastianp265.aitrainer.feature.volume.impl.DefaultVolumeService
import com.github.sebastianp265.aitrainer.integration.volume.VolumeService
import org.koin.core.module.dsl.bind
import org.koin.core.module.dsl.factoryOf
import org.koin.dsl.module

internal val volumeModule = module {
    factoryOf(::DefaultVolumeService) { bind<VolumeService>() }
}
