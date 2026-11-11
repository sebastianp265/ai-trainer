package com.github.sebastianp265.aitrainer.feature.coach.impl

import com.github.sebastianp265.aitrainer.integration.nutrition.NutritionService
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeTool
import com.github.sebastianp265.aitrainer.integration.coach.CoachService
import com.github.sebastianp265.aitrainer.integration.coach.ProposalState
import com.github.sebastianp265.aitrainer.integration.coach.ProposalType
import com.github.sebastianp265.aitrainer.integration.exercise.ExerciseService
import com.github.sebastianp265.aitrainer.integration.health.HealthService
import com.github.sebastianp265.aitrainer.integration.plan.PlanService
import com.github.sebastianp265.aitrainer.integration.volume.VolumeService
import com.github.sebastianp265.aitrainer.integration.workout.LastTimeGroup
import com.github.sebastianp265.aitrainer.integration.workout.WorkoutService
import com.github.sebastianp265.aitrainer.shared.coach.CoachDao
import com.github.sebastianp265.aitrainer.shared.coach.CoachNoteEntity
import com.github.sebastianp265.aitrainer.shared.coach.CoachProfileEntity
import com.github.sebastianp265.aitrainer.shared.coach.CoachProposalEntity
import kotlinx.coroutines.flow.first
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.todayIn
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.time.Clock

internal class ToolRun(
    val text: String,
    val isError: Boolean = false,
    val checked: String? = null,
    val proposalId: Long? = null,
)

