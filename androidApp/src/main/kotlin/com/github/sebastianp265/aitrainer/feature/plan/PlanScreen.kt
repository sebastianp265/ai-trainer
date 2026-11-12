package com.github.sebastianp265.aitrainer.feature.plan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.sebastianp265.aitrainer.feature.plan.impl.NEW_PLAN_ID
import com.github.sebastianp265.aitrainer.feature.plan.impl.PlanDetailContent
import com.github.sebastianp265.aitrainer.feature.plan.impl.PlanDetailViewModel
import com.github.sebastianp265.aitrainer.feature.plan.impl.PlanEditActions
import com.github.sebastianp265.aitrainer.feature.plan.impl.PlanEditContent
import com.github.sebastianp265.aitrainer.feature.plan.impl.PlanEditViewModel
import kotlinx.datetime.LocalDate
import org.koin.compose.viewmodel.koinViewModel
import androidx.compose.runtime.getValue

internal const val NEW_PLAN = NEW_PLAN_ID

@Composable
internal fun PlanDetailScreen(planId: Long, onBack: () -> Unit, onEdit: () -> Unit, onStarted: () -> Unit, onDeleted: () -> Unit) {
    val viewModel: PlanDetailViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(planId) { viewModel.show(planId) }
    PlanDetailContent(
        state = state,
        onBack = onBack,
        onEdit = onEdit,
        onStart = { viewModel.start(onStarted) },
        onDelete = { viewModel.delete(onDeleted) },
    )
}

@Composable
internal fun PlanEditScreen(
    planId: Long,
    onOpenExercisePicker: () -> Unit,
    pickedExerciseKey: String?,
    onPickedExerciseHandled: () -> Unit,
    onClose: () -> Unit,
    onSaved: (planId: Long) -> Unit,
    onDeleted: () -> Unit,
) {
    val viewModel: PlanEditViewModel = koinViewModel()
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(planId) { viewModel.load(planId) }
    LaunchedEffect(pickedExerciseKey) {
        pickedExerciseKey?.let {
            viewModel.addExercise(it)
            onPickedExerciseHandled()
        }
    }
    val actions = remember(viewModel) {
        object : PlanEditActions {
            override fun changeName(name: String) = viewModel.changeName(name)
            override fun changeDate(date: LocalDate?) = viewModel.changeDate(date)
            override fun addExercise() = onOpenExercisePicker()
            override fun removeExercise(exerciseKey: String) = viewModel.removeExercise(exerciseKey)
            override fun addSet(exerciseKey: String) = viewModel.addSet(exerciseKey)
            override fun removeSet(setId: Long) = viewModel.removeSet(setId)
            override fun changeWeight(setId: Long, weight: String) = viewModel.changeWeight(setId, weight)
            override fun changeReps(setId: Long, reps: String) = viewModel.changeReps(setId, reps)
            override fun changeRpe(setId: Long, rpe: Double?) = viewModel.changeRpe(setId, rpe)
            override fun toggleWarmup(setId: Long) = viewModel.toggleWarmup(setId)
            override fun save() = viewModel.save(onSaved)
            override fun delete() = viewModel.delete(onDeleted)
            override fun close() = onClose()
        }
    }
    PlanEditContent(state, actions)
}
