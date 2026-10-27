package com.github.sebastianp265.aitrainer.sync

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CancellationException

class HealthConnectSyncWorker(
    context: Context,
    params: WorkerParameters,
    private val healthConnectSync: HealthConnectSync,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        healthConnectSync.syncRecentDays()
        Result.success()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Result.retry()
    }
}
