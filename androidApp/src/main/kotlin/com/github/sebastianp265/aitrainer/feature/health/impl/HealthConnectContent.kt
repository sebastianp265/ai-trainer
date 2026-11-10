package com.github.sebastianp265.aitrainer.feature.health.impl

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.design.GroupCard
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons
import com.github.sebastianp265.aitrainer.shared.design.SectionHeader
import com.github.sebastianp265.aitrainer.shared.design.extendedColors
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import com.github.sebastianp265.aitrainer.shared.design.GroupRow
import androidx.compose.foundation.layout.height
import androidx.compose.material3.LinearProgressIndicator
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAccess
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType
import com.github.sebastianp265.aitrainer.integration.health.HealthStatus
import com.github.sebastianp265.aitrainer.shared.localization.formatDateTime
import kotlin.time.Instant
import com.github.sebastianp265.aitrainer.shared.design.DesignOutlinedButton

internal interface HealthConnectActions {
    fun connect(permissions: Set<String>)
    fun update()
    fun syncNow()
    fun managePermissions()
}

@Composable
internal fun HealthConnectContent(state: HealthConnectUiState, actions: HealthConnectActions) {
    val status = state.status ?: return
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (status.access == HealthConnectAccess.CONNECTED) {
            connectedItems(state, status, actions)
        } else {
            disconnectedItems(status.access, state.requiredPermissions, actions)
        }
    }
}

