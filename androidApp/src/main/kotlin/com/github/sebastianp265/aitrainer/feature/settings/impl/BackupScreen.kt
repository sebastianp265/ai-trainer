package com.github.sebastianp265.aitrainer.feature.settings.impl

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.settings.SettingsService
import com.github.sebastianp265.aitrainer.integration.shared.BackupContents
import com.github.sebastianp265.aitrainer.integration.shared.DatabaseBackup
import com.github.sebastianp265.aitrainer.shared.localization.formatDate
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.Instant
import com.github.sebastianp265.aitrainer.shared.design.DesignOutlinedButton

internal enum class BackupMessage { EXPORTED, EXPORT_FAILED, INVALID_FILE, IMPORT_FAILED }

internal data class BackupUiState(
    val lastExport: Instant? = null,
    val message: BackupMessage? = null,
    val pendingImport: BackupContents? = null,
)

internal class BackupViewModel(
    private val databaseBackup: DatabaseBackup,
    private val settingsService: SettingsService,
    private val clock: Clock,
) : ViewModel() {

    private val message = MutableStateFlow<BackupMessage?>(null)
    private val pending = MutableStateFlow<Pair<String, BackupContents>?>(null)

    val uiState: StateFlow<BackupUiState> = combine(settingsService.observeLastExport(), message, pending) { last, message, pending ->
        BackupUiState(last, message, pending?.second)
    }.stateIn(viewModelScope, WhileUiSubscribed, BackupUiState())

    fun export(destination: String) {
        viewModelScope.launch {
            withContext(NonCancellable) {
                if (databaseBackup.export(destination)) {
                    settingsService.setLastExport(clock.now())
                    message.value = BackupMessage.EXPORTED
                } else {
                    message.value = BackupMessage.EXPORT_FAILED
                }
            }
        }
    }

    fun chooseFileToImport(source: String) {
        viewModelScope.launch {
            val contents = databaseBackup.inspect(source)
            if (contents == null) message.value = BackupMessage.INVALID_FILE else pending.value = source to contents
        }
    }

    fun confirmImport() {
        val source = pending.value?.first ?: return
        pending.value = null
        viewModelScope.launch {
            withContext(NonCancellable) {
                if (!databaseBackup.import(source)) message.value = BackupMessage.IMPORT_FAILED
            }
        }
    }

    fun cancelImport() {
        pending.value = null
    }

    fun messageShown() {
        message.value = null
    }
}

@Composable
internal fun BackupContent(
    state: BackupUiState,
    fileName: String,
    onExport: (String) -> Unit,
    onChooseImport: (String) -> Unit,
    onConfirmImport: () -> Unit,
    onCancelImport: () -> Unit,
    onMessageShown: () -> Unit,
) {
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        uri?.let { onExport(it.toString()) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { onChooseImport(it.toString()) }
    }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.backup_export), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.backup_export_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { exportLauncher.launch(fileName) }, Modifier.testTag(BackupTags.EXPORT)) { Text(stringResource(R.string.backup_export_button)) }
                state.lastExport?.let {
                    Text(
                        stringResource(R.string.backup_last_export, formatDate(it.toLocalDateTime(TimeZone.currentSystemDefault()).date)),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.backup_import), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.backup_import_text), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                DesignOutlinedButton(onClick = { importLauncher.launch(arrayOf("*/*")) }, Modifier.testTag(BackupTags.IMPORT)) {
                    Text(stringResource(R.string.backup_import_button))
                }
            }
        }
        Text(stringResource(R.string.backup_footer), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    state.message?.let { message ->
        AlertDialog(
            onDismissRequest = onMessageShown,
            text = { Text(stringResource(messageText(message)), Modifier.testTag(BackupTags.MESSAGE)) },
            confirmButton = { TextButton(onClick = onMessageShown) { Text(stringResource(R.string.ok)) } },
        )
    }
    state.pendingImport?.let { contents ->
        AlertDialog(
            onDismissRequest = onCancelImport,
            title = { Text(stringResource(R.string.backup_confirm_title)) },
            text = { Text(stringResource(R.string.backup_confirm_text, contents.workouts, contents.plans, contents.healthDays, contents.coachNotes)) },
            confirmButton = { TextButton(onClick = onConfirmImport, Modifier.testTag(BackupTags.CONFIRM)) { Text(stringResource(R.string.backup_confirm_button)) } },
            dismissButton = { TextButton(onClick = onCancelImport) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun messageText(message: BackupMessage): Int = when (message) {
    BackupMessage.EXPORTED -> R.string.backup_exported
    BackupMessage.EXPORT_FAILED -> R.string.backup_export_failed
    BackupMessage.INVALID_FILE -> R.string.backup_invalid_file
    BackupMessage.IMPORT_FAILED -> R.string.backup_import_failed
}

internal object BackupTags {
    const val EXPORT = "backup_export"
    const val IMPORT = "backup_import"
    const val CONFIRM = "backup_confirm"
    const val MESSAGE = "backup_message"
}

internal fun backupFileName(now: Instant): String =
    "aitrainer-backup-${now.toLocalDateTime(TimeZone.currentSystemDefault()).date}.db"
