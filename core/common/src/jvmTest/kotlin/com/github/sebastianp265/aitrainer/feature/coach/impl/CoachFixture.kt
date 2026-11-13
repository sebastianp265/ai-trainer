package com.github.sebastianp265.aitrainer.feature.coach.impl

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.github.sebastianp265.aitrainer.feature.exercise.impl.DefaultExerciseService
import com.github.sebastianp265.aitrainer.feature.health.impl.DefaultHealthService
import com.github.sebastianp265.aitrainer.feature.health.impl.FakeHealthDataSource
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthReloads
import com.github.sebastianp265.aitrainer.feature.nutrition.impl.DefaultNutritionService
import com.github.sebastianp265.aitrainer.feature.plan.impl.DefaultPlanService
import com.github.sebastianp265.aitrainer.feature.settings.impl.DefaultSettingsService
import com.github.sebastianp265.aitrainer.feature.volume.impl.DefaultVolumeService
import com.github.sebastianp265.aitrainer.feature.workout.impl.DefaultWorkoutService
import com.github.sebastianp265.aitrainer.integration.coach.CoachFailure
import com.github.sebastianp265.aitrainer.integration.coach.CoachReadiness
import com.github.sebastianp265.aitrainer.integration.coach.CoachReply
import com.github.sebastianp265.aitrainer.integration.coach.CoachRequest
import com.github.sebastianp265.aitrainer.integration.coach.CoachResponder
import com.github.sebastianp265.aitrainer.integration.coach.CoachStep
import com.github.sebastianp265.aitrainer.integration.coach.ProposalDesk
import com.github.sebastianp265.aitrainer.integration.coach.ProposalDraft
import com.github.sebastianp265.aitrainer.integration.coach.ProposalOutcome
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.integration.workout.SetChange
import com.github.sebastianp265.aitrainer.shared.database.impl.inMemoryDatabase
import com.github.sebastianp265.aitrainer.shared.database.impl.seedExerciseMuscles
import com.github.sebastianp265.aitrainer.shared.database.impl.seedExerciseName
import com.github.sebastianp265.aitrainer.shared.database.impl.seedExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import java.nio.file.Files
import kotlin.test.assertTrue
import kotlin.time.Clock
import kotlin.time.Instant

internal class ScriptedResponder : CoachResponder {
    val requests = mutableListOf<CoachRequest>()
    val readiness = MutableStateFlow(CoachReadiness.READY)
    private val script = ArrayDeque<suspend (ProposalDesk) -> CoachReply>()

    fun answer(text: String, checked: List<String> = emptyList()) {
        script.addLast { CoachReply.Answer(text, checked) }
    }

    fun proposeAndAnswer(text: String, vararg drafts: ProposalDraft) {
        script.addLast { desk ->
            drafts.forEach { assertTrue(desk.propose(it) is ProposalOutcome.Saved) }
            CoachReply.Answer(text, emptyList())
        }
    }

    fun fail(kind: CoachFailure) {
        script.addLast { CoachReply.Failure(kind, "failure") }
    }

    override fun observeReadiness(): Flow<CoachReadiness> = readiness

    override suspend fun reply(request: CoachRequest, desk: ProposalDesk, onProgress: (CoachStep?) -> Unit): CoachReply {
        requests += request
        return script.removeFirst()(desk)
    }
}

internal class CoachFixture {
    var now: Instant = Instant.parse("2026-11-10T10:00:00Z")
    val clock = object : Clock {
        override fun now() = now
    }
    val database = inMemoryDatabase().also {
        it.seedExercises("bench", "squat")
        it.seedExerciseName("bench", "Bench Press")
        it.seedExerciseName("squat", "Back Squat")
        it.seedExerciseMuscles("bench", "CHEST", "ARMS")
        it.seedExerciseMuscles("squat", "LEGS")
    }
    val responder = ScriptedResponder()
    val plans = DefaultPlanService(database.planDao(), clock)
    val workouts = DefaultWorkoutService(database.workoutDao(), database.planDao(), plans, clock)
    val volume = DefaultVolumeService(database.volumeDao(), clock)
    val exercises = DefaultExerciseService(database.exerciseDao(), clock)
    val nutrition = DefaultNutritionService(database.nutritionDao(), clock)
    val healthSource = FakeHealthDataSource()
    private val dataStore = { name: String -> PreferenceDataStoreFactory.create(produceFile = { Files.createTempDirectory(name).resolve("$name.preferences_pb").toFile() }) }
    val health = DefaultHealthService(healthSource, dataStore("health"), HealthReloads(), TimeZone.UTC)
    val settings = DefaultSettingsService(dataStore("settings"))
    val memory = DefaultCoachMemoryService(database.coachDao(), clock)
    val desk = DefaultProposalDesk(workouts, volume, exercises, nutrition, database.coachDao(), clock)
    val coach = DefaultCoachService(responder, settings, database.coachDao(), desk, workouts, health, plans, volume, exercises, nutrition, clock)

    suspend fun proposeId(draft: ProposalDraft): Long = (desk.propose(draft) as ProposalOutcome.Saved).proposalId

    suspend fun finishBenchWorkout(weight: Double = 80.0) {
        workouts.startSession()
        val session = workouts.observeActiveSession().first()!!.id
        workouts.addExercise(session, "bench")
        workouts.changeSet(workouts.observeSets(session).first().single().id, SetChange.WeightTo(weight))
        workouts.finishSession(session)
    }
}

internal fun benchSet(weightKg: Double = 80.0, reps: Int = 8, rpe: Double? = null) = PlannedSet("bench", reps, weightKg, rpe, false)
