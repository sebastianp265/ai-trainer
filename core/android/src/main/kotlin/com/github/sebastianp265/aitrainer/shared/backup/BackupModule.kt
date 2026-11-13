package com.github.sebastianp265.aitrainer.shared.backup

import com.github.sebastianp265.aitrainer.integration.shared.DatabaseBackup
import com.github.sebastianp265.aitrainer.shared.backup.impl.AndroidDatabaseBackup
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

internal val backupModule = module {
    single<DatabaseBackup> { AndroidDatabaseBackup(androidContext()) }
}
