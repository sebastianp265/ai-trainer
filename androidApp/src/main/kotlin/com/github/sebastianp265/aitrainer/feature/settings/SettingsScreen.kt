package com.github.sebastianp265.aitrainer.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.health.HealthDataSection
import com.github.sebastianp265.aitrainer.feature.settings.impl.BackupContent
import com.github.sebastianp265.aitrainer.feature.settings.impl.BackupViewModel
import com.github.sebastianp265.aitrainer.feature.settings.impl.SettingsActions
import com.github.sebastianp265.aitrainer.feature.settings.impl.SettingsContent
import com.github.sebastianp265.aitrainer.feature.settings.impl.SettingsViewModel
import com.github.sebastianp265.aitrainer.feature.settings.impl.backupFileName
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.getValue

@Composable
internal fun SettingsScreen(
    onOpenAi: () -> Unit,
    onOpenMemory: () -> Unit,
    onOpenBackup: () -> Unit,
) {
    val viewModel: SettingsViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        object : SettingsActions {
            override fun setCoachEnabled(enabled: Boolean) = viewModel.setCoachEnabled(enabled)
            override fun openAi() = onOpenAi()
            override fun openMemory() = onOpenMemory()
            override fun openBackup() = onOpenBackup()
        }
    }
    SettingsContent(state, actions, healthDataSection = { HealthDataSection() })
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
