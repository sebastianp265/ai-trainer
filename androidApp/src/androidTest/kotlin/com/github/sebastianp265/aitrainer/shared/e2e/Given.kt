package com.github.sebastianp265.aitrainer.shared.e2e

import com.github.sebastianp265.aitrainer.integration.plan.PlanAuthor
import com.github.sebastianp265.aitrainer.integration.plan.PlanDraft
import com.github.sebastianp265.aitrainer.integration.plan.PlanService
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.integration.settings.Secret
import com.github.sebastianp265.aitrainer.integration.settings.SecretStore
import com.github.sebastianp265.aitrainer.integration.workout.SetChange
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import org.koin.core.context.GlobalContext
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module
import kotlin.time.Clock
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

const val BENCH_PRESS = "BARBELL_BENCH_PRESS"
const val BACK_SQUAT = "BACK_SQUAT"

fun sets(exerciseKey: String, count: Int = 1, reps: Int = 10, kg: Double = 0.0, rpe: Double? = null, warmup: Boolean = false) =
    List(count) { PlannedSet(exerciseKey, reps, kg, rpe, warmup) }

class Given {

    private val koin get() = GlobalContext.get()

    fun aiApiKey() = runBlocking { koin.get<SecretStore>().write(Secret.CLAUDE_API_KEY, "test-key") }

    fun plan(name: String, sets: List<PlannedSet>): Long = runBlocking {
        koin.get<PlanService>().createPlan(PlanDraft(name, sets), PlanAuthor.USER)
    }

    fun finishedWorkout(sets: List<PlannedSet>, on: LocalDate? = null) = runBlocking {
        val workouts = workoutServiceOn(on)
        val sessionId = startWith(workouts, sets)
        workouts.finishSession(sessionId)
    }

    fun runningWorkout(sets: List<PlannedSet>) = runBlocking { startWith(koin.get(), sets) }

    fun runningWorkoutFromPlan(planId: Long, removedSetIndex: Int) = runBlocking {
        val workouts = koin.get<WorkoutService>()
        workouts.startSession(planId)
        val sessionId = workouts.observeActiveSession().first()!!.id
        workouts.deleteSet(workouts.observeSets(sessionId).first()[removedSetIndex].id)
    }

    private suspend fun startWith(workouts: WorkoutService, sets: List<PlannedSet>): Long {
        workouts.startSession()
        val sessionId = workouts.observeActiveSession().first()!!.id
        sets.forEach { workouts.addExercise(sessionId, it.exerciseKey) }
        workouts.observeSets(sessionId).first().zip(sets) { stored, wanted ->
            workouts.changeSet(stored.id, SetChange.RepsTo(wanted.reps))
            workouts.changeSet(stored.id, SetChange.WeightTo(wanted.weightKg))
            workouts.changeSet(stored.id, SetChange.RpeTo(wanted.rpe))
            if (wanted.isWarmup) workouts.changeSet(stored.id, SetChange.ToggleWarmup)
        }
        return sessionId
    }

    private fun workoutServiceOn(date: LocalDate?): WorkoutService {
        if (date == null) return koin.get()
        val noonOfDate = date.atStartOfDayIn(TimeZone.currentSystemDefault()) + 12.hours
        loadKoinModules(module { single<Clock> { FixedClock(noonOfDate) } })
        return koin.get<WorkoutService>().also { loadKoinModules(module { single<Clock> { Clock.System } }) }
    }

    private class FixedClock(private val instant: Instant) : Clock {
        override fun now() = instant
    }
}
