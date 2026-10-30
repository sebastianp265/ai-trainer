package com.github.sebastianp265.aitrainer.data.repo

import com.github.sebastianp265.aitrainer.data.db.dao.ProfilePrefsDao
import com.github.sebastianp265.aitrainer.data.db.entity.ProfilePrefsEntity
import com.github.sebastianp265.aitrainer.domain.model.ProfilePrefs
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomProfileRepository(
    private val dao: ProfilePrefsDao,
) : ProfileRepository {
    override fun observe(): Flow<ProfilePrefs> = dao.observe().map { it?.toDomain() ?: ProfilePrefs.DEFAULT }

    override suspend fun modify(change: (ProfilePrefs) -> ProfilePrefs) =
        dao.modifyInTransaction(ProfilePrefs.DEFAULT.toEntity()) { change(it.toDomain()).toEntity() }
}

internal fun ProfilePrefsEntity.toDomain() = ProfilePrefs(
    goal = goal,
    sessionsPerWeek = sessionsPerWeek,
    sessionLengthMin = sessionLengthMin,
    weakMuscleGroups = weakMuscleGroups.toSet(),
    availableEquipment = availableEquipment.toSet(),
    notes = notes,
)

internal fun ProfilePrefs.toEntity() = ProfilePrefsEntity(
    goal = goal,
    sessionsPerWeek = sessionsPerWeek,
    sessionLengthMin = sessionLengthMin,
    weakMuscleGroups = weakMuscleGroups.sorted(),
    availableEquipment = availableEquipment.sorted(),
    notes = notes,
)
