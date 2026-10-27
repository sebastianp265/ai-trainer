package com.github.sebastianp265.aitrainer.ui.progress

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup
import com.github.sebastianp265.aitrainer.domain.model.WeightMeasurement
import com.github.sebastianp265.aitrainer.domain.model.WeightSource
import com.github.sebastianp265.aitrainer.domain.stats.DailyWeight
import com.github.sebastianp265.aitrainer.domain.stats.EnergyAverages
import com.github.sebastianp265.aitrainer.domain.stats.MuscleGroupVolume
import com.github.sebastianp265.aitrainer.ui.formatBodyWeight
import com.github.sebastianp265.aitrainer.ui.formatDate
import com.github.sebastianp265.aitrainer.ui.formatKcal
import com.github.sebastianp265.aitrainer.ui.formatSets
import com.github.sebastianp265.aitrainer.ui.formatSignedKcal
import com.github.sebastianp265.aitrainer.ui.formatSignedKg
import com.github.sebastianp265.aitrainer.ui.healthconnect.HealthDataUsageActivity
import com.github.sebastianp265.aitrainer.ui.labelRes
import com.github.sebastianp265.aitrainer.ui.theme.AITrainerTheme
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.Instant

object ProgressTags {
    const val HEALTH_CONNECT_STATUS = "progress_health_connect_status"
    const val LATEST_WEIGHT = "progress_latest_weight"
    const val WEIGHT_AVERAGE = "progress_weight_average"
    const val WEIGHT_TREND = "progress_weight_trend"
    const val WEIGHT_INPUT = "progress_weight_input"
    const val AVERAGE_INTAKE = "progress_average_intake"
    const val AVERAGE_BURN = "progress_average_burn"
    const val AVERAGE_BALANCE = "progress_average_balance"

    fun muscleSets(muscleGroup: MuscleGroup) = "progress_muscle_sets:${muscleGroup.name}"
}

interface ProgressActions {
    fun connectHealthConnect(permissions: Set<String>)
    fun syncHealthConnectNow()
    fun updateHealthConnect()
    fun openHealthDataUsage()
    fun addWeight(weightKg: Double)
    fun deleteMeasurement(measurement: WeightMeasurement)
}

@Composable
fun ProgressScreen(viewModel: ProgressViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val permissionLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) {
        viewModel.onHealthConnectPermissionsResult()
    }
    LifecycleResumeEffect(viewModel) {
        viewModel.refreshHealthConnectStatus()
        onPauseOrDispose {}
    }
    val actions = remember(viewModel, context, permissionLauncher) {
        object : ProgressActions {
            override fun connectHealthConnect(permissions: Set<String>) = permissionLauncher.launch(permissions)
            override fun syncHealthConnectNow() = viewModel.syncHealthConnectNow()
            override fun updateHealthConnect() = context.openHealthConnectInPlayStore()
            override fun openHealthDataUsage() = context.startActivity(Intent(context, HealthDataUsageActivity::class.java))
            override fun addWeight(weightKg: Double) = viewModel.addWeight(weightKg)
            override fun deleteMeasurement(measurement: WeightMeasurement) = viewModel.deleteMeasurement(measurement)
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
    if (state.isLoading) {
        Box(Modifier.fillMaxSize()) { CircularProgressIndicator(Modifier.align(Alignment.Center)) }
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item { HealthConnectCard(state.healthConnectStatus, state.healthConnectPermissions, actions) }
        bodyWeightSection(state, actions)
        item { HorizontalDivider() }
        calorieBalanceSection(state)
        item { HorizontalDivider() }
        trainingVolumeSection(state.lastSevenDaysVolume)
    }
}

@Composable
private fun HealthConnectCard(status: HealthConnectStatus, permissions: Set<String>, actions: ProgressActions) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.health_connect), style = MaterialTheme.typography.titleMedium)
            val description = when (status) {
                HealthConnectStatus.CHECKING -> null
                HealthConnectStatus.UNAVAILABLE -> R.string.health_connect_unavailable
                HealthConnectStatus.UPDATE_REQUIRED -> R.string.health_connect_update_required
                HealthConnectStatus.NOT_CONNECTED -> R.string.health_connect_not_connected
                HealthConnectStatus.CONNECTED -> R.string.health_connect_connected
            }
            description?.let { Text(stringResource(it), Modifier.testTag(ProgressTags.HEALTH_CONNECT_STATUS)) }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                when (status) {
                    HealthConnectStatus.NOT_CONNECTED ->
                        Button(onClick = { actions.connectHealthConnect(permissions) }) { Text(stringResource(R.string.health_connect_connect)) }
                    HealthConnectStatus.CONNECTED ->
                        OutlinedButton(onClick = actions::syncHealthConnectNow) { Text(stringResource(R.string.health_connect_sync_now)) }
                    HealthConnectStatus.UPDATE_REQUIRED ->
                        Button(onClick = actions::updateHealthConnect) { Text(stringResource(R.string.health_connect_update)) }
                    HealthConnectStatus.CHECKING, HealthConnectStatus.UNAVAILABLE -> Unit
                }
                TextButton(onClick = actions::openHealthDataUsage) { Text(stringResource(R.string.health_connect_how_data_is_used)) }
            }
        }
    }
}

