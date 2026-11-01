package com.github.sebastianp265.aitrainer.shared.platform

import com.github.sebastianp265.aitrainer.integration.shared.BundledFiles
import com.github.sebastianp265.aitrainer.integration.shared.DatabaseBuilders
import com.github.sebastianp265.aitrainer.integration.shared.PreferencesStores
import com.github.sebastianp265.aitrainer.shared.platform.impl.AndroidBundledFiles
import com.github.sebastianp265.aitrainer.shared.platform.impl.AndroidDatabaseBuilders
import com.github.sebastianp265.aitrainer.shared.platform.impl.AndroidPreferencesStores
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal val platformModule = module {
    single<DatabaseBuilders> { AndroidDatabaseBuilders(androidContext()) }
    single<PreferencesStores> { AndroidPreferencesStores(androidContext()) }
    single<BundledFiles> { AndroidBundledFiles() }
}

