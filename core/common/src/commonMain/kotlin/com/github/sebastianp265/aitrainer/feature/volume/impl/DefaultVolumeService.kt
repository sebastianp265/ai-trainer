package com.github.sebastianp265.aitrainer.feature.volume.impl

import com.github.sebastianp265.aitrainer.integration.volume.DEFAULT_MAX_WEEKLY_SETS
import com.github.sebastianp265.aitrainer.integration.volume.DEFAULT_MIN_WEEKLY_SETS
import com.github.sebastianp265.aitrainer.integration.volume.TargetSource
import com.github.sebastianp265.aitrainer.integration.volume.VolumeService
import com.github.sebastianp265.aitrainer.integration.volume.WeeklySets
import com.github.sebastianp265.aitrainer.integration.volume.WeeklyTarget
import com.github.sebastianp265.aitrainer.shared.volume.VolumeDao
import com.github.sebastianp265.aitrainer.shared.volume.WeeklyTargetEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import kotlin.time.Clock

internal class DefaultVolumeService(
    private val dao: VolumeDao,
    private val clock: Clock,
) : VolumeService {

    override fun observeWeeklySets(from: LocalDate, toInclusive: LocalDate): Flow<List<WeeklySets>> =
        dao.observeDoneSets(mondayOf(from), toInclusive).map { weeklySets(it) }

    override fun observePlannedSets(from: LocalDate, toInclusive: LocalDate): Flow<Map<String, Double>> =
        dao.observePlannedSets(from, toInclusive).map { rows -> weeklySets(rows).fold(emptyMap()) { total, week -> total.plusSets(week.setsByGroup) } }

    override fun observeTargets(): Flow<List<WeeklyTarget>> = dao.observeTargets().map { stored ->
        val byGroup = stored.associateBy { it.muscleGroupKey }
        MUSCLE_GROUPS_IN_ORDER.map { key -> byGroup[key]?.toModel() ?: defaultTarget(key) }
    }

    override suspend fun setTarget(groupKey: String, minSets: Int, maxSets: Int, source: TargetSource, reason: String?) {
        require(minSets in 0..maxSets) { "A target range needs 0 <= min <= max" }
        dao.upsertTarget(WeeklyTargetEntity(groupKey, minSets, maxSets, source, clock.todayIn(TimeZone.currentSystemDefault()), reason))
    }

    override suspend fun restoreDefaultTargets() = dao.deleteAllTargets()
}

internal val MUSCLE_GROUPS_IN_ORDER = listOf("CHEST", "BACK", "LEGS", "SHOULDERS", "ARMS", "ABS")

private fun defaultTarget(groupKey: String) =
    WeeklyTarget(groupKey, DEFAULT_MIN_WEEKLY_SETS, DEFAULT_MAX_WEEKLY_SETS, TargetSource.DEFAULT, date = null, reason = null)

private fun WeeklyTargetEntity.toModel() = WeeklyTarget(muscleGroupKey, minSets, maxSets, source, setOn, reason)

private fun Map<String, Double>.plusSets(other: Map<String, Double>): Map<String, Double> =
    (keys + other.keys).associateWith { (this[it] ?: 0.0) + (other[it] ?: 0.0) }
