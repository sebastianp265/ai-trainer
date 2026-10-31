package com.github.sebastianp265.aitrainer.feature.progress.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.core.ui.formatDate
import com.github.sebastianp265.aitrainer.core.ui.formatGrams
import com.github.sebastianp265.aitrainer.core.ui.formatKcal
import com.github.sebastianp265.aitrainer.core.ui.theme.AITrainerTheme
import com.github.sebastianp265.aitrainer.feature.health.domain.model.HealthDay
import com.github.sebastianp265.aitrainer.feature.health.domain.usecase.HealthConnectAccess
import com.github.sebastianp265.aitrainer.feature.health.ui.HealthDataUsageActivity
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel

@Preview(showBackground = true)
@Composable
private fun ProgressPreview() {
    AITrainerTheme {
        ProgressContent(
            ProgressUiState(
                healthConnectAccess = HealthConnectAccess.CONNECTED,
                healthDays = listOf(
                    HealthDay(LocalDate(2026, 10, 29), 2100, 140.0, 230.0, 70.0, listOf("com.fitatu.tracker")),
                    HealthDay(LocalDate(2026, 10, 28), 1900, null, null, null, listOf("com.fitatu.tracker")),
                ),
            ),
            NoOpProgressActions,
        )
    }
}

private object NoOpProgressActions : ProgressActions {
    override fun connectHealthConnect(permissions: Set<String>) {}
    override fun updateHealthConnect() {}
    override fun openHealthDataUsage() {}
}

object ProgressTags {
    const val HEALTH_CONNECT_STATUS = "progress_health_connect_status"

    fun healthDay(date: LocalDate) = "progress_health_day:$date"
    fun healthDaySources(date: LocalDate) = "progress_health_day_sources:$date"
}

interface ProgressActions {
    fun connectHealthConnect(permissions: Set<String>)
    fun updateHealthConnect()
    fun openHealthDataUsage()
}

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        viewModel.refresh()
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.refresh()
        onPauseOrDispose {}
    }
    val actions = remember(context, permissionLauncher) {
        object : ProgressActions {
            override fun connectHealthConnect(permissions: Set<String>) = permissionLauncher.launch(permissions)
            override fun updateHealthConnect() = context.openHealthConnectInPlayStore()
            override fun openHealthDataUsage() = context.startActivity(Intent(context, HealthDataUsageActivity::class.java))
        }
    }
    ProgressContent(state, actions)
}

private const val HEALTH_CONNECT_PLAY_STORE_URI =
    "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"

private fun Context.openHealthConnectInPlayStore() {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, HEALTH_CONNECT_PLAY_STORE_URI.toUri()))
    } catch (_: ActivityNotFoundException) {
    }
}

@Composable
fun ProgressContent(state: ProgressUiState, actions: ProgressActions) {
    val access = state.healthConnectAccess
    if (access == null) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (access != HealthConnectAccess.CONNECTED) {
            item { HealthConnectCard(access, state.healthConnectPermissions, actions) }
        }
        state.healthDays?.let { healthDaysSection(it) }
    }
}

@Composable
private fun HealthConnectCard(access: HealthConnectAccess, permissions: Set<String>, actions: ProgressActions) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.health_connect), style = MaterialTheme.typography.titleMedium)
            val description = when (access) {
                HealthConnectAccess.UNAVAILABLE -> R.string.health_connect_unavailable
                HealthConnectAccess.UPDATE_REQUIRED -> R.string.health_connect_update_required
                HealthConnectAccess.NOT_CONNECTED -> R.string.health_connect_not_connected
                HealthConnectAccess.CONNECTED -> null
            }
            description?.let { Text(stringResource(it), Modifier.testTag(ProgressTags.HEALTH_CONNECT_STATUS)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (access) {
                    HealthConnectAccess.NOT_CONNECTED ->
                        Button(onClick = { actions.connectHealthConnect(permissions) }) { Text(stringResource(R.string.health_connect_connect)) }
                    HealthConnectAccess.UPDATE_REQUIRED ->
                        Button(onClick = actions::updateHealthConnect) { Text(stringResource(R.string.health_connect_update)) }
                    HealthConnectAccess.UNAVAILABLE, HealthConnectAccess.CONNECTED -> Unit
                }
                TextButton(onClick = actions::openHealthDataUsage) { Text(stringResource(R.string.health_connect_how_data_is_used)) }
            }
        }
    }
}

private fun LazyListScope.healthDaysSection(days: List<HealthDay>) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.health_data_title), style = MaterialTheme.typography.titleLarge)
            Text(stringResource(R.string.health_data_subtitle), style = MaterialTheme.typography.bodyMedium)
            if (days.isEmpty()) Text(stringResource(R.string.health_data_empty))
        }
    }
    items(days, key = { it.date.toString() }) { day ->
        ListItem(
            headlineContent = { Text(formatDate(day.date)) },
            supportingContent = {
                Column {
                    Text(day.summary(), Modifier.testTag(ProgressTags.healthDay(day.date)))
                    Text(
                        day.sources(),
                        Modifier.testTag(ProgressTags.healthDaySources(day.date)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            },
        )
    }
}

@Composable
private fun HealthDay.summary(): String = stringResource(
    R.string.health_data_day,
    kcal(kcalEaten),
    grams(proteinG),
    grams(carbsG),
    grams(fatG),
)

@Composable
private fun HealthDay.sources(): String = stringResource(
    R.string.health_data_sources,
    packages(sourcePackages),
)

@Composable
private fun packages(packageNames: List<String>): String =
    packageNames.joinToString().ifEmpty { stringResource(R.string.value_missing) }

@Composable
private fun kcal(value: Int?): String =
    value?.let { stringResource(R.string.value_kcal, formatKcal(it)) } ?: stringResource(R.string.value_missing)

@Composable
private fun grams(value: Double?): String =
    value?.let { stringResource(R.string.value_grams, formatGrams(it)) } ?: stringResource(R.string.value_missing)
