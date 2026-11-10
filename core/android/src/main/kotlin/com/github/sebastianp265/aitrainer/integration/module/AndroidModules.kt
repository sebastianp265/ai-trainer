package com.github.sebastianp265.aitrainer.integration.module

import com.github.sebastianp265.aitrainer.shared.health.healthDataSourceModule
import com.github.sebastianp265.aitrainer.shared.backup.backupModule
import com.github.sebastianp265.aitrainer.shared.network.networkModule
import com.github.sebastianp265.aitrainer.shared.platform.platformModule
import com.github.sebastianp265.aitrainer.shared.security.securityModule

val androidModules = listOf(
    platformModule,
    securityModule,
    healthDataSourceModule,
    networkModule,
    backupModule,
)

