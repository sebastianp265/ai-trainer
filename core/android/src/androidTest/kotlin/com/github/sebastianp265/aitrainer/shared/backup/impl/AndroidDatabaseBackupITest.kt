package com.github.sebastianp265.aitrainer.shared.backup.impl

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith
import java.io.File
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class AndroidDatabaseBackupITest {

    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseFile = context.getDatabasePath("aitrainer.db")
    private val backup = AndroidDatabaseBackup(context)
    private val exported = File(context.cacheDir, "exported-backup.db")

    @BeforeTest
    fun createDatabase() {
        databaseFile.parentFile?.mkdirs()
        databaseFile.delete()
        SQLiteDatabase.openOrCreateDatabase(databaseFile, null).use { database ->
            database.execSQL("CREATE TABLE workout_session (id INTEGER PRIMARY KEY, finishedAt INTEGER)")
            database.execSQL("CREATE TABLE plan (id INTEGER PRIMARY KEY)")
            database.execSQL("CREATE TABLE coach_note (id INTEGER PRIMARY KEY)")
            database.execSQL("CREATE TABLE coach_message (id INTEGER PRIMARY KEY)")
            database.execSQL("INSERT INTO workout_session (id, finishedAt) VALUES (1, 2000), (2, 3000), (3, NULL)")
            database.execSQL("INSERT INTO plan (id) VALUES (1)")
            database.execSQL("INSERT INTO coach_note (id) VALUES (1)")
            database.version = 12
        }
        exported.delete()
    }

    @AfterTest
    fun clean() {
        databaseFile.delete()
        exported.delete()
    }

    @Test
    fun `an exported file shows what the database contained`() = runBlocking {
        assertTrue(backup.export(Uri.fromFile(exported).toString()))

        val contents = assertNotNull(backup.inspect(Uri.fromFile(exported).toString()))

        assertEquals(12, contents.schemaVersion)
        assertEquals(2, contents.workouts)
        assertEquals(1, contents.plans)
        assertEquals(1, contents.coachNotes)
        assertEquals(0, contents.coachMessages)
    }

    @Test
    fun `a file that is not a backup cannot be inspected`() = runBlocking {
        exported.writeText("this is not a database")

        assertNull(backup.inspect(Uri.fromFile(exported).toString()))
    }
}
