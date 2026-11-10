package com.github.sebastianp265.aitrainer.integration.volume

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface VolumeService {
    fun observeWeeklySets(from: LocalDate, toInclusive: LocalDate): Flow<List<WeeklySets>>

    fun observePlannedSets(from: LocalDate, toInclusive: LocalDate): Flow<Map<String, Double>>

    fun observeTargets(): Flow<List<WeeklyTarget>>

    suspend fun setTarget(groupKey: String, minSets: Int, maxSets: Int, source: TargetSource = TargetSource.USER, reason: String? = null)

    suspend fun restoreDefaultTargets()
}
