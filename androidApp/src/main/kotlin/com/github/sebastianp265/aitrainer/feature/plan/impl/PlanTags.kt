package com.github.sebastianp265.aitrainer.feature.plan.impl

import kotlinx.datetime.LocalDate

internal object PlanTags {
    const val NAME_FIELD = "plan_name"
    const val DATE_FIELD = "plan_date"
    const val SAVE = "plan_save"
    const val ASK_COACH = "plan_ask_coach"
    const val WEEK_STRIP = "plan_week_strip"
    const val START = "plan_start"
    const val ADD_EXERCISE = "plan_add_exercise"
    const val SET_WEIGHT = "plan_set_weight"
    const val SET_REPS = "plan_set_reps"

    fun weekDay(date: LocalDate) = "plan_week_day:$date"
}