internal class CoachToolbox(
    private val workouts: WorkoutService,
    private val health: HealthService,
    private val plans: PlanService,
    private val volume: VolumeService,
    private val exercises: ExerciseService,
    private val nutrition: NutritionService,
    private val dao: CoachDao,
    private val clock: Clock,
) {

    private companion object {
        const val DEFAULT_DAYS = 28
        const val MAX_WORKOUTS = 15
        const val MAX_SEARCH_RESULTS = 12
    }

    val definitions: List<ClaudeTool> = listOf(
        tool("get_workouts", "Finished gym workouts from the last N days (default 28) with working sets as sets×reps×kg@RPE.", """{"type":"object","properties":{"days":{"type":"integer"}}}"""),
        tool("get_exercise_history", "History of one exercise: per session the working sets, estimated 1RM and average RPE.", """{"type":"object","properties":{"exerciseKey":{"type":"string"}},"required":["exerciseKey"]}"""),
        tool("find_exercises", "Finds exercise keys by a name fragment. Use it before proposing exercises.", """{"type":"object","properties":{"query":{"type":"string"}},"required":["query"]}"""),
        tool("get_health_days", "Daily values from Health Connect (calories, macros, weight, steps, sleep, resting heart rate, HRV) between two dates (YYYY-MM-DD). Missing values are left out.", """{"type":"object","properties":{"from":{"type":"string"},"to":{"type":"string"}},"required":["from","to"]}"""),
        tool("get_external_workouts", "Workouts recorded by other apps (Health Connect) between two dates.", """{"type":"object","properties":{"from":{"type":"string"},"to":{"type":"string"}},"required":["from","to"]}"""),
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

    suspend fun run(name: String, inputJson: String): ToolRun = try {
        val input = coachJson.parseToJsonElement(inputJson).jsonObject
        when (name) {
            "get_workouts" -> getWorkouts(input.int("days") ?: DEFAULT_DAYS)
            "get_exercise_history" -> getExerciseHistory(input.string("exerciseKey"))
            "find_exercises" -> findExercises(input.string("query"))
            "get_health_days" -> getHealthDays(input.date("from"), input.date("to"))
            "get_external_workouts" -> getExternalWorkouts(input.date("from"), input.date("to"))
            "get_plans" -> getPlans()
            "get_weekly_sets" -> getWeeklySets(input.int("weeks") ?: 4)
            "get_workout" -> getWorkout(input.int("sessionId")?.toLong())
            "propose_workout_change" -> proposeWorkoutChange(inputJson)
            "propose_new_exercise" -> proposeNewExercise(inputJson)
            "propose_nutrition_target" -> proposeNutritionTarget(inputJson)
            "propose_plan" -> proposePlan(inputJson, ProposalType.NEW_PLAN)
            "propose_plan_change" -> proposePlan(inputJson, ProposalType.CHANGE_PLAN)
            "propose_plans" -> proposePlans(inputJson)
            "propose_target_change" -> proposeTarget(inputJson)
            "add_note" -> addNote(input.string("text"))
            "update_note" -> updateNote(input.int("id") ?: -1, input.string("text"))
            "delete_note" -> deleteNote(input.int("id") ?: -1)
            "update_profile" -> updateProfile(input.string("field"), input.string("value"))
            else -> ToolRun("Unknown tool $name", isError = true)
        }
    } catch (e: IllegalArgumentException) {
        ToolRun("Invalid input: ${e.message}", isError = true)
    } catch (e: kotlinx.serialization.SerializationException) {
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

    private suspend fun getExternalWorkouts(from: LocalDate, to: LocalDate): ToolRun {
        val zone = TimeZone.currentSystemDefault()
        val list = health.observeWorkouts().first().filter { it.startedAt.toLocalDateTime(zone).date in from..to }
        val text = list.joinToString("\n") { "${it.startedAt.toLocalDateTime(zone).date} ${it.type} ${it.durationMinutes} min" + (it.distanceM?.let { d -> ", ${roundKg(d / 1000)} km" } ?: "") }
        return ToolRun(text.ifEmpty { "No workouts from other apps between $from and $to." }, checked = "external_workouts")
    }

    private suspend fun getPlans(): ToolRun {
        val names = exerciseNames()
        val text = plans.observeUpcoming().first().joinToString("\n") { plan ->
            val sets = plan.sets.joinToString("; ") { "${names[it.exerciseKey] ?: it.exerciseKey} [${it.exerciseKey}] ${if (it.isWarmup) "warm-up " else ""}${it.reps}×${roundKg(it.weightKg)} kg${it.rpe?.let { r -> " @${roundKg(r)}" }.orEmpty()}" }
            "planId=${plan.id} \"${plan.name}\" ${plan.date ?: "no date"} by ${plan.author.name.lowercase()}: $sets"
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

    private suspend fun proposeWorkoutChange(inputJson: String): ToolRun {
        val input = coachJson.decodeFromString<ProposeWorkoutChangeInput>(inputJson)
        val session = (input.sessionId?.let { workouts.findSession(it) } ?: workouts.observeActiveSession().first())
            ?: throw IllegalArgumentException("no running workout; give sessionId of a finished workout from get_workouts")
        val exerciseKeys = (input.sets.map { it.exerciseKey } + input.removeExerciseKeys).distinct()
        require(exerciseKeys.isNotEmpty()) { "nothing to change" }
        val known = exerciseNames().keys
        exerciseKeys.forEach { require(it in known) { "unknown exerciseKey $it; use find_exercises" } }
        input.sets.forEach { require(it.reps > 0) { "reps must be positive" } }
        val id = dao.insertSupersedingProposal(
            CoachProposalEntity(
                type = ProposalType.WORKOUT_CHANGE, state = ProposalState.PENDING, summary = input.summary,
                payload = coachJson.encodeToString(ProposalPayload.serializer(), ProposalPayload(sessionId = session.id, exerciseKeys = exerciseKeys, workoutSets = input.sets)),
                subjectKey = "workout:${session.id}", messageId = null, createdAt = clock.now(),
            ),
        )
        return ToolRun("Proposal saved. The user will see it as a card and must accept it.", proposalId = id)
    }

    private suspend fun proposeNutritionTarget(inputJson: String): ToolRun {
        val input = coachJson.decodeFromString<ProposeNutritionTargetInput>(inputJson)
        require(input.kcal != null || input.proteinG != null) { "give kcal, proteinG or both" }
        input.kcal?.let { require(it in 1000..6000) { "kcal must be between 1000 and 6000" } }
        input.proteinG?.let { require(it in 30..400) { "proteinG must be between 30 and 400" } }
        val current = nutrition.observeTarget().first()
        val id = dao.insertSupersedingProposal(
            CoachProposalEntity(
                type = ProposalType.NUTRITION_TARGET, state = ProposalState.PENDING, summary = input.summary,
                payload = coachJson.encodeToString(
                    ProposalPayload.serializer(),
                    ProposalPayload(
                        reason = input.reason,
                        nutrition = NutritionPayload(input.kcal, input.proteinG, current?.kcal, current?.proteinG, current?.reason),
                    ),
                ),
                subjectKey = "nutrition", messageId = null, createdAt = clock.now(),
            ),
        )
        return ToolRun("Proposal saved. The user will see it as a card and must accept it.", proposalId = id)
    }

    private suspend fun proposeNewExercise(inputJson: String): ToolRun {
        val input = coachJson.decodeFromString<ProposeNewExerciseInput>(inputJson)
        require(input.name.isNotBlank()) { "name is empty" }
        val groups = exercises.observeMuscleGroups().first().map { it.key }
        (input.secondaryGroupKeys + input.primaryGroupKey).forEach { require(it in groups) { "unknown muscle group $it; use one of ${groups.joinToString()}" } }
        exerciseNames().entries.firstOrNull { it.value.equals(input.name.trim(), ignoreCase = true) }?.let {
            throw IllegalArgumentException("this exercise already exists with key ${it.key}")
        }
        val id = dao.insertSupersedingProposal(
            CoachProposalEntity(
                type = ProposalType.NEW_EXERCISE, state = ProposalState.PENDING, summary = input.summary,
                payload = coachJson.encodeToString(
                    ProposalPayload.serializer(),
                    ProposalPayload(newExercise = NewExercisePayload(input.name.trim(), input.primaryGroupKey, input.secondaryGroupKeys)),
                ),
                subjectKey = "exercise:${input.name.trim().lowercase()}", messageId = null, createdAt = clock.now(),
            ),
        )
        return ToolRun("Proposal saved. Once the user accepts it, find_exercises returns its key.", proposalId = id)
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

    private suspend fun proposePlan(inputJson: String, type: ProposalType): ToolRun {
        val input = coachJson.decodeFromString<ProposePlanInput>(inputJson)
        require(type == ProposalType.NEW_PLAN || input.planId != null) { "planId is required for a change" }
        validate(PlanPayload(input.planId, input.name, input.date, input.sets))
        val subject = input.planId?.let { "plan:$it" }
        val id = dao.insertSupersedingProposal(
            CoachProposalEntity(
                type = type, state = ProposalState.PENDING, summary = input.summary,
                payload = coachJson.encodeToString(ProposalPayload.serializer(), ProposalPayload(plans = listOf(PlanPayload(input.planId, input.name, input.date, input.sets)))),
                subjectKey = subject, messageId = null, createdAt = clock.now(),
            ),
        )
        return ToolRun("Proposal saved. The user will see it as a card and must accept it.", proposalId = id)
    }

    private suspend fun proposePlans(inputJson: String): ToolRun {
        val input = coachJson.decodeFromString<ProposePlansInput>(inputJson)
        require(input.plans.isNotEmpty()) { "plans must not be empty" }
        input.plans.forEach { validate(it) }
        val id = dao.insertProposal(
            CoachProposalEntity(
                type = ProposalType.MULTI_PLAN, state = ProposalState.PENDING, summary = input.summary,
                payload = coachJson.encodeToString(ProposalPayload.serializer(), ProposalPayload(plans = input.plans)),
                subjectKey = null, messageId = null, createdAt = clock.now(),
            ),
        )
        return ToolRun("Proposal saved. The user will see it as a card and must accept it.", proposalId = id)
    }

    private suspend fun proposeTarget(inputJson: String): ToolRun {
        val input = coachJson.decodeFromString<ProposeTargetInput>(inputJson)
        require(input.minSets in 0..input.maxSets) { "minSets must be between 0 and maxSets" }
        val current = volume.observeTargets().first().firstOrNull { it.groupKey == input.groupKey }
            ?: throw IllegalArgumentException("unknown muscle group ${input.groupKey}")
        val id = dao.insertSupersedingProposal(
            CoachProposalEntity(
                type = ProposalType.TARGET, state = ProposalState.PENDING, summary = input.summary,
                payload = coachJson.encodeToString(
                    ProposalPayload.serializer(),
                    ProposalPayload(groupKey = input.groupKey, minSets = input.minSets, maxSets = input.maxSets, reason = input.reason, previousMinSets = current.minSets, previousMaxSets = current.maxSets),
                ),
                subjectKey = "target:${input.groupKey}", messageId = null, createdAt = clock.now(),
            ),
        )
        return ToolRun("Proposal saved. The user will see it as a card and must accept it.", proposalId = id)
    }

    private suspend fun validate(plan: PlanPayload) {
        require(plan.name.isNotBlank()) { "plan name is empty" }
        require(plan.sets.isNotEmpty()) { "plan has no sets" }
        plan.date?.let { LocalDate.parse(it) }
        val known = exerciseNames().keys
        plan.sets.forEach {
            require(it.exerciseKey in known) { "unknown exerciseKey ${it.exerciseKey}; use find_exercises" }
            require(it.reps > 0) { "reps must be positive" }
        }
    }

    private suspend fun addNote(text: String): ToolRun {
        require(text.isNotBlank()) { "text is empty" }
        val id = dao.insertNote(CoachNoteEntity(text = text.trim(), createdAt = clock.now()))
        return ToolRun("Note saved with id $id.")
    }

    private suspend fun updateNote(id: Int, text: String): ToolRun =
        if (dao.updateNote(id.toLong(), text.trim()) > 0) ToolRun("Note updated.") else ToolRun("No note with id $id.", isError = true)

    private suspend fun deleteNote(id: Int): ToolRun =
        if (dao.deleteNote(id.toLong()) > 0) ToolRun("Note deleted.") else ToolRun("No note with id $id.", isError = true)

    private suspend fun updateProfile(field: String, value: String): ToolRun {
        require(field in CoachService.PROFILE_FIELDS) { "field must be one of ${CoachService.PROFILE_FIELDS.joinToString()}" }
        if (value.isBlank()) dao.deleteProfileField(field) else dao.upsertProfile(CoachProfileEntity(field, value.trim()))
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

    private fun tool(name: String, description: String, schema: String) = ClaudeTool(name, description, schema)

    private fun JsonObject.string(key: String): String = this[key]?.jsonPrimitive?.contentOrNull ?: throw IllegalArgumentException("$key is required")

    private fun JsonObject.int(key: String): Int? = this[key]?.jsonPrimitive?.contentOrNull?.toIntOrNull()

    private fun JsonObject.date(key: String): LocalDate = LocalDate.parse(string(key))
}

@Serializable
internal data class ProposePlanInput(
    val planId: Long? = null,
    val name: String,
    val date: String? = null,
    val summary: String,
    val sets: List<PlannedSetPayload>,
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
    val sets: List<PlannedSetPayload> = emptyList(),
    val removeExerciseKeys: List<String> = emptyList(),
)

@Serializable
internal data class ProposePlansInput(val summary: String, val plans: List<PlanPayload>)

@Serializable
internal data class ProposeTargetInput(
    val groupKey: String,
    val minSets: Int,
    val maxSets: Int,
    val reason: String,
    val summary: String,
)

private fun setItemSchema() =
    """{"type":"object","properties":{"exerciseKey":{"type":"string"},"reps":{"type":"integer"},"weightKg":{"type":"number"},"rpe":{"type":"number"},"warmup":{"type":"boolean"}},"required":["exerciseKey","reps"]}"""

private fun planItemSchema() =
    """{"type":"object","properties":{"planId":{"type":"integer"},"name":{"type":"string"},"date":{"type":"string"},"sets":{"type":"array","items":${setItemSchema()}}},"required":["name","sets"]}"""

private fun planSchema(withId: Boolean) =
    """{"type":"object","properties":{${if (withId) """"planId":{"type":"integer"},""" else ""}"name":{"type":"string"},"date":{"type":"string","description":"YYYY-MM-DD, optional"},"summary":{"type":"string"},"sets":{"type":"array","items":${setItemSchema()}}},"required":[${if (withId) """"planId",""" else ""}"name","summary","sets"]}"""
