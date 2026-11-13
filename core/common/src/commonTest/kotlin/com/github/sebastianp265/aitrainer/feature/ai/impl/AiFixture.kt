package com.github.sebastianp265.aitrainer.feature.ai.impl

import com.github.sebastianp265.aitrainer.shared.database.impl.temporaryDataStore
import com.github.sebastianp265.aitrainer.feature.coach.impl.DefaultCoachMemoryService
import com.github.sebastianp265.aitrainer.feature.exercise.impl.DefaultExerciseService
import com.github.sebastianp265.aitrainer.feature.health.impl.DefaultHealthService
import com.github.sebastianp265.aitrainer.feature.health.impl.FakeHealthDataSource
import com.github.sebastianp265.aitrainer.feature.health.impl.HealthReloads
import com.github.sebastianp265.aitrainer.feature.nutrition.impl.DefaultNutritionService
import com.github.sebastianp265.aitrainer.feature.plan.impl.DefaultPlanService
import com.github.sebastianp265.aitrainer.feature.volume.impl.DefaultVolumeService
import com.github.sebastianp265.aitrainer.feature.workout.impl.DefaultWorkoutService
import com.github.sebastianp265.aitrainer.integration.ai.AiClient
import com.github.sebastianp265.aitrainer.integration.ai.AiContent
import com.github.sebastianp265.aitrainer.integration.ai.AiFailureKind
import com.github.sebastianp265.aitrainer.integration.ai.AiRequest
import com.github.sebastianp265.aitrainer.integration.ai.AiResponse
import com.github.sebastianp265.aitrainer.integration.ai.AiResult
import com.github.sebastianp265.aitrainer.integration.coach.CoachRequest
import com.github.sebastianp265.aitrainer.integration.coach.CoachRole
import com.github.sebastianp265.aitrainer.integration.coach.CoachTurn
import com.github.sebastianp265.aitrainer.integration.coach.ProposalDesk
import com.github.sebastianp265.aitrainer.integration.coach.ProposalDraft
import com.github.sebastianp265.aitrainer.integration.coach.ProposalOutcome
import com.github.sebastianp265.aitrainer.integration.settings.Secret
import com.github.sebastianp265.aitrainer.integration.settings.SecretStore
import com.github.sebastianp265.aitrainer.integration.workout.SetChange
import com.github.sebastianp265.aitrainer.shared.database.impl.inMemoryDatabase
import com.github.sebastianp265.aitrainer.shared.database.impl.seedExerciseMuscles
import com.github.sebastianp265.aitrainer.shared.database.impl.seedExerciseName
import com.github.sebastianp265.aitrainer.shared.database.impl.seedExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlin.time.Clock
import kotlin.time.Instant

internal class ScriptedAi : AiClient {
    val requests = mutableListOf<AiRequest>()
    var whileAnswering: () -> Unit = {}
    var keyCheckResult: AiFailureKind? = null
    val checkedKeys = mutableListOf<String>()
    private val script = ArrayDeque<AiResult>()

    fun answer(text: String) {
        script.addLast(AiResult.Success(AiResponse(listOf(AiContent.Text(text)), "end_turn", inputTokens = 100, cacheReadTokens = 0, cacheWriteTokens = 0, outputTokens = 20)))
    }

    fun callTool(name: String, inputJson: String) {
        script.addLast(AiResult.Success(AiResponse(listOf(AiContent.ToolUse("toolu_${script.size}", name, inputJson)), "tool_use", inputTokens = 100, cacheReadTokens = 0, cacheWriteTokens = 0, outputTokens = 20)))
    }

    fun fail(kind: AiFailureKind) {
        script.addLast(AiResult.Failure(kind, "failure"))
    }

    override suspend fun send(request: AiRequest): AiResult {
        requests += request
        whileAnswering()
        return script.removeFirst()
    }

    override suspend fun checkApiKey(apiKey: String): AiFailureKind? {
        checkedKeys += apiKey
        return keyCheckResult
    }
}

internal class RecordingDesk : ProposalDesk {
    val drafts = mutableListOf<ProposalDraft>()
    var rejection: String? = null

    override suspend fun propose(draft: ProposalDraft): ProposalOutcome {
        rejection?.let { return ProposalOutcome.Rejected(it) }
        drafts += draft
        return ProposalOutcome.Saved(drafts.size.toLong())
    }
}

internal class InMemorySecretStore : SecretStore {
    private val stored = MutableStateFlow<String?>(null)

    override suspend fun write(secret: Secret, value: String) {
        stored.value = value
    }

    override suspend fun read(secret: Secret): String? = stored.value

    override suspend fun delete(secret: Secret) {
        stored.value = null
    }

    override fun observeIsStored(secret: Secret): Flow<Boolean> = kotlinx.coroutines.flow.flow { stored.collect { emit(it != null) } }
}

internal fun requestOf(vararg texts: String) = CoachRequest(texts.map { CoachTurn(CoachRole.USER, it, null) })

internal class AiFixture {
    var now: Instant = Instant.parse("2026-11-10T10:00:00Z")
    val clock = object : Clock {
        override fun now() = now
    }
    val database = inMemoryDatabase(
        seedExercises("bench", "squat"),
        seedExerciseName("bench", "Bench Press"),
        seedExerciseName("squat", "Back Squat"),
        seedExerciseMuscles("bench", "CHEST", "ARMS"),
        seedExerciseMuscles("squat", "LEGS"),
    )
    val client = ScriptedAi()
    val desk = RecordingDesk()
    val secrets = InMemorySecretStore()
    val plans = DefaultPlanService(database.planDao(), clock)
    val workouts = DefaultWorkoutService(database.workoutDao(), database.planDao(), plans, clock)
    val volume = DefaultVolumeService(database.volumeDao(), clock)
    val exercises = DefaultExerciseService(database.exerciseDao(), clock)
    val nutrition = DefaultNutritionService(database.nutritionDao(), clock)
    val memory = DefaultCoachMemoryService(database.coachDao(), clock)
    val healthSource = FakeHealthDataSource()
    private val dataStore = ::temporaryDataStore
    val health = DefaultHealthService(healthSource, dataStore("health"), HealthReloads(), TimeZone.UTC)
    val usage = AiUsageLog(database.aiDao(), clock)
    val ai = DefaultAiService(dataStore("ai"), secrets, { client }, usage, clock)
    val toolbox = AiToolbox(workouts, health, plans, volume, exercises, memory, clock)
    val responder = AiCoachResponder({ client }, ai, usage, toolbox, memory, workouts, health, volume, nutrition, clock)

    suspend fun finishBenchWorkout(weight: Double = 80.0) {
        workouts.startSession()
        val session = workouts.observeActiveSession().first()!!.id
        workouts.addExercise(session, "bench")
        workouts.changeSet(workouts.observeSets(session).first().single().id, SetChange.WeightTo(weight))
        workouts.finishSession(session)
    }
}
