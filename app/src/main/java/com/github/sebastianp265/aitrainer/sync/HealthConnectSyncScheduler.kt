package com.github.sebastianp265.aitrainer.sync

import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

class HealthConnectSyncScheduler(private val workManager: WorkManager) {

    private companion object {
        const val IMMEDIATE_SYNC = "health_connect_immediate_sync"
    }

    fun syncNow() {
        workManager.enqueueUniqueWork(
            IMMEDIATE_SYNC,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<HealthConnectSyncWorker>().build(),
        )
    }
}
