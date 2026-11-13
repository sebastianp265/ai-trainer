package com.github.sebastianp265.aitrainer.feature.ai.impl

import com.github.sebastianp265.aitrainer.integration.ai.AiClient
import com.github.sebastianp265.aitrainer.integration.ai.AiContent
import com.github.sebastianp265.aitrainer.integration.ai.AiFailureKind
import com.github.sebastianp265.aitrainer.integration.ai.AiMessage
import com.github.sebastianp265.aitrainer.integration.ai.AiResponse
import com.github.sebastianp265.aitrainer.integration.ai.AiResult
import com.github.sebastianp265.aitrainer.integration.ai.AiRole
import com.github.sebastianp265.aitrainer.integration.ai.AiRun
import com.github.sebastianp265.aitrainer.integration.ai.AiService
import com.github.sebastianp265.aitrainer.integration.coach.CoachFailure
import com.github.sebastianp265.aitrainer.integration.coach.CoachMemoryService
import com.github.sebastianp265.aitrainer.integration.coach.CoachReadiness
import com.github.sebastianp265.aitrainer.integration.coach.CoachReply
import com.github.sebastianp265.aitrainer.integration.coach.CoachRequest
import com.github.sebastianp265.aitrainer.integration.coach.CoachResponder
import com.github.sebastianp265.aitrainer.integration.coach.CoachStep
import com.github.sebastianp265.aitrainer.integration.coach.ProposalDesk
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.integration.nutrition.NutritionService
import com.github.sebastianp265.aitrainer.integration.volume.VolumeService
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlin.time.Clock
import kotlin.time.Duration

internal class AiCoachResponder(
    private val client: () -> AiClient,
    private val ai: AiService,
    private val usage: AiUsageLog,
    private val toolbox: AiToolbox,
    private val memory: CoachMemoryService,
    private val workouts: WorkoutService,
    private val health: HealthService,
    private val volume: VolumeService,
    private val nutrition: NutritionService,
    private val clock: Clock,
) : CoachResponder {

    private companion object {
        const val MAX_TOOL_ROUNDS = 8
        const val SUMMARY_DAYS = 28
        const val SUMMARY_WORKOUTS = 5
    }

    override fun observeReadiness(): Flow<CoachReadiness> =
        ai.observeIsApiKeyStored().map { if (it) CoachReadiness.READY else CoachReadiness.NEEDS_SETUP }

    override suspend fun reply(request: CoachRequest, desk: ProposalDesk, onProgress: (CoachStep?) -> Unit): CoachReply {
        val startedAt = clock.now()
        var run = AiRun(rounds = 0, inputTokens = 0, cacheReadTokens = 0, cacheWriteTokens = 0, outputTokens = 0, duration = Duration.ZERO, stopReason = null)
        val aiRequest = buildAiRequest(context(request), request.review?.asPrompt())
        val checked = mutableListOf<String>()
        val conversation = aiRequest.messages.toMutableList()
        repeat(MAX_TOOL_ROUNDS) {
            when (val result = client().send(aiRequest.copy(messages = conversation.toList()))) {
                is AiResult.Failure -> return CoachReply.Failure(result.kind.toCoachFailure(), result.message)
                is AiResult.Success -> {
                    val response = result.response
                    run = run.after(response, clock.now() - startedAt)
                    usage.record(response, run)
                    val toolUses = response.content.filterIsInstance<AiContent.ToolUse>()
                    if (toolUses.isEmpty()) {
                        return CoachReply.Answer(response.content.filterIsInstance<AiContent.Text>().joinToString("\n") { it.text }, checked)
                    }
                    conversation += AiMessage(AiRole.ASSISTANT, response.content)
                    val results = toolUses.map { use ->
                        onProgress(use.name.toStep())
                        val toolRun = toolbox.run(use.name, use.inputJson, desk)
                        toolRun.checked?.let { if (it !in checked) checked += it }
                        AiContent.ToolResult(use.id, toolRun.text, toolRun.isError)
                    }
                    conversation += AiMessage(AiRole.USER, results)
                }
            }
        }
        return CoachReply.Answer("", checked)
    }

    private suspend fun context(request: CoachRequest): AiContext {
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        val sessions = workouts.observeFinishedSessions(today.minus(DatePeriod(days = SUMMARY_DAYS)), today).first().take(SUMMARY_WORKOUTS)
        return AiContext(
            model = ai.observeModel().first(),
            tuning = ai.observeTuning().first(),
            today = today,
            profile = memory.observeProfile().first(),
            notes = memory.observeNotes().first(),
            targets = volume.observeTargets().first(),
            nutritionTarget = nutrition.observeTarget().first(),
            recentWorkouts = sessions.map { "${it.date} ${it.planName ?: "Workout"}: ${it.exerciseCount} exercises, ${it.setCount} working sets" },
            healthDays = health.observeDays(today.minus(DatePeriod(days = SUMMARY_DAYS)), today).first(),
            turns = request.turns,
            tools = toolbox.definitions,
        )
    }
}

private fun AiRun.after(response: AiResponse, elapsed: Duration) = AiRun(
    rounds = rounds + 1,
    inputTokens = inputTokens + response.inputTokens,
    cacheReadTokens = cacheReadTokens + response.cacheReadTokens,
    cacheWriteTokens = cacheWriteTokens + response.cacheWriteTokens,
    outputTokens = outputTokens + response.outputTokens,
    duration = elapsed,
    stopReason = response.stopReason,
)

private fun AiFailureKind.toCoachFailure() = when (this) {
    AiFailureKind.NO_KEY, AiFailureKind.KEY_REJECTED -> CoachFailure.NEEDS_SETUP
    AiFailureKind.NO_NETWORK -> CoachFailure.NO_CONNECTION
    AiFailureKind.OVERLOADED -> CoachFailure.BUSY
    AiFailureKind.OTHER -> CoachFailure.OTHER
}

private fun String.toStep() = when (this) {
    "get_workouts" -> CoachStep.WORKOUTS
    "get_exercise_history" -> CoachStep.EXERCISE_HISTORY
    "get_health_days" -> CoachStep.HEALTH
    "get_plans" -> CoachStep.PLANS
    "get_weekly_sets" -> CoachStep.WEEKLY_SETS
    "get_workout" -> CoachStep.WORKOUT
    "find_exercises" -> CoachStep.EXERCISES
    else -> CoachStep.WRITING
}
