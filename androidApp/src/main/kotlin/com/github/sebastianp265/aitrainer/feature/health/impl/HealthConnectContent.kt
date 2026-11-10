package com.github.sebastianp265.aitrainer.feature.health.impl

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAccess
import com.github.sebastianp265.aitrainer.integration.health.HealthDataType
import com.github.sebastianp265.aitrainer.integration.health.HealthStatus
import com.github.sebastianp265.aitrainer.shared.localization.formatDateTime
import kotlin.time.Instant

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
        contentPadding = PaddingValues(16.dp),
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
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.hc_connected_summary, status.grantedTypes.size, HealthDataType.entries.size),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(lastSyncText(state.lastSync), style = MaterialTheme.typography.bodyMedium)
        }
    }
    item {
        Button(onClick = actions::syncNow, enabled = !state.isSyncing, modifier = Modifier.testTag(HealthConnectTags.SYNC_NOW)) {
            Text(stringResource(if (state.isSyncing) R.string.hc_syncing else R.string.hc_sync_now))
        }
    }
    item { Text(stringResource(R.string.hc_data_types), style = MaterialTheme.typography.titleMedium) }
    item {
        Column {
            HealthDataType.entries.forEach { type ->
                HealthTypeRow(type, isGranted = type in status.grantedTypes)
                HorizontalDivider()
            }
        }
    }
    item { OutlinedButton(onClick = actions::managePermissions) { Text(stringResource(R.string.hc_change_permissions)) } }
    item { Text(stringResource(R.string.hc_history), style = MaterialTheme.typography.titleMedium) }
    item {
        Text(
            stringResource(if (status.hasHistoryPermission) R.string.hc_history_granted else R.string.hc_history_missing),
            style = MaterialTheme.typography.bodyMedium,
        )
    }
    item { Text(stringResource(R.string.hc_optional_note), style = MaterialTheme.typography.bodySmall) }
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
        Text(stringResource(title), style = MaterialTheme.typography.titleMedium)
    }
    item { Text(stringResource(R.string.hc_off_note), style = MaterialTheme.typography.bodyMedium) }
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
    item { Text(stringResource(R.string.hc_benefits_title), style = MaterialTheme.typography.titleMedium) }
    item {
        Column {
            Benefit(R.string.hc_benefit_balance, R.string.hc_benefit_balance_text)
            Benefit(R.string.hc_benefit_body, R.string.hc_benefit_body_text)
            Benefit(R.string.hc_benefit_recovery, R.string.hc_benefit_recovery_text)
            Benefit(R.string.hc_benefit_workouts, R.string.hc_benefit_workouts_text)
        }
    }
    item { Text(stringResource(R.string.hc_off_choice), style = MaterialTheme.typography.bodySmall) }
}

@Composable
private fun Benefit(@StringRes title: Int, @StringRes text: Int) {
    ListItem(
        headlineContent = { Text(stringResource(title)) },
        supportingContent = { Text(stringResource(text)) },
    )
}

@Composable
private fun HealthTypeRow(type: HealthDataType, isGranted: Boolean) {
    ListItem(
        headlineContent = { Text(stringResource(type.labelRes())) },
        supportingContent = { Text(stringResource(type.descriptionRes())) },
        trailingContent = {
            Text(
                stringResource(if (isGranted) R.string.hc_access_granted else R.string.hc_access_missing),
                style = MaterialTheme.typography.labelMedium,
            )
        },
        modifier = Modifier.testTag(HealthConnectTags.typeRow(type)),
    )
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
    Column(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen).testTag(HealthConnectTags.SETTINGS_ROW).padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(stringResource(R.string.health_connect), style = MaterialTheme.typography.titleMedium)
        if (status.access != HealthConnectAccess.CONNECTED) {
            Text(stringResource(R.string.hc_not_connected), style = MaterialTheme.typography.bodyMedium)
        } else {
            val total = HealthDataType.entries.size
            Text(
                if (state.lastSync == null) {
                    stringResource(R.string.hc_row_summary_never_synced, granted.size, total)
                } else {
                    stringResource(R.string.hc_row_summary, granted.size, total, formatDateTime(state.lastSync))
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            val missing = HealthDataType.entries.filterNot { it in granted }
            val missingNames = missing.map { stringResource(it.labelRes()) }
            if (missing.isNotEmpty()) {
                Text(
                    stringResource(R.string.hc_row_missing, missingNames.joinToString()),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
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
