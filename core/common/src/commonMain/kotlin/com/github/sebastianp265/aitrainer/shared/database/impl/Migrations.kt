package com.github.sebastianp265.aitrainer.shared.database.impl

import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL

internal val migration7To8 = object : Migration(7, 8) {
    override fun migrate(connection: SQLiteConnection) {
        listOf(
            "INSERT OR IGNORE INTO muscle_group (key) VALUES ('BACK'), ('LEGS'), ('SHOULDERS'), ('ARMS')",
            """
            INSERT OR REPLACE INTO muscle_group_translation (muscleGroupKey, language, name) VALUES
            ('CHEST', 'en', 'Chest'), ('CHEST', 'pl', 'Klatka'), ('BACK', 'en', 'Back'), ('BACK', 'pl', 'Plecy'),
            ('LEGS', 'en', 'Legs'), ('LEGS', 'pl', 'Nogi'), ('SHOULDERS', 'en', 'Shoulders'), ('SHOULDERS', 'pl', 'Barki'),
            ('ARMS', 'en', 'Arms'), ('ARMS', 'pl', 'Ramiona'), ('ABS', 'en', 'Abs'), ('ABS', 'pl', 'Brzuch')
            """,
            "CREATE TEMP TABLE muscle_map (oldKey TEXT PRIMARY KEY, newKey TEXT NOT NULL)",
            """
            INSERT INTO muscle_map (oldKey, newKey) VALUES
            ('CHEST', 'CHEST'), ('LATS', 'BACK'), ('UPPER_BACK', 'BACK'), ('LOWER_BACK', 'BACK'), ('TRAPS', 'BACK'),
            ('FRONT_DELTS', 'SHOULDERS'), ('SIDE_DELTS', 'SHOULDERS'), ('REAR_DELTS', 'SHOULDERS'),
            ('BICEPS', 'ARMS'), ('TRICEPS', 'ARMS'), ('FOREARMS', 'ARMS'), ('ABS', 'ABS'), ('OBLIQUES', 'ABS'),
            ('GLUTES', 'LEGS'), ('QUADS', 'LEGS'), ('HAMSTRINGS', 'LEGS'), ('ADDUCTORS', 'LEGS'), ('CALVES', 'LEGS')
            """,
            """
            CREATE TEMP TABLE new_primary AS
            SELECT em.exerciseKey AS exerciseKey, m.newKey AS muscleGroupKey, 1 AS isPrimary, MIN(em.rowid) AS firstRow
            FROM exercise_muscle em JOIN muscle_map m ON m.oldKey = em.muscleGroupKey
            WHERE em.isPrimary = 1
            GROUP BY em.exerciseKey
            """,
            """
            CREATE TEMP TABLE new_secondary AS
            SELECT DISTINCT em.exerciseKey AS exerciseKey, m.newKey AS muscleGroupKey, 0 AS isPrimary
            FROM exercise_muscle em JOIN muscle_map m ON m.oldKey = em.muscleGroupKey
            WHERE NOT EXISTS (
                SELECT 1 FROM new_primary p WHERE p.exerciseKey = em.exerciseKey AND p.muscleGroupKey = m.newKey
            )
            """,
            "DELETE FROM exercise_muscle",
            "INSERT INTO exercise_muscle (exerciseKey, muscleGroupKey, isPrimary) SELECT exerciseKey, muscleGroupKey, isPrimary FROM new_primary ORDER BY firstRow",
            "INSERT INTO exercise_muscle (exerciseKey, muscleGroupKey, isPrimary) SELECT exerciseKey, muscleGroupKey, isPrimary FROM new_secondary",
            "DELETE FROM muscle_group_translation WHERE muscleGroupKey NOT IN ('CHEST', 'BACK', 'LEGS', 'SHOULDERS', 'ARMS', 'ABS')",
            "DELETE FROM muscle_group WHERE key NOT IN ('CHEST', 'BACK', 'LEGS', 'SHOULDERS', 'ARMS', 'ABS')",
            "INSERT OR IGNORE INTO equipment (key) VALUES ('OTHER')",
            "INSERT OR IGNORE INTO equipment_translation (equipmentKey, language, name) VALUES ('OTHER', 'en', 'Other'), ('OTHER', 'pl', 'Inne')",
            "DROP TABLE new_secondary",
            "DROP TABLE new_primary",
            "DROP TABLE muscle_map",
        ).forEach(connection::execSQL)
    }
}

internal val allMigrations = arrayOf(migration7To8)
