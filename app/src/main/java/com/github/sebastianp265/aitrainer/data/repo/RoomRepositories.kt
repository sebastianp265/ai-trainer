package com.github.sebastianp265.aitrainer.data.repo

import com.github.sebastianp265.aitrainer.data.db.SeedExercises
import com.github.sebastianp265.aitrainer.data.db.dao.BodyWeightDao
import com.github.sebastianp265.aitrainer.data.db.dao.DailyEnergyDao
import com.github.sebastianp265.aitrainer.data.db.dao.ExerciseDao
import com.github.sebastianp265.aitrainer.data.db.dao.ProfilePrefsDao
import com.github.sebastianp265.aitrainer.data.db.dao.SessionSummaryRow
import com.github.sebastianp265.aitrainer.data.db.dao.WorkoutDao
import com.github.sebastianp265.aitrainer.data.db.entity.BodyWeightEntity
import com.github.sebastianp265.aitrainer.data.db.entity.DailyEnergyEntity
import com.github.sebastianp265.aitrainer.data.db.entity.ExerciseEntity
import com.github.sebastianp265.aitrainer.data.db.entity.ProfilePrefsEntity
import com.github.sebastianp265.aitrainer.data.db.entity.SetEntryEntity
import com.github.sebastianp265.aitrainer.data.db.entity.WorkoutSessionEntity
import com.github.sebastianp265.aitrainer.domain.model.DailyEnergy
import com.github.sebastianp265.aitrainer.domain.model.Exercise
import com.github.sebastianp265.aitrainer.domain.model.HealthDayTotals
import com.github.sebastianp265.aitrainer.domain.model.LoggedSet
import com.github.sebastianp265.aitrainer.domain.model.WeightMeasurement
import com.github.sebastianp265.aitrainer.domain.model.WeightSource
import com.github.sebastianp265.aitrainer.domain.stats.applyHealthConnectDay
import com.github.sebastianp265.aitrainer.domain.model.ExercisePreference
import com.github.sebastianp265.aitrainer.domain.model.ProfilePrefs
import com.github.sebastianp265.aitrainer.domain.model.SessionSummary
import com.github.sebastianp265.aitrainer.domain.model.SetEntry
import com.github.sebastianp265.aitrainer.domain.model.WorkoutSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Instant

class RoomExerciseRepository(
    private val dao: ExerciseDao,
) : ExerciseRepository {
    override fun observeExercises(): Flow<List<Exercise>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun ensureSeeded() = dao.insertAllIgnoringExisting(SeedExercises.all)

    override suspend fun setPreference(exerciseId: Long, preference: ExercisePreference) =
        dao.setPreference(exerciseId, preference)
}

class RoomProfileRepository(
    private val dao: ProfilePrefsDao,
) : ProfileRepository {
    override fun observe(): Flow<ProfilePrefs> = dao.observe().map { it?.toDomain() ?: ProfilePrefs.DEFAULT }

    override suspend fun modify(change: (ProfilePrefs) -> ProfilePrefs) =
        dao.modifyInTransaction(ProfilePrefs.DEFAULT.toEntity()) { change(it.toDomain()).toEntity() }
}

