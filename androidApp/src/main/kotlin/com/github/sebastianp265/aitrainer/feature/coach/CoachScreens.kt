package com.github.sebastianp265.aitrainer.feature.coach

import com.github.sebastianp265.aitrainer.integration.coach.CoachTopic
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.coach.impl.ApiKeyContent
import com.github.sebastianp265.aitrainer.feature.coach.impl.ApiKeyViewModel
import com.github.sebastianp265.aitrainer.feature.coach.impl.CoachActions
import com.github.sebastianp265.aitrainer.feature.coach.impl.CoachContent
import com.github.sebastianp265.aitrainer.feature.coach.impl.CoachViewModel
import com.github.sebastianp265.aitrainer.feature.coach.impl.MemoryActions
import com.github.sebastianp265.aitrainer.feature.coach.impl.MemoryContent
import com.github.sebastianp265.aitrainer.feature.coach.impl.MemoryViewModel
import com.github.sebastianp265.aitrainer.feature.coach.impl.ModelPickerContent
import com.github.sebastianp265.aitrainer.feature.coach.impl.ModelPickerViewModel
import com.github.sebastianp265.aitrainer.feature.coach.impl.ProposalPreviewContent
import com.github.sebastianp265.aitrainer.feature.coach.impl.ProposalPreviewViewModel
import org.koin.compose.viewmodel.koinViewModel

@Composable
internal fun CoachScreen(topic: CoachTopic?, onOpenPreview: (Long) -> Unit, onOpenTargets: () -> Unit, onOpenApiKey: () -> Unit) {
    val viewModel: CoachViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(viewModel) { viewModel.startWith(topic) }
    val actions = remember(viewModel) {
        object : CoachActions {
            override fun send(text: String) = viewModel.send(text)
            override fun retry() = viewModel.retry()
            override fun clearTopic() = viewModel.clearTopic()
            override fun accept(proposalId: Long) = viewModel.accept(proposalId)
            override fun undo(proposalId: Long) = viewModel.undo(proposalId)
            override fun preview(proposalId: Long) = onOpenPreview(proposalId)
            override fun openTargets() = onOpenTargets()
            override fun openApiKey() = onOpenApiKey()
        }
    }
    CoachContent(state, actions)
}

@Composable
internal fun CoachMemoryScreen(onOpenTargets: () -> Unit) {
    val viewModel: MemoryViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = remember(viewModel) {
        object : MemoryActions {
            override fun setProfile(field: String, value: String) = viewModel.setProfile(field, value)
            override fun updateNote(id: Long, text: String) = viewModel.updateNote(id, text)
            override fun deleteNote(id: Long) = viewModel.deleteNote(id)
            override fun openTargets() = onOpenTargets()
        }
    }
    MemoryContent(state, actions)
}

@Composable
internal fun CoachProposalScreen(proposalId: Long, onAccepted: () -> Unit, onReply: () -> Unit) {
    val viewModel: ProposalPreviewViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(proposalId) { viewModel.show(proposalId) }
    ProposalPreviewContent(state, onAccept = { viewModel.accept(onAccepted) }, onReply = onReply)
}

@Composable
internal fun ApiKeyScreen() {
    val viewModel: ApiKeyViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ApiKeyContent(state, onSave = viewModel::saveAndCheck, onRemove = viewModel::remove)
}

@Composable
internal fun ModelPickerScreen() {
    val viewModel: ModelPickerViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    ModelPickerContent(state, onSelect = viewModel::select)
}
