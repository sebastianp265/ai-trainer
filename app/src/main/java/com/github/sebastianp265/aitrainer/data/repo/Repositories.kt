package com.github.sebastianp265.aitrainer.data.repo

import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.ExercisePreference
import com.github.sebastianp265.aitrainer.domain.model.ProfilePrefs
import com.github.sebastianp265.aitrainer.domain.model.SessionSummary
import com.github.sebastianp265.aitrainer.domain.model.SetEntry
import com.github.sebastianp265.aitrainer.domain.model.WorkoutSession
import com.github.sebastianp265.aitrainer.domain.model.DailyEnergy
import com.github.sebastianp265.aitrainer.domain.model.HealthDayTotals
import com.github.sebastianp265.aitrainer.domain.model.LoggedSet
import com.github.sebastianp265.aitrainer.domain.model.WeightMeasurement
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlin.time.Instant

interface ExerciseRepository {
    fun observeExercises(): Flow<List<Exercise>>

    suspend fun ensureSeeded()

    suspend fun setPreference(exerciseId: Long, preference: ExercisePreference)
}

interface ProfileRepository {
    fun observe(): Flow<ProfilePrefs>
    suspend fun modify(change: (ProfilePrefs) -> ProfilePrefs)
}

interface AppSettingsRepository {
    fun observeClaudeModel(): Flow<String>
    suspend fun setClaudeModel(modelId: String)

    companion object {
        const val DEFAULT_CLAUDE_MODEL = "claude-sonnet-5-5"
    }
}

interface WorkoutRepository {
    fun observeActiveSession(): Flow<WorkoutSession?>
    fun observeSets(sessionId: Long): Flow<List<SetEntry>>
    fun observeRecentSessions(limit: Int = 20): Flow<List<SessionSummary>>

    suspend fun startSession(planSessionRef: String? = null): Long
    suspend fun finishSession(sessionId: Long)
    suspend fun discardSession(sessionId: Long)

    suspend fun addSet(sessionId: Long, exerciseId: Long, reps: Int, weightKg: Double, isWarmup: Boolean = false): Long
    suspend fun modifySet(setId: Long, change: (SetEntry) -> SetEntry)
    suspend fun deleteSet(setId: Long)

    suspend fun lastSetsFor(exerciseId: Long, excludingSessionId: Long): List<SetEntry>

    fun observeFinishedSetsSince(from: LocalDate): Flow<List<LoggedSet>>
}

interface EnergyRepository {
    fun observeRange(from: LocalDate, toInclusive: LocalDate): Flow<List<DailyEnergy>>
    suspend fun replaceHealthConnectDays(from: LocalDate, toInclusive: LocalDate, days: List<HealthDayTotals>)
}

interface BodyWeightRepository {
    fun observeSince(from: Instant): Flow<List<WeightMeasurement>>
    suspend fun addManualMeasurement(weightKg: Double)
    suspend fun delete(id: Long)
}