class RoomWorkoutRepository(
    private val dao: WorkoutDao,
    private val clock: Clock,
) : WorkoutRepository {
    override fun observeActiveSession(): Flow<WorkoutSession?> =
        dao.observeActiveSession().map { it?.toDomain() }

    override fun observeSets(sessionId: Long): Flow<List<SetEntry>> =
        dao.observeSets(sessionId).map { list -> list.map { it.toDomain() } }

    override fun observeRecentSessions(limit: Int): Flow<List<SessionSummary>> =
        dao.observeRecentSessions(limit).map { list -> list.map { it.toDomain() } }

    override suspend fun startSession(planSessionRef: String?): Long =
        dao.insertSession(
            WorkoutSessionEntity(
                date = clock.todayIn(TimeZone.currentSystemDefault()),
                startedAt = clock.now(),
                finishedAt = null,
                planSessionRef = planSessionRef,
                notes = null,
            )
        )

    override suspend fun finishSession(sessionId: Long) {
        val session = dao.getSession(sessionId) ?: return
        dao.updateSession(session.copy(finishedAt = clock.now()))
    }

    override suspend fun discardSession(sessionId: Long) = dao.deleteSession(sessionId)

    override suspend fun addSet(
        sessionId: Long,
        exerciseId: Long,
        reps: Int,
        weightKg: Double,
        isWarmup: Boolean,
    ): Long = dao.insertSetAtEnd(
        SetEntryEntity(
            sessionId = sessionId,
            exerciseId = exerciseId,
            positionInSession = 0,
            reps = reps,
            weightKg = weightKg,
            rpe = null,
            isWarmup = isWarmup,
        )
    )

    override suspend fun modifySet(setId: Long, change: (SetEntry) -> SetEntry) =
        dao.modifySetInTransaction(setId) { change(it.toDomain()).toEntity() }

    override suspend fun deleteSet(setId: Long) = dao.deleteSet(setId)

    override suspend fun lastSetsFor(exerciseId: Long, excludingSessionId: Long): List<SetEntry> =
        dao.lastSetsForExercise(exerciseId, excludingSessionId).map { it.toDomain() }

    override fun observeFinishedSetsSince(from: LocalDate): Flow<List<LoggedSet>> =
        dao.observeFinishedSetsSince(from).map { rows ->
            rows.map { LoggedSet(it.date, it.primaryMuscles, it.secondaryMuscles, it.isWarmup) }
        }
}

class RoomEnergyRepository(
    private val dao: DailyEnergyDao,
    private val clock: Clock,
) : EnergyRepository {
    override fun observeRange(from: LocalDate, toInclusive: LocalDate): Flow<List<DailyEnergy>> =
        dao.observeRange(from, toInclusive).map { list -> list.map { it.toDomain() } }

    override suspend fun replaceHealthConnectDays(from: LocalDate, toInclusive: LocalDate, days: List<HealthDayTotals>) {
        val healthByDate = days.associateBy { it.date }
        val now = clock.now()
        dao.replaceDays(datesBetween(from, toInclusive)) { date, existing ->
            applyHealthConnectDay(date, existing?.toDomain(), healthByDate[date])?.toEntity(updatedAt = now)
        }
    }

    private fun datesBetween(from: LocalDate, toInclusive: LocalDate): List<LocalDate> =
        generateSequence(from) { it.plus(DatePeriod(days = 1)) }.takeWhile { it <= toInclusive }.toList()
}

class RoomBodyWeightRepository(
    private val dao: BodyWeightDao,
    private val clock: Clock,
) : BodyWeightRepository {
    override fun observeSince(from: Instant): Flow<List<WeightMeasurement>> =
        dao.observeSince(from).map { list -> list.map { it.toDomain() } }

    override suspend fun addManualMeasurement(weightKg: Double) {
        dao.insertIgnoringDuplicates(BodyWeightEntity(measuredAt = clock.now(), weightKg = weightKg, source = WeightSource.MANUAL))
    }

    override suspend fun delete(id: Long) = dao.delete(id)
}

internal fun ExerciseEntity.toDomain() = Exercise(id, name, primaryMuscles, secondaryMuscles, equipment, preference)

internal fun WorkoutSessionEntity.toDomain() = WorkoutSession(id, date, startedAt, finishedAt, planSessionRef, notes)

internal fun SetEntryEntity.toDomain() = SetEntry(id, sessionId, exerciseId, positionInSession, reps, weightKg, rpe, isWarmup)

internal fun SetEntry.toEntity() = SetEntryEntity(id, sessionId, exerciseId, positionInSession, reps, weightKg, rpe, isWarmup)

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

internal fun DailyEnergyEntity.toDomain() =
    DailyEnergy(date, kcalBurned, kcalBurnedSource, kcalEaten, proteinG, carbsG, fatG)

internal fun DailyEnergy.toEntity(updatedAt: Instant) =
    DailyEnergyEntity(date, kcalBurned, kcalBurnedSource, kcalEaten, proteinG, carbsG, fatG, updatedAt)

internal fun BodyWeightEntity.toDomain() = WeightMeasurement(id, measuredAt, weightKg, source)

internal fun SessionSummaryRow.toDomain() = SessionSummary(id, date, startedAt, finishedAt, exerciseCount, setCount)
