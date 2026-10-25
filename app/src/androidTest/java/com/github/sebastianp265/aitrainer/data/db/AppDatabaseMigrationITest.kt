package com.github.sebastianp265.aitrainer.data.db

import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.github.sebastianp265.aitrainer.data.db.entity.SetEntryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.runner.RunWith
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationITest {

    private companion object {
        const val DB_NAME = "migration-test.db"
    }

    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper = MigrationTestHelper(instrumentation, AppDatabase::class.java)

    @AfterTest
    fun deleteTestDatabase() {
        instrumentation.targetContext.deleteDatabase(DB_NAME)
    }

    @Test
    fun upgradeFromV1KeepsLoggedSetsWithTheirPositionInSession() {
        helper.createDatabase(DB_NAME, 1).use { v1 ->
            v1.execSQL(
                "INSERT INTO exercise (id, name, primaryMuscles, secondaryMuscles, equipment, preference) " +
                    "VALUES (1, 'Barbell Bench Press', 'CHEST', 'TRICEPS,FRONT_DELTS', 'BARBELL', 'NEUTRAL')",
            )
            v1.execSQL(
                "INSERT INTO workout_session (id, date, startedAt, finishedAt, planSessionRef, notes) " +
                    "VALUES (1, '2026-10-20', 1000, 2000, NULL, NULL)",
            )
            v1.execSQL(
                "INSERT INTO set_entry (id, sessionId, exerciseId, setOrder, reps, weightKg, rpe, isWarmup) " +
                    "VALUES (7, 1, 1, 3, 8, 82.5, NULL, 0)",
            )
        }

        helper.runMigrationsAndValidate(DB_NAME, 2, true).close()

        val db = Room.databaseBuilder(instrumentation.targetContext, AppDatabase::class.java, DB_NAME).build()
        try {
            val sets = runBlocking { db.workoutDao().observeSets(1).first() }
            assertEquals(
                listOf(
                    SetEntryEntity(
                        id = 7,
                        sessionId = 1,
                        exerciseId = 1,
                        positionInSession = 3,
                        reps = 8,
                        weightKg = 82.5,
                        rpe = null,
                        isWarmup = false,
                    ),
                ),
                sets,
            )
        } finally {
            db.close()
        }
    }
}