private fun LazyListScope.bodyWeightSection(state: ProgressUiState, actions: ProgressActions) {
    item {
        var showAddWeight by rememberSaveable { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(R.string.progress_body_weight)
            val summary = state.weightSummary
            LabeledValue(R.string.progress_latest_weight, summary.latestKg?.let { kg(formatBodyWeight(it)) }, ProgressTags.LATEST_WEIGHT)
            LabeledValue(R.string.progress_seven_day_average, summary.sevenDayAverageKg?.let { kg(formatBodyWeight(it)) }, ProgressTags.WEIGHT_AVERAGE)
            LabeledValue(
                R.string.progress_trend,
                summary.weeklyChangeKg?.let { stringResource(R.string.value_kg_per_week, formatSignedKg(it)) },
                ProgressTags.WEIGHT_TREND,
            )
            Button(onClick = { showAddWeight = true }) { Text(stringResource(R.string.progress_add_weight)) }
        }
        if (showAddWeight) {
            AddWeightDialog(
                onSave = {
                    actions.addWeight(it)
                    showAddWeight = false
                },
                onDismiss = { showAddWeight = false },
            )
        }
    }
    item {
        if (state.dailyWeights.isEmpty()) {
            Text(stringResource(R.string.progress_no_weight_yet))
        } else {
            WeightChart(state.chartStart, state.dailyWeights, state.weightMovingAverage)
        }
    }
    items(state.recentMeasurements, key = { it.id }) { measurement ->
        ListItem(
            headlineContent = { Text(kg(formatBodyWeight(measurement.weightKg))) },
            supportingContent = { Text(formatDate(measurement.measuredAt.localDate())) },
            trailingContent = {
                IconButton(onClick = { actions.deleteMeasurement(measurement) }) {
                    Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.progress_delete_measurement))
                }
            },
        )
    }
}

private fun LazyListScope.calorieBalanceSection(state: ProgressUiState) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(R.string.progress_calorie_balance)
            Text(stringResource(R.string.progress_last_7_days), style = MaterialTheme.typography.labelLarge)
            EnergySummary(state.lastSevenDaysEnergy)
        }
    }
    item {
        var showWeekly by rememberSaveable { mutableStateOf(false) }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !showWeekly, onClick = { showWeekly = false }, label = { Text(stringResource(R.string.progress_daily)) })
                FilterChip(selected = showWeekly, onClick = { showWeekly = true }, label = { Text(stringResource(R.string.progress_weekly)) })
            }
            when {
                showWeekly && state.weeklyBalances.isNotEmpty() -> WeeklyBalanceChart(state.chartStart, state.weeklyBalances)
                !showWeekly && state.dailyBalances.isNotEmpty() -> DailyBalanceChart(state.chartStart, state.dailyBalances)
                else -> Text(stringResource(R.string.progress_no_calorie_data))
            }
        }
    }
}

