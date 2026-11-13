package com.github.sebastianp265.aitrainer.feature.coach.impl

import androidx.compose.foundation.clickable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.ai.AiCacheDuration
import com.github.sebastianp265.aitrainer.integration.ai.AiEffort
import com.github.sebastianp265.aitrainer.integration.ai.AiThinking
import com.github.sebastianp265.aitrainer.integration.ai.AiTuning
import com.github.sebastianp265.aitrainer.integration.coach.CoachRun
import com.github.sebastianp265.aitrainer.shared.design.CardLabel
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons
import com.github.sebastianp265.aitrainer.shared.localization.formatNumber
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun ModelTuningSection(model: String, tuning: AiTuning, onTune: (AiTuning) -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            Modifier.fillMaxWidth().clickable { expanded = !expanded }.padding(vertical = 4.dp).testTag(ModelTuningTags.TOGGLE),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.model_tuning_title), style = MaterialTheme.typography.titleSmall)
            Icon(DesignIcons.ChevronRight, contentDescription = null, modifier = Modifier.size(20.dp).rotate(if (expanded) 90f else 0f))
        }
        if (expanded) TuningOptions(model, tuning, onTune)
    }
}

@Composable
private fun TuningOptions(model: String, tuning: AiTuning, onTune: (AiTuning) -> Unit) {
    val canTurnThinkingOff = model in AiTuning.MODELS_WITH_THINKING_OFF &&
        (tuning.effort ?: AiTuning.HIGHEST_EFFORT_WITH_THINKING_OFF) <= AiTuning.HIGHEST_EFFORT_WITH_THINKING_OFF
    val thinking = if (canTurnThinkingOff) tuning.thinking else AiThinking.ADAPTIVE
    Surface(shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceContainer) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Choice(stringResource(R.string.model_effort), stringResource(R.string.model_effort_hint)) {
                Option(stringResource(R.string.model_default), tuning.effort == null, ModelTuningTags.option("effort", "default")) { onTune(tuning.copy(effort = null)) }
                AiEffort.entries.forEach { effort ->
                    Option(stringResource(effort.labelRes()), tuning.effort == effort, ModelTuningTags.option("effort", effort.name)) { onTune(tuning.copy(effort = effort)) }
                }
            }
            Choice(
                stringResource(R.string.model_thinking),
                stringResource(if (canTurnThinkingOff) R.string.model_thinking_hint else R.string.model_thinking_always_on),
            ) {
                Option(stringResource(R.string.model_thinking_adaptive), thinking == AiThinking.ADAPTIVE, ModelTuningTags.option("thinking", "adaptive")) {
                    onTune(tuning.copy(thinking = AiThinking.ADAPTIVE))
                }
                Option(stringResource(R.string.model_thinking_off), thinking == AiThinking.OFF, ModelTuningTags.option("thinking", "off"), enabled = canTurnThinkingOff) {
                    onTune(tuning.copy(thinking = AiThinking.OFF))
                }
            }
            MaxTokensField(tuning.maxTokens) { onTune(tuning.copy(maxTokens = it)) }
            Choice(stringResource(R.string.model_cache), stringResource(R.string.model_cache_hint)) {
                Option(stringResource(R.string.model_cache_minutes), tuning.cacheDuration == AiCacheDuration.FIVE_MINUTES, ModelTuningTags.option("cache", "minutes")) {
                    onTune(tuning.copy(cacheDuration = AiCacheDuration.FIVE_MINUTES))
                }
                Option(stringResource(R.string.model_cache_hour), tuning.cacheDuration == AiCacheDuration.ONE_HOUR, ModelTuningTags.option("cache", "hour")) {
                    onTune(tuning.copy(cacheDuration = AiCacheDuration.ONE_HOUR))
                }
            }
            TextButton(onClick = { onTune(AiTuning()) }, enabled = tuning != AiTuning(), modifier = Modifier.testTag(ModelTuningTags.RESET)) {
                Text(stringResource(R.string.model_tuning_reset))
            }
        }
    }
}

