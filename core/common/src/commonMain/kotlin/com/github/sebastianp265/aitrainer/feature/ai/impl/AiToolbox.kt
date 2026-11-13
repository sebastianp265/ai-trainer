package com.github.sebastianp265.aitrainer.feature.ai.impl

import com.github.sebastianp265.aitrainer.integration.ai.AiTool
import com.github.sebastianp265.aitrainer.integration.coach.CoachMemoryService
import com.github.sebastianp265.aitrainer.integration.coach.ProposalDesk
import com.github.sebastianp265.aitrainer.integration.coach.ProposalDraft
import com.github.sebastianp265.aitrainer.integration.coach.ProposalOutcome
import com.github.sebastianp265.aitrainer.integration.coach.ProposedPlan
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.exercise.NewExercise
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.integration.plan.PlanService
import com.github.sebastianp265.aitrainer.integration.plan.PlannedSet
import com.github.sebastianp265.aitrainer.integration.volume.VolumeService
import com.github.sebastianp265.aitrainer.integration.workout.LastTimeGroup
import com.github.sebastianp265.aitrainer.integration.workout.RPE_MAX
import com.github.sebastianp265.aitrainer.integration.workout.RPE_MIN
import com.github.sebastianp265.aitrainer.integration.workout.RPE_STEP
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

internal val aiJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

internal class ToolRun(
    val text: String,
    val isError: Boolean = false,
    val checked: String? = null,
)