private fun androidx.compose.foundation.lazy.LazyListScope.connectedItems(
    state: HealthConnectUiState,
    status: HealthStatus,
    actions: HealthConnectActions,
) {
    item {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.hc_connected_summary, status.grantedTypes.size, HealthDataType.entries.size),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(lastSyncText(state.lastSync), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = actions::syncNow, enabled = !state.isSyncing, modifier = Modifier.height(40.dp).testTag(HealthConnectTags.SYNC_NOW)) {
                    Text(stringResource(if (state.isSyncing) R.string.hc_syncing else R.string.hc_sync_now))
                }
                if (state.syncFailed) {
                    Text(stringResource(R.string.hc_sync_failed), color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
    item { SectionHeader(stringResource(R.string.hc_data_types)) }
    item {
        GroupCard {
            HealthDataType.entries.forEachIndexed { index, type ->
                HealthTypeRow(type, isGranted = type in status.grantedTypes, divider = index < HealthDataType.entries.lastIndex)
            }
        }
    }
    item {
        DesignOutlinedButton(onClick = actions::managePermissions, Modifier.fillMaxWidth().height(48.dp)) { Text(stringResource(R.string.hc_change_permissions)) }
    }
    item { SectionHeader(stringResource(R.string.hc_history)) }
    item {
        Text(
            stringResource(if (status.hasHistoryPermission) R.string.hc_history_granted else R.string.hc_history_missing),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    item { Text(stringResource(R.string.hc_optional_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

private fun androidx.compose.foundation.lazy.LazyListScope.disconnectedItems(
    access: HealthConnectAccess,
    permissions: Set<String>,
    actions: HealthConnectActions,
) {
    item {
        val title = when (access) {
            HealthConnectAccess.UNAVAILABLE -> R.string.health_connect_unavailable
            HealthConnectAccess.UPDATE_REQUIRED -> R.string.health_connect_update_required
            else -> R.string.hc_not_connected
        }
        Text(stringResource(title), Modifier.padding(top = 8.dp), style = MaterialTheme.typography.titleMedium)
    }
    item { Text(stringResource(R.string.hc_off_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    item {
        when (access) {
            HealthConnectAccess.NOT_CONNECTED -> Button(
                onClick = { actions.connect(permissions) },
                modifier = Modifier.testTag(HealthConnectTags.CONNECT),
            ) { Text(stringResource(R.string.hc_connect_button)) }
            HealthConnectAccess.UPDATE_REQUIRED -> Button(onClick = actions::update) { Text(stringResource(R.string.health_connect_update)) }
            HealthConnectAccess.UNAVAILABLE -> Button(onClick = actions::update) { Text(stringResource(R.string.hc_install)) }
            HealthConnectAccess.CONNECTED -> Unit
        }
    }
    item { SectionHeader(stringResource(R.string.hc_benefits_title)) }
    item {
        GroupCard {
            Benefit(R.string.hc_benefit_balance, R.string.hc_benefit_balance_text, divider = true)
            Benefit(R.string.hc_benefit_body, R.string.hc_benefit_body_text, divider = true)
            Benefit(R.string.hc_benefit_recovery, R.string.hc_benefit_recovery_text, divider = true)
            Benefit(R.string.hc_benefit_workouts, R.string.hc_benefit_workouts_text, divider = false)
        }
    }
    item { Text(stringResource(R.string.hc_off_choice), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
}

@Composable
private fun Benefit(@StringRes title: Int, @StringRes text: Int, divider: Boolean) {
    GroupRow(divider = divider) {
        RowTexts(stringResource(title), stringResource(text))
    }
}

@Composable
private fun RowScope.RowTexts(title: String, detail: String) {
    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(detail, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun HealthTypeRow(type: HealthDataType, isGranted: Boolean, divider: Boolean) {
    GroupRow(Modifier.testTag(HealthConnectTags.typeRow(type)), divider = divider) {
        RowTexts(stringResource(type.labelRes()), stringResource(type.descriptionRes()))
        val color = if (isGranted) extendedColors.success else extendedColors.warning
        Row(Modifier.padding(end = 12.dp), horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            if (isGranted) Icon(DesignIcons.Check, contentDescription = null, Modifier.size(16.dp), tint = color)
            Text(stringResource(if (isGranted) R.string.hc_access_granted else R.string.hc_access_missing), style = MaterialTheme.typography.bodyMedium, color = color)
        }
    }
}

@Composable
private fun lastSyncText(lastSync: Instant?): String =
    if (lastSync == null) {
        stringResource(R.string.hc_never_synced)
    } else {
        stringResource(R.string.hc_synced_at, formatDateTime(lastSync))
    }

@Composable
internal fun HealthConnectSettingsRowContent(state: HealthConnectUiState, onOpen: () -> Unit) {
    val status = state.status ?: return
    val granted = status.grantedTypes
    val total = HealthDataType.entries.size
    val connected = status.access == HealthConnectAccess.CONNECTED
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        GroupCard {
            GroupRow(Modifier.testTag(HealthConnectTags.SETTINGS_ROW), onClick = onOpen, divider = false) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(R.string.health_connect), style = MaterialTheme.typography.bodyLarge)
                    Text(
                        when {
                            !connected -> stringResource(R.string.hc_not_connected)
                            state.lastSync == null -> stringResource(R.string.hc_row_summary_never_synced, granted.size, total)
                            else -> stringResource(R.string.hc_row_summary, granted.size, total, formatDateTime(state.lastSync))
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (connected) {
                        LinearProgressIndicator(
                            progress = { granted.size.toFloat() / total },
                            modifier = Modifier.fillMaxWidth().padding(top = 6.dp).height(4.dp),
                            drawStopIndicator = {},
                        )
                    }
                }
                Text(
                    stringResource(R.string.settings_manage),
                    Modifier.padding(end = 12.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        val missing = HealthDataType.entries.filterNot { it in granted }
        if (connected && missing.isNotEmpty()) {
            Text(
                stringResource(R.string.hc_row_missing, missing.map { stringResource(it.labelRes()) }.joinToString()),
                Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal object HealthConnectTags {
    const val SYNC_NOW = "health_connect_sync_now"
    const val CONNECT = "health_connect_connect"
    const val SETTINGS_ROW = "health_connect_settings_row"
    fun typeRow(type: HealthDataType) = "health_connect_type:${type.name}"
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
    HealthDataType.WORKOUTS -> R.string.hc_type_workouts
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
    HealthDataType.WORKOUTS -> R.string.hc_type_workouts_text
}

@PreviewLightDark
@Composable
private fun HealthConnectContentPreview() {
    MaterialTheme {
        Surface {
            HealthConnectContent(
                state = HealthConnectUiState(
                    status = HealthStatus(
                        access = HealthConnectAccess.CONNECTED,
                        grantedTypes = HealthDataType.entries.toSet() - HealthDataType.HEART_RATE_VARIABILITY,
                        hasHistoryPermission = true,
                    ),
                    lastSync = Instant.parse("2026-11-10T08:12:00Z"),
                ),
                actions = object : HealthConnectActions {
                    override fun connect(permissions: Set<String>) = Unit
                    override fun update() = Unit
                    override fun syncNow() = Unit
                    override fun managePermissions() = Unit
                },
            )
        }
    }
}
