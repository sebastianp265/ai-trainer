package com.github.sebastianp265.aitrainer.integration.coach

import kotlinx.coroutines.flow.Flow

interface CoachMemoryService {
    fun observeProfile(): Flow<Map<String, String>>

    suspend fun setProfileField(field: String, value: String)

    fun observeNotes(): Flow<List<CoachNote>>

    suspend fun addNote(text: String): Long

    suspend fun updateNote(id: Long, text: String): Boolean

    suspend fun deleteNote(id: Long): Boolean

    companion object {
        val PROFILE_FIELDS = listOf("goal", "time", "equipment", "limitations", "preferences")
    }
}