private fun AiEffort.labelRes() = when (this) {
    AiEffort.LOW -> R.string.model_effort_low
    AiEffort.MEDIUM -> R.string.model_effort_medium
    AiEffort.HIGH -> R.string.model_effort_high
    AiEffort.XHIGH -> R.string.model_effort_xhigh
    AiEffort.MAX -> R.string.model_effort_max
}

@Composable
private fun MaxTokensField(maxTokens: Int, onChange: (Int) -> Unit) {
    var text by rememberSaveable(maxTokens) { mutableStateOf(maxTokens.toString()) }
    val valid = text.toIntOrNull() in 1..AiTuning.HIGHEST_MAX_TOKENS
    val highest = formatNumber(AiTuning.HIGHEST_MAX_TOKENS.toDouble())
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(stringResource(R.string.model_max_tokens), style = MaterialTheme.typography.bodyLarge)
        Text(stringResource(R.string.model_max_tokens_hint), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(
            value = text,
            onValueChange = { typed ->
                text = typed.filter(Char::isDigit)
                text.toIntOrNull()?.takeIf { it in 1..AiTuning.HIGHEST_MAX_TOKENS }?.let(onChange)
            },
            singleLine = true,
            isError = !valid,
            suffix = { Text(stringResource(R.string.model_tokens)) },
            supportingText = { Text(stringResource(R.string.model_max_tokens_range, highest)) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = MaterialTheme.shapes.small,
            modifier = Modifier.fillMaxWidth().testTag(ModelTuningTags.MAX_TOKENS),
        )
    }
}

@Composable
private fun Choice(title: String, hint: String, options: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) { options() }
    }
}

@Composable
private fun Option(label: String, selected: Boolean, tag: String, enabled: Boolean = true, onClick: () -> Unit) {
    FilterChip(selected = selected, onClick = onClick, label = { Text(label) }, enabled = enabled, modifier = Modifier.testTag(tag))
}

@Composable
internal fun LastRunCard(run: CoachRun) {
    Card(Modifier.fillMaxWidth().testTag(ModelTuningTags.LAST_RUN)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            CardLabel(stringResource(R.string.model_last_run_title))
            RunRow(stringResource(R.string.model_last_run_rounds), formatNumber(run.rounds.toDouble()))
            RunRow(stringResource(R.string.model_last_run_input), formatNumber(run.inputTokens.toDouble()))
            RunRow(stringResource(R.string.model_last_run_cache_read), formatNumber(run.cacheReadTokens.toDouble()))
            RunRow(stringResource(R.string.model_last_run_cache_write), formatNumber(run.cacheWriteTokens.toDouble()))
            RunRow(stringResource(R.string.model_last_run_output), formatNumber(run.outputTokens.toDouble()))
            RunRow(stringResource(R.string.model_last_run_time), stringResource(R.string.model_seconds, formatNumber(run.duration.inWholeMilliseconds / 1000.0, 1)))
            RunRow(stringResource(R.string.model_last_run_stop), stopReasonLabel(run.stopReason))
        }
    }
}

@Composable
private fun RunRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun stopReasonLabel(stopReason: String?): String = when (stopReason) {
    "end_turn" -> stringResource(R.string.model_stop_end_turn)
    "max_tokens" -> stringResource(R.string.model_stop_max_tokens)
    "tool_use" -> stringResource(R.string.model_stop_tool_use)
    else -> stopReason ?: "–"
}

internal object ModelTuningTags {
    const val TOGGLE = "model_tuning_toggle"
    const val RESET = "model_tuning_reset"
    const val LAST_RUN = "model_last_run"
    const val MAX_TOKENS = "model_tuning_max_tokens"

    fun option(setting: String, value: String) = "model_tuning:$setting:$value"
}

@PreviewLightDark
@Composable
private fun ModelTuningPreview() {
    MaterialTheme {
        Surface {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                TuningOptions("claude-sonnet-5-5", AiTuning(effort = AiEffort.LOW), onTune = {})
                LastRunCard(CoachRun(rounds = 3, inputTokens = 412, cacheReadTokens = 7_930, cacheWriteTokens = 640, outputTokens = 380, duration = 9.seconds, stopReason = "end_turn"))
            }
        }
    }
}
