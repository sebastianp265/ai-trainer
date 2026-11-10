package com.github.sebastianp265.aitrainer.feature.coach.impl

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.coach.ClaudeFailureKind
import com.github.sebastianp265.aitrainer.integration.coach.CoachActivity
import com.github.sebastianp265.aitrainer.integration.coach.CoachMessage
import com.github.sebastianp265.aitrainer.integration.coach.CoachRole
import com.github.sebastianp265.aitrainer.integration.coach.ProposalState
import com.github.sebastianp265.aitrainer.integration.coach.ProposalType
import com.github.sebastianp265.aitrainer.integration.exercise.Exercise
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.shared.localization.formatLongWeekdayDate
import com.github.sebastianp265.aitrainer.shared.localization.formatNumber
import com.github.sebastianp265.aitrainer.shared.localization.formatWeekdayDate
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons
import com.github.sebastianp265.aitrainer.shared.design.DesignOutlinedButton
import com.github.sebastianp265.aitrainer.shared.design.DesignFilterChip
import com.github.sebastianp265.aitrainer.shared.design.extendedColors

internal interface CoachActions {
    fun send(text: String)
    fun retry()
    fun accept(proposalId: Long)
    fun preview(proposalId: Long)
    fun openTargets()
    fun openApiKey()
}

