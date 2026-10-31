package com.github.sebastianp265.aitrainer.feature.exercise.data

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase

class ExerciseLibraryCallback : RoomDatabase.Callback() {

    private var tablesWereRecreated = false

    override fun onCreate(db: SupportSQLiteDatabase) = insertLibrary(db)

    override fun onDestructiveMigration(db: SupportSQLiteDatabase) {
        tablesWereRecreated = true
    }

    override fun onOpen(db: SupportSQLiteDatabase) {
        if (tablesWereRecreated) {
            tablesWereRecreated = false
            insertLibrary(db)
        }
    }

    private fun insertLibrary(db: SupportSQLiteDatabase) {
        val converters = ExerciseConverters()
        ExerciseLibrary.all.forEach { exercise ->
            val row = ContentValues().apply {
                put("key", exercise.key.name)
                put("primaryMuscles", converters.muscleGroupsToString(exercise.primaryMuscles))
                put("secondaryMuscles", converters.muscleGroupsToString(exercise.secondaryMuscles))
                put("equipment", exercise.equipment.name)
            }
            db.insert("exercise", SQLiteDatabase.CONFLICT_ABORT, row)
        }
    }
}
