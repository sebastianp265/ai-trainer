package com.github.sebastianp265.aitrainer.data.repo

import com.github.sebastianp265.aitrainer.domain.model.ProfilePrefs
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    fun observe(): Flow<ProfilePrefs>
    suspend fun modify(change: (ProfilePrefs) -> ProfilePrefs)
}
