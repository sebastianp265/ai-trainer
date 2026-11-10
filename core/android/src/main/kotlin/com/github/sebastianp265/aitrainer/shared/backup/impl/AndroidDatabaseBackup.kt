package com.github.sebastianp265.aitrainer.shared.backup.impl

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Process
import com.github.sebastianp265.aitrainer.integration.shared.BackupContents
import com.github.sebastianp265.aitrainer.integration.shared.DatabaseBackup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class AndroidDatabaseBackup(private val context: Context) : DatabaseBackup {

    private companion object {
        const val DATABASE_NAME = "aitrainer.db"
        const val SNAPSHOT_NAME = "backup-snapshot.db"
        const val INSPECT_NAME = "backup-inspect.db"
    }

    override suspend fun export(destination: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val snapshot = File(context.cacheDir, SNAPSHOT_NAME).also { it.delete() }
            SQLiteDatabase.openDatabase(context.getDatabasePath(DATABASE_NAME).path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
                database.execSQL("VACUUM INTO '${snapshot.path}'")
            }
            checkNotNull(context.contentResolver.openOutputStream(Uri.parse(destination))).use { output ->
                snapshot.inputStream().use { it.copyTo(output) }
            }
            snapshot.delete()
        }.isSuccess
    }

    override suspend fun inspect(source: String): BackupContents? = withContext(Dispatchers.IO) {
        runCatching {
            val copy = copyToCache(source, INSPECT_NAME)
            try {
                SQLiteDatabase.openDatabase(copy.path, null, SQLiteDatabase.OPEN_READONLY).use { database ->
                    BackupContents(
                        schemaVersion = database.version,
                        workouts = database.count("workout_session WHERE finishedAt IS NOT NULL"),
                        plans = database.count("plan"),
                        healthDays = database.count("health_day"),
                        coachNotes = database.count("coach_note"),
                        coachMessages = database.count("coach_message"),
                    )
                }
            } finally {
                copy.delete()
            }
        }.getOrNull()
    }

    override suspend fun import(source: String): Boolean = withContext(Dispatchers.IO) {
        val copy = runCatching { copyToCache(source, INSPECT_NAME) }.getOrNull() ?: return@withContext false
        val target = context.getDatabasePath(DATABASE_NAME)
        runCatching {
            File(target.path + "-wal").delete()
            File(target.path + "-shm").delete()
            copy.copyTo(target, overwrite = true)
            copy.delete()
        }.onFailure { return@withContext false }
        restartApp()
        true
    }

    private fun copyToCache(source: String, name: String): File {
        val file = File(context.cacheDir, name).also { it.delete() }
        checkNotNull(context.contentResolver.openInputStream(Uri.parse(source))).use { input ->
            file.outputStream().use { input.copyTo(it) }
        }
        return file
    }

    private fun SQLiteDatabase.count(tableAndCondition: String): Int =
        rawQuery("SELECT COUNT(*) FROM $tableAndCondition", null).use { cursor ->
            cursor.moveToFirst()
            cursor.getInt(0)
        }

    private fun restartApp() {
        val launch = checkNotNull(context.packageManager.getLaunchIntentForPackage(context.packageName))
        context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK))
        Process.killProcess(Process.myPid())
    }
}
