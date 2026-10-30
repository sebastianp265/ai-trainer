package com.github.sebastianp265.aitrainer.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.github.sebastianp265.aitrainer.domain.model.Equipment
import com.github.sebastianp265.aitrainer.domain.model.Goal
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup

@Entity(tableName = "profile_prefs")
data class ProfilePrefsEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val goal: Goal,
    val sessionsPerWeek: Int,
    val sessionLengthMin: Int,
    val weakMuscleGroups: List<MuscleGroup>,
    val availableEquipment: List<Equipment>,
    val notes: String,
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
