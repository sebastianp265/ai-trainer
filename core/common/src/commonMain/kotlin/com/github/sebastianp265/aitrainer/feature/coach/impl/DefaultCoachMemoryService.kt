package com.github.sebastianp265.aitrainer.feature.coach.impl

import com.github.sebastianp265.aitrainer.integration.coach.CoachMemoryService
import com.github.sebastianp265.aitrainer.integration.coach.CoachNote
import com.github.sebastianp265.aitrainer.shared.coach.CoachDao
import com.github.sebastianp265.aitrainer.shared.coach.CoachNoteEntity
import com.github.sebastianp265.aitrainer.shared.coach.CoachProfileEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlin.time.Clock

internal class DefaultCoachMemoryService(
    private val dao: CoachDao,
    private val clock: Clock,
) : CoachMemoryService {

    override fun observeProfile(): Flow<Map<String, String>> = dao.observeProfile().map { list -> list.associate { it.field to it.value } }

    override suspend fun setProfileField(field: String, value: String) {
        require(field in CoachMemoryService.PROFILE_FIELDS) { "field must be one of ${CoachMemoryService.PROFILE_FIELDS.joinToString()}" }
        if (value.isBlank()) dao.deleteProfileField(field) else dao.upsertProfile(CoachProfileEntity(field, value.trim()))
    }

    override fun observeNotes(): Flow<List<CoachNote>> = dao.observeNotes().map { list -> list.map { CoachNote(it.id, it.text, it.createdAt) } }

    override suspend fun addNote(text: String): Long {
        require(text.isNotBlank()) { "text is empty" }
        return dao.insertNote(CoachNoteEntity(text = text.trim(), createdAt = clock.now()))
    }

    override suspend fun updateNote(id: Long, text: String): Boolean = dao.updateNote(id, text.trim()) > 0

    override suspend fun deleteNote(id: Long): Boolean = dao.deleteNote(id) > 0
}