internal class AiToolbox(
    private val workouts: WorkoutService,
    private val health: HealthService,
    private val plans: PlanService,
    private val volume: VolumeService,
    private val exercises: ExerciseService,
    private val memory: CoachMemoryService,
    private val clock: Clock,
) {

    private companion object {
        const val DEFAULT_DAYS = 28
        const val MAX_WORKOUTS = 15
        const val MAX_SEARCH_RESULTS = 12
        const val PROPOSAL_SAVED = "Proposal saved. The user will see it as a card and must accept it."
    }

    val definitions: List<AiTool> = listOf(
        tool("get_workouts", "Finished gym workouts from the last N days (default 28) with working sets as sets×reps×kg@RPE.", """{"type":"object","properties":{"days":{"type":"integer"}}}"""),
        tool("get_exercise_history", "History of one exercise: per session the working sets, estimated 1RM and average RPE.", """{"type":"object","properties":{"exerciseKey":{"type":"string"}},"required":["exerciseKey"]}"""),
        tool("find_exercises", "Finds exercise keys by a name fragment. Use it before proposing exercises.", """{"type":"object","properties":{"query":{"type":"string"}},"required":["query"]}"""),
        tool("get_health_days", "Daily values from Health Connect (calories, macros, weight, steps, sleep, resting heart rate, HRV) between two dates (YYYY-MM-DD). Missing values are left out.", """{"type":"object","properties":{"from":{"type":"string"},"to":{"type":"string"}},"required":["from","to"]}"""),
        tool("get_plans", "Upcoming plans (not done yet) with all their sets.", """{"type":"object","properties":{}}"""),
        tool("get_weekly_sets", "Working sets per muscle group per week (primary 1, secondary 0.5) for the last N weeks, with the weekly targets.", """{"type":"object","properties":{"weeks":{"type":"integer"}}}"""),
        tool("get_workout", "All sets of one gym workout. Without sessionId: the workout running right now.", """{"type":"object","properties":{"sessionId":{"type":"integer"}}}"""),
        tool(
            "propose_workout_change",
            "Proposes new sets for some exercises of the running workout (default) or of a finished workout (sessionId from get_workouts, e.g. to fix a typo). " +
                "For each exercise you list, give ALL its sets, including the ones already done; exercises in removeExerciseKeys are removed. The user must accept it.",
            """{"type":"object","properties":{"sessionId":{"type":"integer"},"summary":{"type":"string"},"sets":{"type":"array","items":${setItemSchema()}},"removeExerciseKeys":{"type":"array","items":{"type":"string"}}},"required":["summary"]}""",
        ),
        tool("propose_plan", "Proposes a NEW plan. The user must accept it in the chat. Give a one-line summary in the user's language.", planSchema(withId = false)),
        tool("propose_plan_change", "Proposes a change of an EXISTING upcoming plan (full new content). planId is required.", planSchema(withId = true)),
        tool("propose_plans", "Proposes several plans at once (new, or changes when planId is set). The user can accept all.", """{"type":"object","properties":{"summary":{"type":"string"},"plans":{"type":"array","items":${planItemSchema()}}},"required":["summary","plans"]}"""),
        tool("propose_target_change", "Proposes a new weekly working-set target range for one muscle group (CHEST, BACK, LEGS, SHOULDERS, ARMS, ABS).", """{"type":"object","properties":{"groupKey":{"type":"string"},"minSets":{"type":"integer"},"maxSets":{"type":"integer"},"reason":{"type":"string"},"summary":{"type":"string"}},"required":["groupKey","minSets","maxSets","reason","summary"]}"""),
        tool(
            "propose_new_exercise",
            "Proposes a new exercise when find_exercises finds nothing that fits. Muscle groups: CHEST, BACK, LEGS, SHOULDERS, ARMS, ABS. The user must accept it.",
            """{"type":"object","properties":{"name":{"type":"string"},"primaryGroupKey":{"type":"string"},"secondaryGroupKeys":{"type":"array","items":{"type":"string"}},"summary":{"type":"string"}},"required":["name","primaryGroupKey","summary"]}""",
        ),
        tool(
            "propose_nutrition_target",
            "Proposes a daily calorie and/or protein target shown in Progress. Base it on the Health Connect data and the goal. The user must accept it.",
            """{"type":"object","properties":{"kcal":{"type":"integer"},"proteinG":{"type":"integer"},"reason":{"type":"string"},"summary":{"type":"string"}},"required":["reason","summary"]}""",
        ),
        tool("add_note", "Saves a short note about the user for future conversations.", """{"type":"object","properties":{"text":{"type":"string"}},"required":["text"]}"""),
        tool("update_note", "Changes the text of one of your notes.", """{"type":"object","properties":{"id":{"type":"integer"},"text":{"type":"string"}},"required":["id","text"]}"""),
        tool("delete_note", "Deletes one of your notes.", """{"type":"object","properties":{"id":{"type":"integer"}},"required":["id"]}"""),
        tool("update_profile", "Fills in a profile field: goal, time, equipment, limitations or preferences.", """{"type":"object","properties":{"field":{"type":"string"},"value":{"type":"string"}},"required":["field","value"]}"""),
    )

    suspend fun run(name: String, inputJson: String, desk: ProposalDesk): ToolRun = try {
        val input = aiJson.parseToJsonElement(inputJson).jsonObject
        when (name) {
            "get_workouts" -> getWorkouts(input.int("days") ?: DEFAULT_DAYS)
            "get_exercise_history" -> getExerciseHistory(input.string("exerciseKey"))
            "find_exercises" -> findExercises(input.string("query"))
            "get_health_days" -> getHealthDays(input.date("from"), input.date("to"))
            "get_plans" -> getPlans()
            "get_weekly_sets" -> getWeeklySets(input.int("weeks") ?: 4)
            "get_workout" -> getWorkout(input.int("sessionId")?.toLong())
            "propose_workout_change" -> proposeWorkoutChange(inputJson, desk)
            "propose_new_exercise" -> proposeNewExercise(inputJson, desk)
            "propose_nutrition_target" -> proposeNutritionTarget(inputJson, desk)
            "propose_plan" -> proposePlan(inputJson, desk, isChange = false)
            "propose_plan_change" -> proposePlan(inputJson, desk, isChange = true)
            "propose_plans" -> proposePlans(inputJson, desk)
            "propose_target_change" -> proposeTarget(inputJson, desk)
            "add_note" -> addNote(input.string("text"))
            "update_note" -> updateNote(input.int("id") ?: -1, input.string("text"))
            "delete_note" -> deleteNote(input.int("id") ?: -1)
            "update_profile" -> updateProfile(input.string("field"), input.string("value"))
            else -> ToolRun("Unknown tool $name", isError = true)
        }
    } catch (e: IllegalArgumentException) {
        ToolRun("Invalid input: ${e.message}", isError = true)
    } catch (e: SerializationException) {
        ToolRun("Invalid input: ${e.message}", isError = true)
    }

    private suspend fun getWorkouts(days: Int): ToolRun {
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        val sessions = workouts.observeFinishedSessions(today.minus(DatePeriod(days = days)), today).first().take(MAX_WORKOUTS)
        val names = exerciseNames()
        val text = sessions.map { session ->
            val report = workouts.observeWorkoutReport(session.id).first()
            val title = session.planName ?: "Workout"
            val lines = report?.exercises.orEmpty().joinToString("; ") { "${names[it.exerciseKey] ?: it.exerciseKey}: ${groupsText(it.groups)}" }
            "sessionId=${session.id} ${session.date} $title (${session.finishedAt?.let { (it - session.startedAt).inWholeMinutes }} min): $lines"
        }.joinToString("\n")
        return ToolRun(text.ifEmpty { "No workouts in the last $days days." }, checked = "workouts")
    }

    private suspend fun getExerciseHistory(key: String): ToolRun {
        val history = workouts.observeExerciseHistory(key).first()
        val text = history.sessions.take(MAX_WORKOUTS).joinToString("\n") {
            "${it.date}: ${groupsText(it.groups)}; est. 1RM ${it.estimatedOneRepMaxKg?.let { v -> roundKg(v) } ?: "-"}; avg RPE ${it.averageRpe?.let { v -> roundKg(v) } ?: "-"}"
        }
        return ToolRun(text.ifEmpty { "No finished workouts with $key." }, checked = "exercise:$key")
    }

    private suspend fun findExercises(query: String): ToolRun {
        val q = query.trim().lowercase()
        val found = exercises.exercises().filter { e -> e.namesByLanguage.values.any { it.lowercase().contains(q) } || e.key.lowercase().contains(q.replace(' ', '_')) }
        val text = found.take(MAX_SEARCH_RESULTS).joinToString("\n") { "${it.key}: ${it.namesByLanguage["en"]} (${it.primaryMuscles.joinToString { m -> m.key }})" }
        return ToolRun(text.ifEmpty { "No exercise matches \"$query\"." })
    }

    private suspend fun getHealthDays(from: LocalDate, to: LocalDate): ToolRun {
        val days = health.observeDays(from, to).first()
        val text = days.sortedBy { it.date }.joinToString("\n") { d ->
            listOfNotNull(
                d.kcalEaten?.let { "eaten $it kcal" }, d.proteinG?.let { "protein ${roundKg(it)} g" }, d.kcalBurned?.let { "burned $it kcal" },
                d.weightKg?.let { "weight ${roundKg(it)} kg" }, d.steps?.let { "steps $it" }, d.sleepMinutes?.let { "sleep ${it / 60} h ${it % 60} min" },
                d.restingHeartRate?.let { "resting HR ${roundKg(it)}" }, d.hrvMs?.let { "HRV ${roundKg(it)} ms" },
            ).joinToString(", ", prefix = "${d.date}: ")
        }
        return ToolRun(text.ifEmpty { "No Health Connect data between $from and $to." }, checked = "health_days")
    }

    private suspend fun getPlans(): ToolRun {
        val names = exerciseNames()
        val text = plans.observeUpcoming().first().joinToString("\n") { plan ->
            val sets = plan.sets.joinToString("; ") { "${names[it.exerciseKey] ?: it.exerciseKey} [${it.exerciseKey}] ${if (it.isWarmup) "warm-up " else ""}${it.reps}×${roundKg(it.weightKg)} kg${it.rpe?.let { r -> " @${roundKg(r)}" }.orEmpty()}" }
            "planId=${plan.id} \"${plan.name}\" by ${plan.author.name.lowercase()}: $sets"
        }
        return ToolRun(text.ifEmpty { "No upcoming plans." }, checked = "plans")
    }

    private suspend fun getWorkout(sessionId: Long?): ToolRun {
        val session = (if (sessionId == null) workouts.observeActiveSession().first() else workouts.findSession(sessionId))
            ?: return ToolRun(if (sessionId == null) "No workout is running." else "No workout with id $sessionId.", isError = sessionId != null)
        val names = exerciseNames()
        val sets = workouts.observeSets(session.id).first().sortedBy { it.positionInSession }
        val plan = session.planId?.let { plans.observePlan(it).first() }
        val text = buildString {
            appendLine("sessionId=${session.id} ${session.date} ${if (session.finishedAt == null) "RUNNING now" else "finished"}${plan?.let { " from plan \"${it.name}\"" }.orEmpty()}")
            sets.groupBy { it.exerciseKey }.forEach { (key, exerciseSets) ->
                appendLine("${names[key] ?: key} [$key]: " + exerciseSets.joinToString("; ") { "${if (it.isWarmup) "warm-up " else ""}${it.reps}×${roundKg(it.weightKg)} kg${it.rpe?.let { r -> " @${roundKg(r)}" }.orEmpty()}" })
            }
            if (plan != null) appendLine("Planned: " + plan.sets.joinToString("; ") { "${it.exerciseKey} ${if (it.isWarmup) "warm-up " else ""}${it.reps}×${roundKg(it.weightKg)} kg" })
        }
        return ToolRun(text.trim(), checked = "workout")
    }

    private suspend fun getWeeklySets(weeks: Int): ToolRun {
        val today = clock.todayIn(TimeZone.currentSystemDefault())
        val sets = volume.observeWeeklySets(today.minus(DatePeriod(days = weeks * 7)), today).first()
        val targets = volume.observeTargets().first()
        val text = buildString {
            targets.forEach { appendLine("${it.groupKey}: target ${it.minSets}-${it.maxSets} (${it.source.name.lowercase()})") }
            sets.forEach { week -> appendLine("week of ${week.weekStart}: " + week.setsByGroup.entries.joinToString { "${it.key} ${roundKg(it.value)}" }) }
        }
        return ToolRun(text.trim(), checked = "weekly_sets")
    }

    private suspend fun proposeWorkoutChange(inputJson: String, desk: ProposalDesk): ToolRun {
        val input = aiJson.decodeFromString<ProposeWorkoutChangeInput>(inputJson)
        val draft = ProposalDraft.WorkoutChange(input.sessionId, input.sets.map { it.toPlannedSet() }, input.removeExerciseKeys, input.summary)
        return propose(desk, draft, PROPOSAL_SAVED)
    }

    private suspend fun proposeNutritionTarget(inputJson: String, desk: ProposalDesk): ToolRun {
        val input = aiJson.decodeFromString<ProposeNutritionTargetInput>(inputJson)
        return propose(desk, ProposalDraft.Nutrition(input.kcal, input.proteinG, input.reason, input.summary), PROPOSAL_SAVED)
    }

    private suspend fun proposeNewExercise(inputJson: String, desk: ProposalDesk): ToolRun {
        val input = aiJson.decodeFromString<ProposeNewExerciseInput>(inputJson)
        val draft = ProposalDraft.Exercise(NewExercise(input.name, input.primaryGroupKey, input.secondaryGroupKeys), input.summary)
        return propose(desk, draft, "Proposal saved. Once the user accepts it, find_exercises returns its key.")
    }

    private suspend fun proposePlan(inputJson: String, desk: ProposalDesk, isChange: Boolean): ToolRun {
        val input = aiJson.decodeFromString<ProposePlanInput>(inputJson)
        require(!isChange || input.planId != null) { "planId is required for a change" }
        val plan = ProposedPlan(input.planId.takeIf { isChange }, input.name, input.sets.map { it.toPlannedSet() })
        return propose(desk, ProposalDraft.Plan(plan, input.summary), PROPOSAL_SAVED)
    }

    private suspend fun proposePlans(inputJson: String, desk: ProposalDesk): ToolRun {
        val input = aiJson.decodeFromString<ProposePlansInput>(inputJson)
        return propose(desk, ProposalDraft.Plans(input.plans.map { it.toProposedPlan() }, input.summary), PROPOSAL_SAVED)
    }

    private suspend fun proposeTarget(inputJson: String, desk: ProposalDesk): ToolRun {
        val input = aiJson.decodeFromString<ProposeTargetInput>(inputJson)
        return propose(desk, ProposalDraft.Target(input.groupKey, input.minSets, input.maxSets, input.reason, input.summary), PROPOSAL_SAVED)
    }

    private suspend fun propose(desk: ProposalDesk, draft: ProposalDraft, savedText: String): ToolRun = when (val outcome = desk.propose(draft)) {
        is ProposalOutcome.Saved -> ToolRun(savedText)
        is ProposalOutcome.Rejected -> ToolRun("Invalid input: ${outcome.reason}", isError = true)
    }

    private suspend fun addNote(text: String): ToolRun = ToolRun("Note saved with id ${memory.addNote(text)}.")

    private suspend fun updateNote(id: Int, text: String): ToolRun =
        if (memory.updateNote(id.toLong(), text)) ToolRun("Note updated.") else ToolRun("No note with id $id.", isError = true)

    private suspend fun deleteNote(id: Int): ToolRun =
        if (memory.deleteNote(id.toLong())) ToolRun("Note deleted.") else ToolRun("No note with id $id.", isError = true)

    private suspend fun updateProfile(field: String, value: String): ToolRun {
        memory.setProfileField(field, value)
        return ToolRun("Profile updated.")
    }

    private suspend fun exerciseNames(): Map<String, String> =
        exercises.exercises().associate { it.key to (it.namesByLanguage["en"] ?: it.key) }

    private fun groupsText(groups: List<LastTimeGroup>): String =
        groups.joinToString(", ") { g ->
            val load = if (g.weightKg > 0) "${roundKg(g.weightKg)} kg" else "bodyweight"
            val rpeTo = g.rpeTo
            val rpe = g.rpeFrom?.let { from -> "@${roundKg(from)}" + if (rpeTo != null && rpeTo != from) "-${roundKg(rpeTo)}" else "" }.orEmpty()
            "${g.setCount}×${g.reps}×$load$rpe"
        }

    private fun roundKg(value: Double): String = if (value % 1.0 == 0.0) value.toLong().toString() else (kotlin.math.round(value * 10) / 10).toString()

    private fun tool(name: String, description: String, schema: String) = AiTool(name, description, schema)

    private fun JsonObject.string(key: String): String = this[key]?.jsonPrimitive?.contentOrNull ?: throw IllegalArgumentException("$key is required")

    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()

    private fun JsonObject.date(key: String): LocalDate = LocalDate.parse(string(key))
}

