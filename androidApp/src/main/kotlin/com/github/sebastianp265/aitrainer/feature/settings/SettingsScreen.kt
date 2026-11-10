package com.github.sebastianp265.aitrainer.feature.settings

import android.content.pm.ApplicationInfo
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.health.HealthConnectSettingsRow
import com.github.sebastianp265.aitrainer.feature.settings.impl.BackupContent
import com.github.sebastianp265.aitrainer.feature.settings.impl.BackupViewModel
import com.github.sebastianp265.aitrainer.feature.settings.impl.SettingsActions
import com.github.sebastianp265.aitrainer.feature.settings.impl.SettingsContent
import com.github.sebastianp265.aitrainer.feature.settings.impl.SettingsViewModel
import com.github.sebastianp265.aitrainer.feature.settings.impl.backupFileName
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun SettingsScreen(
    onOpenHealthConnect: () -> Unit,
    onOpenApiKey: () -> Unit,
    onOpenModel: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenDebug: () -> Unit,
    onOpenBackup: () -> Unit,
) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val isDebuggable = (LocalContext.current.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    val actions = remember(viewModel) {
        object : SettingsActions {
            override fun setCoachEnabled(enabled: Boolean) = viewModel.setCoachEnabled(enabled)
            override fun openApiKey() = onOpenApiKey()
            override fun openModel() = onOpenModel()
            override fun openMemory() = onOpenMemory()
            override fun openDebug() = onOpenDebug()
            override fun openBackup() = onOpenBackup()
        }
    }
    SettingsContent(state, actions, showDebug = isDebuggable, healthConnectRow = { HealthConnectSettingsRow(onOpenHealthConnect) })
}

@Composable
internal fun BackupScreen() {
    val viewModel: BackupViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    BackupContent(
        state = state,
        fileName = backupFileName(kotlin.time.Clock.System.now()),
        onExport = viewModel::export,
        onChooseImport = viewModel::chooseFileToImport,
        onConfirmImport = viewModel::confirmImport,
        onCancelImport = viewModel::cancelImport,
        onMessageShown = viewModel::messageShown,
    )
}
