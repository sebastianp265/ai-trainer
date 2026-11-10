package com.github.sebastianp265.aitrainer.feature.debug.impl

import com.github.sebastianp265.aitrainer.integration.debug.DebugCounts
import com.github.sebastianp265.aitrainer.integration.debug.DebugService
import com.github.sebastianp265.aitrainer.shared.coach.CoachDao
import com.github.sebastianp265.aitrainer.shared.database.DATABASE_SCHEMA_VERSION
import com.github.sebastianp265.aitrainer.shared.health.HealthDao
import com.github.sebastianp265.aitrainer.shared.plan.PlanDao
import com.github.sebastianp265.aitrainer.shared.workout.WorkoutDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

internal class DefaultDebugService(
    private val workoutDao: WorkoutDao,
    private val planDao: PlanDao,
    private val healthDao: HealthDao,
    private val coachDao: CoachDao,
) : DebugService {

    override fun observeCounts(): Flow<DebugCounts> = combine(
        workoutDao.observeFinishedCount(),
        planDao.observePlanCount(),
        healthDao.observeDayCount(),
        coachDao.observeNoteCount(),
    ) { workouts, plans, days, notes -> DebugCounts(DATABASE_SCHEMA_VERSION, workouts, plans, days, notes) }
}