@Serializable
internal data class ToolSet(
    val exerciseKey: String,
    val reps: Int,
    val weightKg: Double = 0.0,
    val rpe: Double? = null,
    val warmup: Boolean = false,
)

@Serializable
internal data class ToolPlan(
    val planId: Long? = null,
    val name: String,
    val sets: List<ToolSet>,
)

@Serializable
internal data class ProposePlanInput(
    val planId: Long? = null,
    val name: String,
    val summary: String,
    val sets: List<ToolSet>,
)

@Serializable
internal data class ProposeNutritionTargetInput(val kcal: Int? = null, val proteinG: Int? = null, val reason: String, val summary: String)

@Serializable
internal data class ProposeNewExerciseInput(
    val name: String,
    val primaryGroupKey: String,
    val secondaryGroupKeys: List<String> = emptyList(),
    val summary: String,
)

@Serializable
internal data class ProposeWorkoutChangeInput(
    val sessionId: Long? = null,
    val summary: String,
    val sets: List<ToolSet> = emptyList(),
    val removeExerciseKeys: List<String> = emptyList(),
)

@Serializable
internal data class ProposePlansInput(val summary: String, val plans: List<ToolPlan>)

@Serializable
internal data class ProposeTargetInput(
    val groupKey: String,
    val minSets: Int,
    val maxSets: Int,
    val reason: String,
    val summary: String,
)

private fun ToolSet.toPlannedSet() = PlannedSet(exerciseKey, reps, weightKg, rpe, warmup)

private fun ToolPlan.toProposedPlan() = ProposedPlan(planId, name, sets.map { it.toPlannedSet() })

private const val RPE_SCALE = "from $RPE_MIN to $RPE_MAX in steps of $RPE_STEP"

private fun setItemSchema() =
    """{"type":"object","properties":{"exerciseKey":{"type":"string"},"reps":{"type":"integer"},"weightKg":{"type":"number"},"rpe":{"type":"number","description":"$RPE_SCALE"},"warmup":{"type":"boolean"}},"required":["exerciseKey","reps"]}"""

private fun planItemSchema() =
    """{"type":"object","properties":{"planId":{"type":"integer"},"name":{"type":"string"},"sets":{"type":"array","items":${setItemSchema()}}},"required":["name","sets"]}"""

private fun planSchema(withId: Boolean) =
    """{"type":"object","properties":{${if (withId) """"planId":{"type":"integer"},""" else ""}"name":{"type":"string"},"summary":{"type":"string"},"sets":{"type":"array","items":${setItemSchema()}}},"required":[${if (withId) """"planId",""" else ""}"name","summary","sets"]}"""