@Composable
private fun EnergySummary(averages: EnergyAverages) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        LabeledValue(R.string.progress_average_intake, averages.averageIntakeKcal?.let { kcal(formatKcal(it)) }, ProgressTags.AVERAGE_INTAKE)
        LabeledValue(R.string.progress_average_burn, averages.averageBurnKcal?.let { kcal(formatKcal(it)) }, ProgressTags.AVERAGE_BURN)
        LabeledValue(
            R.string.progress_average_balance,
            averages.averageBalanceKcal?.let { kcal(formatSignedKcal(it)) },
            ProgressTags.AVERAGE_BALANCE,
        )
    }
}

private fun LazyListScope.trainingVolumeSection(volumes: List<MuscleGroupVolume>) {
    item {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SectionTitle(R.string.progress_sets_per_muscle_group)
            Text(stringResource(R.string.progress_last_7_days), style = MaterialTheme.typography.labelLarge)
            if (volumes.isEmpty()) Text(stringResource(R.string.progress_no_recent_workouts))
        }
    }
    val maxSets = volumes.maxOfOrNull { it.sets } ?: 1.0
    items(volumes, key = { it.muscleGroup }) { volume ->
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(stringResource(volume.muscleGroup.labelRes()), Modifier.width(140.dp))
            LinearProgressIndicator(progress = { (volume.sets / maxSets).toFloat() }, modifier = Modifier.weight(1f))
            Text(formatSets(volume.sets), Modifier.testTag(ProgressTags.muscleSets(volume.muscleGroup)))
        }
    }
}

@Composable
private fun AddWeightDialog(onSave: (Double) -> Unit, onDismiss: () -> Unit) {
    var input by rememberSaveable { mutableStateOf("") }
    val weightKg = input.replace(',', '.').toDoubleOrNull()?.takeIf { it in PLAUSIBLE_BODY_WEIGHT_KG }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.progress_add_weight)) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(stringResource(R.string.progress_weight_kg)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.testTag(ProgressTags.WEIGHT_INPUT),
            )
        },
        confirmButton = {
            TextButton(onClick = { weightKg?.let(onSave) }, enabled = weightKg != null) { Text(stringResource(R.string.save)) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

private val PLAUSIBLE_BODY_WEIGHT_KG = 20.0..400.0

@Composable
private fun SectionTitle(@StringRes title: Int) {
    Text(stringResource(title), style = MaterialTheme.typography.titleLarge)
}

@Composable
private fun LabeledValue(@StringRes label: Int, value: String?, valueTag: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(stringResource(label), Modifier.weight(1f))
        Text(
            value ?: stringResource(R.string.progress_not_enough_data),
            Modifier.testTag(valueTag),
            style = MaterialTheme.typography.titleMedium,
        )
    }
}

@Composable
private fun kg(value: String) = stringResource(R.string.value_kg, value)

@Composable
private fun kcal(value: String) = stringResource(R.string.value_kcal, value)

private fun Instant.localDate(): LocalDate = toLocalDateTime(TimeZone.currentSystemDefault()).date

private object NoOpProgressActions : ProgressActions {
    override fun connectHealthConnect(permissions: Set<String>) {}
    override fun syncHealthConnectNow() {}
    override fun updateHealthConnect() {}
    override fun openHealthDataUsage() {}
    override fun addWeight(weightKg: Double) {}
    override fun deleteMeasurement(measurement: WeightMeasurement) {}
}

@Preview(showBackground = true)
@Composable
private fun ProgressPreview() {
    val start = LocalDate(2026, 9, 14)
    AITrainerTheme {
        ProgressContent(
            ProgressUiState(
                isLoading = false,
                chartStart = start,
                healthConnectStatus = HealthConnectStatus.NOT_CONNECTED,
                weightSummary = WeightSummary(82.1, 82.3, -0.3),
                dailyWeights = listOf(DailyWeight(LocalDate(2026, 10, 24), 82.1)),
                recentMeasurements = listOf(WeightMeasurement(1, Instant.fromEpochMilliseconds(0), 82.1, WeightSource.MANUAL)),
                lastSevenDaysEnergy = EnergyAverages(2200, 2600, -400),
                lastSevenDaysVolume = listOf(MuscleGroupVolume(MuscleGroup.CHEST, 9.0), MuscleGroupVolume(MuscleGroup.TRICEPS, 4.5)),
            ),
            NoOpProgressActions,
        )
    }
}
