package com.github.sebastianp265.aitrainer.integration.debug

import kotlinx.coroutines.flow.Flow

interface DebugService {
    fun observeCounts(): Flow<DebugCounts>
}

data class DebugCounts(
    val schemaVersion: Int,
    val workouts: Int,
    val plans: Int,
    val healthDays: Int,
    val coachNotes: Int,
)
