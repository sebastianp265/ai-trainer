package com.github.sebastianp265.aitrainer.feature.health.impl

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAccess
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType
import com.github.sebastianp265.aitrainer.integration.health.HealthStatus
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons
import com.github.sebastianp265.aitrainer.shared.design.GroupCard
import com.github.sebastianp265.aitrainer.shared.design.GroupRow
import com.github.sebastianp265.aitrainer.shared.design.extendedColors

internal interface HealthConnectActions {
    fun turnOn(type: HealthDataType)
    fun turnOff(type: HealthDataType)
    fun allowHistory()
    fun update()
    fun managePermissions()
}

@Composable
internal fun HealthDataSectionContent(state: HealthConnectUiState, actions: HealthConnectActions) {
    val status = state.status ?: return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        when (status.access) {
            HealthConnectAccess.UNAVAILABLE -> MissingHealthConnect(R.string.health_connect_unavailable, R.string.hc_install, actions::update)
            HealthConnectAccess.UPDATE_REQUIRED -> MissingHealthConnect(R.string.health_connect_update_required, R.string.health_connect_update, actions::update)
            HealthConnectAccess.NOT_CONNECTED, HealthConnectAccess.CONNECTED -> Switches(status, actions)
        }
    }
}

@Composable
private fun Switches(status: HealthStatus, actions: HealthConnectActions) {
    GroupCard {
        HealthDataType.entries.forEach { type ->
            SwitchRow(type, type in status.enabledTypes, actions)
        }
        HistoryRow(status.hasHistoryAccess, actions)
    }
    Text(stringResource(R.string.hc_switches_note), Modifier.padding(horizontal = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    TextButton(onClick = actions::managePermissions) { Text(stringResource(R.string.hc_change_permissions)) }
}

@Composable
private fun SwitchRow(type: HealthDataType, isOn: Boolean, actions: HealthConnectActions) {
    val title = stringResource(type.labelRes())
    GroupRow {
        RowTexts(title, stringResource(type.descriptionRes()))
        Switch(
            checked = isOn,
            onCheckedChange = { if (it) actions.turnOn(type) else actions.turnOff(type) },
            modifier = Modifier.padding(end = 12.dp).semantics { contentDescription = title }.testTag(HealthConnectTags.switch(type)),
        )
    }
}

@Composable
private fun HistoryRow(hasAccess: Boolean, actions: HealthConnectActions) {
    GroupRow(divider = false) {
        RowTexts(stringResource(R.string.hc_history_access), stringResource(R.string.hc_history_access_text))
        if (hasAccess) {
            Row(Modifier.padding(end = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(DesignIcons.Check, contentDescription = null, Modifier.size(16.dp), tint = extendedColors.success)
                Text(stringResource(R.string.hc_history_allowed), style = MaterialTheme.typography.bodyMedium, color = extendedColors.success)
            }
        } else {
            TextButton(onClick = actions::allowHistory, modifier = Modifier.testTag(HealthConnectTags.ALLOW_HISTORY)) {
                Text(stringResource(R.string.hc_history_allow))
            }
        }
    }
}

@Composable
private fun MissingHealthConnect(@StringRes text: Int, @StringRes action: Int, onClick: () -> Unit) {
    GroupCard {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(text), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = onClick) { Text(stringResource(action)) }
        }
    }
}

@Composable
private fun RowScope.RowTexts(title: String, detail: String) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

internal object HealthConnectTags {
    const val ALLOW_HISTORY = "health_connect_allow_history"

    fun switch(type: HealthDataType) = "health_connect_switch:${type.name}"
}

@StringRes
internal fun HealthDataType.labelRes(): Int = when (this) {
    HealthDataType.NUTRITION -> R.string.hc_type_nutrition
    HealthDataType.CALORIES_BURNED -> R.string.hc_type_calories_burned
    HealthDataType.WEIGHT -> R.string.hc_type_weight
    HealthDataType.STEPS -> R.string.hc_type_steps
    HealthDataType.SLEEP -> R.string.hc_type_sleep
    HealthDataType.RESTING_HEART_RATE -> R.string.hc_type_resting_heart_rate
    HealthDataType.HEART_RATE_VARIABILITY -> R.string.hc_type_hrv
}

@StringRes
private fun HealthDataType.descriptionRes(): Int = when (this) {
    HealthDataType.NUTRITION -> R.string.hc_type_nutrition_text
    HealthDataType.CALORIES_BURNED -> R.string.hc_type_calories_burned_text
    HealthDataType.WEIGHT -> R.string.hc_type_weight_text
    HealthDataType.STEPS -> R.string.hc_type_steps_text
    HealthDataType.SLEEP -> R.string.hc_type_sleep_text
    HealthDataType.RESTING_HEART_RATE -> R.string.hc_type_daily_average_text
    HealthDataType.HEART_RATE_VARIABILITY -> R.string.hc_type_daily_average_text
}

@PreviewLightDark
@Composable
private fun HealthDataSectionPreview() {
    MaterialTheme {
        Surface {
            HealthDataSectionContent(
                state = HealthConnectUiState(
                    status = HealthStatus(
                        access = HealthConnectAccess.CONNECTED,
                        enabledTypes = HealthDataType.entries.toSet() - HealthDataType.HEART_RATE_VARIABILITY,
                        hasHistoryAccess = false,
                    ),
                ),
                actions = object : HealthConnectActions {
                    override fun turnOn(type: HealthDataType) = Unit
                    override fun turnOff(type: HealthDataType) = Unit
                    override fun allowHistory() = Unit
                    override fun update() = Unit
                    override fun managePermissions() = Unit
                },
            )
        }
    }
}
