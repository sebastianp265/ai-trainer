package com.github.sebastianp265.aitrainer.ui.debug

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.ui.components.BackTopBar
import com.github.sebastianp265.aitrainer.ui.theme.AITrainerTheme
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DebugScreen(onBack: () -> Unit, viewModel: DebugViewModel = koinViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    DebugContent(
        state = state,
        onReplaceWithDemoData = viewModel::replaceWithDemoData,
        onDemoDataCreatedShown = viewModel::demoDataCreatedShown,
        onBack = onBack,
    )
}

@Composable
fun DebugContent(
    state: DebugUiState,
    onReplaceWithDemoData: () -> Unit,
    onDemoDataCreatedShown: () -> Unit,
    onBack: () -> Unit,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current
    LaunchedEffect(state.showDemoDataCreated) {
        if (state.showDemoDataCreated) {
            snackbarHostState.showSnackbar(resources.getString(R.string.debug_demo_data_created))
            onDemoDataCreatedShown()
        }
    }
    var confirmReplace by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = { BackTopBar(R.string.settings_debug_tools, onBack) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Button(
                onClick = { confirmReplace = true },
                enabled = !state.isReplacingData,
                modifier = Modifier.fillMaxWidth(),
            ) { Text(stringResource(R.string.debug_replace_with_demo_data)) }
            if (state.isReplacingData) CircularProgressIndicator()
        }
    }

    if (confirmReplace) {
        AlertDialog(
            onDismissRequest = { confirmReplace = false },
            title = { Text(stringResource(R.string.debug_replace_confirm_title)) },
            text = { Text(stringResource(R.string.debug_replace_confirm_text)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmReplace = false
                    onReplaceWithDemoData()
                }) { Text(stringResource(R.string.replace)) }
            },
            dismissButton = { TextButton(onClick = { confirmReplace = false }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun DebugPreview() {
    AITrainerTheme {
        DebugContent(DebugUiState(), {}, {}, {})
    }
}
