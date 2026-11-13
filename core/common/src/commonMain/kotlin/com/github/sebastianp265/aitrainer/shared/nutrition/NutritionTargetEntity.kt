package com.github.sebastianp265.aitrainer.shared.nutrition

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.datetime.LocalDate

private const val NUTRITION_TARGET_ROW = 1

@Entity(tableName = "nutrition_target")
internal data class NutritionTargetEntity(
    @PrimaryKey val id: Int = NUTRITION_TARGET_ROW,
    val kcal: Int?,
    val proteinG: Int?,
    val setOn: LocalDate,
    val reason: String?,
)