@Composable
internal fun CoachContent(state: CoachUiState, actions: CoachActions, modifier: Modifier = Modifier) {
    when {
        state.isLoading -> Box(modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        !state.hasKey -> CoachNoKey(actions::openApiKey, modifier)
        else -> Conversation(state, actions, modifier)
    }
}

@Composable
private fun CoachNoKey(onOpenApiKey: () -> Unit, modifier: Modifier) {
    Column(
        modifier.fillMaxSize().padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(72.dp).background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(DesignIcons.Key, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
        }
        Text(stringResource(R.string.coach_no_key_title), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(stringResource(R.string.coach_no_key_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Text(stringResource(R.string.coach_no_key_without), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
        Button(onClick = onOpenApiKey, Modifier.height(48.dp).testTag(CoachTags.OPEN_API_KEY)) { Text(stringResource(R.string.coach_enter_api_key)) }
    }
}

@Composable
private fun Conversation(state: CoachUiState, actions: CoachActions, modifier: Modifier) {
    val listState = rememberLazyListState()
    val failed = state.activity as? CoachActivity.Failed
    val working = state.activity is CoachActivity.Working
    val lastIsUser = state.messages.lastOrNull()?.message?.role == CoachRole.USER
    val itemCount = state.messages.size + (if (working) 1 else 0) + (if (failed != null) 1 else 0)
    LaunchedEffect(itemCount) {
        if (itemCount > 0) listState.animateScrollToItem(listState.layoutInfo.totalItemsCount.coerceAtLeast(1) - 1)
    }
    Column(modifier.fillMaxSize().imePadding()) {
        if (failed?.kind == ClaudeFailureKind.NO_NETWORK) OfflineBanner()
        LazyColumn(
            Modifier.weight(1f).fillMaxWidth(),
            state = listState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (state.messages.isEmpty()) {
                item { Greeting(actions::send, enabled = !working) }
            }
            state.messages.forEachIndexed { index, item ->
                if (index == 0 || dayOf(state.messages[index - 1].message.createdAt) != dayOf(item.message.createdAt)) {
                    item(key = "day:${item.message.id}") { DayLabel(item.message) }
                }
                item(key = "message:${item.message.id}") {
                    MessageBubble(item.message, state.exercises, unsent = failed?.kind == ClaudeFailureKind.NO_NETWORK && lastIsUser && index == state.messages.lastIndex)
                }
                items(item.proposals.size, key = { "proposal:${item.proposals[it].proposal.id}" }) { p ->
                    ProposalCard(item.proposals[p], state.exercises, actions)
                }
                if (item.proposals.any { it.proposal.state == ProposalState.PENDING } && index == state.messages.lastIndex) {
                    item(key = "hint:${item.message.id}") {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Text(stringResource(R.string.coach_change_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (state.messages.any { it.message.review != null }) {
                item(key = "review-footer") { Text(stringResource(R.string.review_footer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (working) item(key = "working") { WorkingRow(state.activity) }
            if (failed != null && lastIsUser) item(key = "failed") { FailedRow(failed, actions) }
        }
        InputRow(enabled = !working, onSend = actions::send)
    }
}

@Composable
private fun OfflineBanner() {
    Surface(
        color = extendedColors.warningContainer,
        contentColor = extendedColors.onWarningContainer,
        modifier = Modifier.fillMaxWidth().testTag(CoachTags.OFFLINE_BANNER),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(DesignIcons.Warning, contentDescription = null)
            Text(stringResource(R.string.coach_offline_banner), style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun Greeting(onSend: (String) -> Unit, enabled: Boolean) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        CoachBubble(stringResource(R.string.coach_greeting_1))
        CoachBubble(stringResource(R.string.coach_greeting_2))
        Text(stringResource(R.string.coach_start_from), Modifier.padding(top = 16.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(R.string.coach_chip_plan, R.string.coach_chip_review, R.string.coach_chip_goal, R.string.coach_chip_time).forEach { id ->
                val text = stringResource(id)
                DesignFilterChip(selected = false, onClick = { if (enabled) onSend(text) }, label = text, modifier = Modifier.testTag(CoachTags.CHIP))
            }
        }
    }
}

@Composable
private fun DayLabel(message: CoachMessage) {
    val today = dayOf(kotlin.time.Clock.System.now())
    val text = when (val day = dayOf(message.createdAt)) {
        today -> stringResource(R.string.coach_today)
        else -> formatLongWeekdayDate(day)
    }
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { Text(text, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

private fun dayOf(instant: Instant) = instant.toLocalDateTime(TimeZone.currentSystemDefault()).date

@Composable
private fun MessageBubble(message: CoachMessage, exercises: Map<String, Exercise>, unsent: Boolean) {
    if (message.role == CoachRole.USER) {
        Row(Modifier.fillMaxWidth().alpha(if (unsent) UNSENT_ALPHA else 1f), horizontalArrangement = Arrangement.End) {
            Surface(shape = RoundedCornerShape(18.dp, 18.dp, 6.dp, 18.dp), color = MaterialTheme.colorScheme.primary) {
                Text(
                    message.text,
                    Modifier.padding(horizontal = 14.dp, vertical = 10.dp).widthIn(max = 300.dp).testTag(CoachTags.USER_MESSAGE),
                    style = BubbleTextStyle,
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            message.review?.let { ReviewCard(it) }
            if (message.text.isNotBlank()) CoachBubble(message.text)
            if (message.checked.isNotEmpty()) {
                Text(
                    stringResource(R.string.coach_checked, checkedPhrases(message.checked, exercises)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp).widthIn(max = 300.dp).testTag(CoachTags.CHECKED),
                )
            }
        }
    }
}

private const val UNSENT_ALPHA = 0.6f

private val BubbleTextStyle = androidx.compose.ui.text.TextStyle(fontSize = 15.sp, lineHeight = 21.sp)

@Composable
private fun CoachBubble(text: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
        Surface(shape = RoundedCornerShape(18.dp, 18.dp, 18.dp, 6.dp), color = MaterialTheme.colorScheme.surfaceContainer) {
            Text(text, Modifier.padding(horizontal = 14.dp, vertical = 10.dp).widthIn(max = 300.dp).testTag(CoachTags.COACH_MESSAGE), style = BubbleTextStyle)
        }
    }
}

@Composable
private fun checkedPhrases(tokens: List<String>, exercises: Map<String, Exercise>): String {
    val locale = LocalConfiguration.current.locales[0]
    return tokens.map { token ->
        when {
            token == "workouts" -> stringResource(R.string.coach_checked_workouts)
            token == "health_days" -> stringResource(R.string.coach_checked_health)
            token == "external_workouts" -> stringResource(R.string.coach_checked_external)
            token == "plans" -> stringResource(R.string.coach_checked_plans)
            token == "weekly_sets" -> stringResource(R.string.coach_checked_weekly_sets)
            token.startsWith("exercise:") ->
                stringResource(R.string.coach_checked_exercise, exercises[token.removePrefix("exercise:")]?.displayName(locale) ?: token.removePrefix("exercise:"))
            else -> token
        }
    }.joinToString(", ")
}

@Composable
private fun ProposalCard(item: ProposalUi, exercises: Map<String, Exercise>, actions: CoachActions) {
    val proposal = item.proposal
    val locale = LocalConfiguration.current.locales[0]
    val pending = proposal.state == ProposalState.PENDING
    val superseded = proposal.state == ProposalState.SUPERSEDED
    Card(
        Modifier.fillMaxWidth().padding(end = 24.dp).alpha(if (superseded) UNSENT_ALPHA else 1f).testTag(CoachTags.PROPOSAL),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, if (superseded) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.inversePrimary),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                kindText(proposal.type, proposal.state),
                style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 0.3.sp),
                color = if (superseded) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Text(proposalTitle(item), style = MaterialTheme.typography.titleMedium.copy(fontSize = 17.sp))
            Text(proposal.summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (proposal.type == ProposalType.MULTI_PLAN) {
                proposal.plans.forEach { plan ->
                    val sets = plan.sets.count { !it.isWarmup }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(plan.name, style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp))
                        Text(
                            stringResource(
                                R.string.session_summary,
                                plan.date?.let { formatWeekdayDate(it) } ?: stringResource(R.string.plan_no_date),
                                pluralStringResource(R.plurals.set_count, sets, sets),
                            ),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            item.changes.forEach { change -> Text(changeText(change, exercises, locale), style = MaterialTheme.typography.bodyMedium) }
            proposal.target?.let { target ->
                val group = exercises.values.flatMap { it.primaryMuscles }.firstOrNull { it.key == target.groupKey }
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(group?.displayName(locale) ?: target.groupKey, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp))
                    Text(
                        stringResource(R.string.coach_target_range, target.previousMinSets, target.previousMaxSets),
                        style = MaterialTheme.typography.bodyMedium.copy(textDecoration = TextDecoration.LineThrough),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(" → ", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        stringResource(R.string.coach_target_range, target.minSets, target.maxSets),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    )
                }
                Text(stringResource(R.string.coach_target_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (proposal.target == null) {
                Text(
                    stringResource(
                        R.string.session_summary,
                        pluralStringResource(R.plurals.exercise_count, item.exerciseCount, item.exerciseCount),
                        pluralStringResource(R.plurals.set_count, item.setCount, item.setCount),
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (pending) {
                Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { actions.accept(proposal.id) }, Modifier.testTag(CoachTags.ACCEPT)) {
                        Text(stringResource(if (proposal.type == ProposalType.MULTI_PLAN) R.string.coach_accept_all else R.string.coach_accept))
                    }
                    if (proposal.target == null) {
                        DesignOutlinedButton(onClick = { actions.preview(proposal.id) }, Modifier.testTag(CoachTags.PREVIEW)) { Text(stringResource(R.string.coach_preview)) }
                    } else {
                        DesignOutlinedButton(onClick = actions::openTargets) { Text(stringResource(R.string.coach_change_myself)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun kindText(type: ProposalType, state: ProposalState): String {
    val kind = stringResource(
        when (type) {
            ProposalType.NEW_PLAN -> R.string.coach_kind_new_plan
            ProposalType.CHANGE_PLAN -> R.string.coach_kind_change_plan
            ProposalType.MULTI_PLAN -> R.string.coach_kind_multi_plan
            ProposalType.TARGET -> R.string.coach_kind_target
        },
    )
    return when (state) {
        ProposalState.PENDING -> kind
        ProposalState.ACCEPTED -> kind + " · " + stringResource(R.string.coach_state_accepted)
        ProposalState.SUPERSEDED -> kind + " · " + stringResource(R.string.coach_state_superseded)
    }
}

@Composable
private fun proposalTitle(item: ProposalUi): String {
    val proposal = item.proposal
    if (proposal.target != null) return stringResource(R.string.coach_target_title)
    if (proposal.plans.size > 1) return stringResource(R.string.coach_plans_count, proposal.plans.size)
    val plan = proposal.plans.single()
    return plan.date?.let { stringResource(R.string.coach_plan_title_dated, plan.name, formatLongWeekdayDate(it)) } ?: plan.name
}

@Composable
internal fun changeText(change: PlanChange, exercises: Map<String, Exercise>, locale: java.util.Locale): String {
    fun name(key: String) = exercises[key]?.displayName(locale) ?: key
    return when (change) {
        is PlanChange.Added -> stringResource(R.string.coach_change_added, name(change.exerciseKey))
        is PlanChange.Removed -> stringResource(R.string.coach_change_removed, name(change.exerciseKey), pluralStringResource(R.plurals.set_count, change.setCount, change.setCount))
        is PlanChange.SetsChanged -> stringResource(R.string.coach_change_sets, name(change.exerciseKey), change.from, change.to)
        is PlanChange.WeightChanged -> stringResource(R.string.coach_change_weight, name(change.exerciseKey), formatNumber(change.deltaKg, 1, signed = true))
    }
}

@Composable
private fun WorkingRow(working: CoachActivity.Working) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag(CoachTags.WORKING)) {
        CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
        Text(
            working.tool?.let { stringResource(R.string.coach_working_tool, toolPhrase(it)) } ?: stringResource(R.string.coach_working),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun toolPhrase(tool: String): String = when (tool) {
    "get_workouts" -> stringResource(R.string.coach_checked_workouts)
    "get_exercise_history" -> stringResource(R.string.coach_tool_exercise_history)
    "get_health_days" -> stringResource(R.string.coach_checked_health)
    "get_external_workouts" -> stringResource(R.string.coach_checked_external)
    "get_plans" -> stringResource(R.string.coach_checked_plans)
    "get_weekly_sets" -> stringResource(R.string.coach_checked_weekly_sets)
    "find_exercises" -> stringResource(R.string.coach_tool_find_exercises)
    else -> stringResource(R.string.coach_tool_writing)
}

@Composable
private fun FailedRow(failed: CoachActivity.Failed, actions: CoachActions) {
    when (failed.kind) {
        ClaudeFailureKind.NO_NETWORK -> Row(Modifier.fillMaxWidth().testTag(CoachTags.FAILED), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Icon(DesignIcons.Warning, contentDescription = null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
            Text(stringResource(R.string.coach_not_sent), Modifier.padding(start = 6.dp), style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp), color = MaterialTheme.colorScheme.error)
            TextButton(onClick = actions::retry, Modifier.testTag(CoachTags.RETRY), colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                Text(stringResource(R.string.coach_retry), style = MaterialTheme.typography.labelLarge.copy(fontSize = 13.sp))
            }
        }
        ClaudeFailureKind.KEY_REJECTED, ClaudeFailureKind.NO_KEY -> ErrorCard(R.string.coach_error_key_title, R.string.coach_error_key_text, R.string.coach_error_key_action, actions::openApiKey)
        else -> ErrorCard(R.string.coach_error_unavailable_title, R.string.coach_error_unavailable_text, R.string.coach_retry, actions::retry)
    }
}

@Composable
private fun ErrorCard(title: Int, text: Int, action: Int, onAction: () -> Unit) {
    Surface(
        Modifier.fillMaxWidth().padding(end = 24.dp).testTag(CoachTags.ERROR_CARD),
        shape = MaterialTheme.shapes.medium,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(stringResource(title), style = MaterialTheme.typography.titleSmall)
            Text(stringResource(text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            DesignOutlinedButton(onClick = onAction, Modifier.height(40.dp).testTag(CoachTags.ERROR_ACTION)) { Text(stringResource(action)) }
        }
    }
}

@Composable
private fun InputRow(enabled: Boolean, onSend: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    val colors = MaterialTheme.colorScheme
    Row(
        Modifier.fillMaxWidth().padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            maxLines = 4,
            textStyle = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp, color = colors.onSurface),
            cursorBrush = SolidColor(colors.primary),
            modifier = Modifier.weight(1f).testTag(CoachTags.INPUT),
            decorationBox = { field ->
                Box(
                    Modifier.heightIn(min = 48.dp).background(colors.surfaceContainer, RoundedCornerShape(24.dp)).padding(horizontal = 18.dp, vertical = 12.dp),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    if (text.isEmpty()) Text(stringResource(R.string.coach_input_hint), style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp), color = colors.onSurfaceVariant)
                    field()
                }
            },
        )
        FilledIconButton(
            onClick = {
                onSend(text.trim())
                text = ""
            },
            enabled = enabled && text.isNotBlank(),
            modifier = Modifier.size(48.dp).testTag(CoachTags.SEND),
        ) { Icon(DesignIcons.Send, contentDescription = stringResource(R.string.coach_send)) }
    }
}

internal object CoachTags {
    const val INPUT = "coach_input"
    const val SEND = "coach_send"
    const val USER_MESSAGE = "coach_user_message"
    const val COACH_MESSAGE = "coach_coach_message"
    const val CHECKED = "coach_checked"
    const val PROPOSAL = "coach_proposal"
    const val ACCEPT = "coach_accept"
    const val PREVIEW = "coach_preview"
    const val WORKING = "coach_working"
    const val CHIP = "coach_chip"
    const val OFFLINE_BANNER = "coach_offline_banner"
    const val FAILED = "coach_failed"
    const val RETRY = "coach_retry"
    const val ERROR_CARD = "coach_error_card"
    const val ERROR_ACTION = "coach_error_action"
    const val OPEN_API_KEY = "coach_open_api_key"
    const val REVIEW = "coach_review"
}

@PreviewLightDark
@Composable
private fun CoachPreview() {
    MaterialTheme {
        CoachContent(
            state = CoachUiState(
                isLoading = false,
                hasKey = true,
                messages = listOf(
                    MessageUi(CoachMessage(1, CoachRole.USER, "Plan me a push day", Instant.fromEpochMilliseconds(1_767_600_000_000), emptyList(), emptyList(), null), emptyList()),
                    MessageUi(CoachMessage(2, CoachRole.COACH, "Here is a push day.", Instant.fromEpochMilliseconds(1_767_600_060_000), listOf("workouts"), emptyList(), null), emptyList()),
                ),
            ),
            actions = object : CoachActions {
                override fun send(text: String) = Unit
                override fun retry() = Unit
                override fun accept(proposalId: Long) = Unit
                override fun preview(proposalId: Long) = Unit
                override fun openTargets() = Unit
                override fun openApiKey() = Unit
            },
        )
    }
}
