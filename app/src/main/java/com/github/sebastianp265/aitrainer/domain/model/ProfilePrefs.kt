package com.github.sebastianp265.aitrainer.domain.model

data class ProfilePrefs(
    val goal: Goal,
    val sessionsPerWeek: Int,
    val sessionLengthMin: Int,
    val weakMuscleGroups: Set<MuscleGroup>,
    val availableEquipment: Set<Equipment>,
    val notes: String,
) {
    companion object {
        val SESSIONS_PER_WEEK_RANGE = 1..7
        val SESSION_LENGTH_MIN_RANGE = 15..180
        const val SESSION_LENGTH_STEP_MIN = 15

        val DEFAULT = ProfilePrefs(
            goal = Goal.MAINTAIN,
            sessionsPerWeek = 3,
            sessionLengthMin = 60,
            weakMuscleGroups = emptySet(),
            availableEquipment = Equipment.entries.toSet(),
            notes = "",
        )
    }
}
