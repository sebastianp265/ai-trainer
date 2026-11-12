package com.github.sebastianp265.aitrainer.feature.coach.impl

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.github.sebastianp265.aitrainer.feature.exercise.impl.DefaultExerciseService
import com.github.sebastianp265.aitrainer.feature.health.impl.DefaultHealthService
import com.github.sebastianp265.aitrainer.feature.health.impl.FakeHealthDataSource
import com.github.sebastianp265.aitrainer.feature.plan.impl.DefaultPlanService
import com.github.sebastianp265.aitrainer.feature.settings.impl.DefaultSettingsService
import com.github.sebastianp265.aitrainer.feature.volume.impl.DefaultVolumeService
import com.github.sebastianp265.aitrainer.feature.workout.impl.DefaultWorkoutService
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeClient
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeContent
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeFailureKind
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeRequest
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeResponse
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeResult
import com.github.sebastianp265.aitrainer.integration.settings.Secret
import com.github.sebastianp265.aitrainer.integration.settings.SecretStore
import com.github.sebastianp265.aitrainer.shared.database.impl.inMemoryDatabase
import com.github.sebastianp265.aitrainer.shared.database.impl.seedExerciseMuscles
import com.github.sebastianp265.aitrainer.shared.database.impl.seedExerciseName
import com.github.sebastianp265.aitrainer.shared.database.impl.seedExercises
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import java.nio.file.Files
import kotlin.time.Clock
import kotlin.time.Instant

internal class ScriptedClaude : ClaudeClient {
    val requests = mutableListOf<ClaudeRequest>()
    private val script = ArrayDeque<ClaudeResult>()

    fun answer(text: String) {
        script.addLast(ClaudeResult.Success(ClaudeResponse(listOf(ClaudeContent.Text(text)), "end_turn", 100, 20)))
    }

    fun callTool(name: String, inputJson: String) {
        script.addLast(ClaudeResult.Success(ClaudeResponse(listOf(ClaudeContent.ToolUse("toolu_${script.size}", name, inputJson)), "tool_use", 100, 20)))
    }

    fun fail(kind: ClaudeFailureKind) {
        script.addLast(ClaudeResult.Failure(kind, "failure"))
    }

    override suspend fun send(request: ClaudeRequest): ClaudeResult {
        requests += request
        return script.removeFirst()
    }

    override suspend fun checkApiKey(apiKey: String): ClaudeFailureKind? = null
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
    val claude = ScriptedClaude()
    val plans = DefaultPlanService(database.planDao(), clock)
    val workouts = DefaultWorkoutService(database.workoutDao(), database.planDao(), plans, clock)
    val volume = DefaultVolumeService(database.volumeDao(), clock)
    val exercises = DefaultExerciseService(database.exerciseDao(), clock)
    val nutrition = com.github.sebastianp265.aitrainer.feature.nutrition.impl.DefaultNutritionService(database.nutritionDao(), clock)
    val healthSource = FakeHealthDataSource()
    private val dataStore = { name: String -> PreferenceDataStoreFactory.create(produceFile = { Files.createTempDirectory(name).resolve("$name.preferences_pb").toFile() }) }
    val health = DefaultHealthService(healthSource, dataStore("health"), com.github.sebastianp265.aitrainer.feature.health.impl.HealthReloads(), kotlinx.datetime.TimeZone.UTC)
    val settings = DefaultSettingsService(
        dataStore("settings"),
        object : SecretStore {
            override suspend fun write(secret: Secret, value: String) = Unit
            override suspend fun read(secret: Secret): String = "test-key"
            override suspend fun delete(secret: Secret) = Unit
            override fun observeIsStored(secret: Secret): Flow<Boolean> = flowOf(true)
        },
    )
    val toolbox = CoachToolbox(workouts, health, plans, volume, exercises, nutrition, database.coachDao(), clock)
    val coach = DefaultCoachService({ claude }, settings, database.coachDao(), toolbox, workouts, health, plans, volume, exercises, nutrition, clock)

    suspend fun finishBenchWorkout(weight: Double = 80.0) {
        workouts.startSession()
        val session = workouts.observeActiveSession().first()!!.id
        workouts.addExercise(session, "bench")
        workouts.changeSet(workouts.observeSets(session).first().single().id, com.github.sebastianp265.aitrainer.integration.workout.SetChange.WeightTo(weight))
        workouts.finishSession(session)
    }
}
