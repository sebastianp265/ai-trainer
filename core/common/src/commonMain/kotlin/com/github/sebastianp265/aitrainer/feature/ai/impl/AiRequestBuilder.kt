package com.github.sebastianp265.aitrainer.feature.ai.impl

import com.github.sebastianp265.aitrainer.integration.ai.AiContent
import com.github.sebastianp265.aitrainer.integration.ai.AiMessage
import com.github.sebastianp265.aitrainer.integration.ai.AiRequest
import com.github.sebastianp265.aitrainer.integration.ai.AiRole
import com.github.sebastianp265.aitrainer.integration.ai.AiTool
import com.github.sebastianp265.aitrainer.integration.ai.AiTuning
import com.github.sebastianp265.aitrainer.integration.coach.CoachMemoryService
import com.github.sebastianp265.aitrainer.integration.coach.CoachNote
import com.github.sebastianp265.aitrainer.integration.coach.CoachRole
import com.github.sebastianp265.aitrainer.integration.coach.CoachTurn
import com.github.sebastianp265.aitrainer.integration.coach.TopicKind
import com.github.sebastianp265.aitrainer.integration.health.HealthDay
import com.github.sebastianp265.aitrainer.integration.nutrition.NutritionTarget
import com.github.sebastianp265.aitrainer.integration.volume.WeeklyTarget
import kotlinx.datetime.LocalDate

internal class AiContext(
    val model: String,
    val tuning: AiTuning,
    val today: LocalDate,
    val profile: Map<String, String>,
    val notes: List<CoachNote>,
    val targets: List<WeeklyTarget>,
    val nutritionTarget: NutritionTarget?,
    val recentWorkouts: List<String>,
    val healthDays: List<HealthDay>,
    val turns: List<CoachTurn>,
    val tools: List<AiTool>,
)

internal fun buildAiRequest(context: AiContext, transientUserText: String? = null): AiRequest =
    AiRequest(
        model = context.model,
        system = systemPrompt(context),
        messages = conversation(context.turns, transientUserText),
        tools = context.tools,
        tuning = context.tuning,
    )

private fun conversation(turns: List<CoachTurn>, transientUserText: String?): List<AiMessage> =
    (turns.map { it.role to it.textForModel() } + listOfNotNull(transientUserText?.let { CoachRole.USER to it }))
        .fold(mutableListOf<Pair<AiRole, String>>()) { messages, (messageRole, messageText) ->
            val role = if (messageRole == CoachRole.USER) AiRole.USER else AiRole.ASSISTANT
            if (messages.isNotEmpty() && messages.last().first == role) {
                messages[messages.lastIndex] = role to (messages.last().second + "\n" + messageText)
            } else {
                messages += role to messageText
            }
            messages
        }
        .dropWhile { it.first == AiRole.ASSISTANT }
        .map { (role, text) -> AiMessage(role, listOf(AiContent.Text(text))) }

private fun CoachTurn.textForModel(): String {
    val topic = topic ?: return text
    val subject = when (topic.kind) {
        TopicKind.PLAN -> "plan id ${topic.id}"
        TopicKind.WORKOUT -> "finished gym workout id ${topic.id}"
        TopicKind.ACTIVE_WORKOUT -> "gym workout running right now, id ${topic.id}"
        TopicKind.EXERCISE -> "exercise key ${topic.id}"
    }
    return "[The user asks about this $subject: \"${topic.label}\"]\n$text"
}

private fun systemPrompt(context: AiContext): String = buildString {
    appendLine("You are the personal strength coach inside a single-user Android training app. Today is ${context.today}.")
    appendLine("Reply in the language the user writes in. Keep answers short and practical.")
    appendLine()
    appendLine("Rules:")
    appendLine("- You change nothing yourself. Plans and weekly target changes are only proposed with the propose_* tools; the user accepts or replies to adjust.")
    appendLine("- When the user asks about the running workout, read it with get_workout and propose changes with propose_workout_change. Keep sets already done unchanged.")
    appendLine("- For a block of several weeks, use propose_plans with the plans in the order to do them, progress the load week by week and include a lighter week.")
    appendLine("- Use read tools when you need data beyond the summary below. Use find_exercises to get exercise keys before proposing exercises.")
    appendLine("- Working sets only count for statistics; warm-up sets are marked warmup. Weights are in kg. RPE is 6 to 10 in steps of 0.5.")
    appendLine("- Weekly sets per muscle group (CHEST, BACK, LEGS, SHOULDERS, ARMS, ABS): a working set counts 1 for the primary group and 0.5 for each secondary group.")
    appendLine("- No readiness scores and no automatic progression. Suggest changes from the data and the user's words.")
    appendLine("- Save lasting facts with add_note and fill the profile with update_profile. Do not store small talk.")
    appendLine()
    appendLine("## Profile")
    CoachMemoryService.PROFILE_FIELDS.forEach { field -> appendLine("$field: ${context.profile[field] ?: "unknown"}") }
    appendLine()
    appendLine("## Notes")
    if (context.notes.isEmpty()) appendLine("(none)") else context.notes.forEach { appendLine("[${it.id}] ${it.text}") }
    appendLine()
    appendLine("## Weekly set targets")
    context.targets.forEach { appendLine("${it.groupKey}: ${it.minSets}-${it.maxSets} (${it.source.name.lowercase()})") }
    appendLine()
    appendLine("## Daily nutrition target")
    appendLine(context.nutritionTarget?.let { "kcal: ${it.kcal ?: "none"}, protein: ${it.proteinG?.let { g -> "$g g" } ?: "none"} (set ${it.setOn})" } ?: "none")
    appendLine()
    appendLine("## Recent data")
    if (context.recentWorkouts.isEmpty()) appendLine("No gym workouts logged yet.") else context.recentWorkouts.forEach { appendLine(it) }
    appendLine(healthSummary(context.healthDays))
}.trimEnd()

private fun healthSummary(days: List<HealthDay>): String {
    if (days.isEmpty()) return "No Health Connect data."
    val weights = days.mapNotNull { it.weightKg }
    val steps = days.mapNotNull { it.steps }
    val sleep = days.mapNotNull { it.sleepMinutes }
    val eaten = days.mapNotNull { it.kcalEaten }
    val parts = listOfNotNull(
        weights.lastOrNull()?.let { "last weight $it kg" },
        steps.takeIf { it.isNotEmpty() }?.let { "average steps ${it.average().toInt()}" },
        sleep.takeIf { it.isNotEmpty() }?.let { "average sleep ${it.average().toInt() / 60} h ${it.average().toInt() % 60} min" },
        eaten.takeIf { it.isNotEmpty() }?.let { "average eaten ${it.average().toInt()} kcal" },
    )
    return "Health Connect, last ${days.size} days: " + parts.joinToString(", ").ifEmpty { "no values" }
}
