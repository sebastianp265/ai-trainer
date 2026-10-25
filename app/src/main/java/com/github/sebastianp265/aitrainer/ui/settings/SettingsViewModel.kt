package com.github.sebastianp265.aitrainer.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.data.repo.AppSettingsRepository
import com.github.sebastianp265.aitrainer.data.repo.ProfileRepository
import com.github.sebastianp265.aitrainer.data.secrets.Secret
import com.github.sebastianp265.aitrainer.data.secrets.SecretStore
import com.github.sebastianp265.aitrainer.domain.model.Equipment
import com.github.sebastianp265.aitrainer.domain.model.Goal
import com.github.sebastianp265.aitrainer.domain.model.MuscleGroup
import com.github.sebastianp265.aitrainer.domain.model.ProfilePrefs
import com.github.sebastianp265.aitrainer.ui.WhileUiSubscribed
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class SettingsUiState(
    val isLoading: Boolean = true,
    val profile: ProfilePrefs = ProfilePrefs.DEFAULT,
    val claudeModel: String = AppSettingsRepository.DEFAULT_CLAUDE_MODEL,
    val isClaudeApiKeyStored: Boolean = false,
)

class SettingsViewModel(
    private val profileRepository: ProfileRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val secretStore: SecretStore,
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        profileRepository.observe(),
        appSettingsRepository.observeClaudeModel(),
        secretStore.observeIsStored(Secret.CLAUDE_API_KEY),
    ) { profile, claudeModel, isKeyStored ->
        SettingsUiState(isLoading = false, profile = profile, claudeModel = claudeModel, isClaudeApiKeyStored = isKeyStored)
    }.stateIn(viewModelScope, WhileUiSubscribed, SettingsUiState())

    fun setGoal(goal: Goal) = modifyProfile { it.copy(goal = goal) }

    fun changeSessionsPerWeek(delta: Int) = modifyProfile {
        it.copy(sessionsPerWeek = (it.sessionsPerWeek + delta).coerceIn(ProfilePrefs.SESSIONS_PER_WEEK_RANGE))
    }

    fun changeSessionLength(steps: Int) = modifyProfile {
        val lengthMin = it.sessionLengthMin + steps * ProfilePrefs.SESSION_LENGTH_STEP_MIN
        it.copy(sessionLengthMin = lengthMin.coerceIn(ProfilePrefs.SESSION_LENGTH_MIN_RANGE))
    }

    fun toggleWeakMuscleGroup(muscleGroup: MuscleGroup) = modifyProfile {
        it.copy(weakMuscleGroups = it.weakMuscleGroups.toggled(muscleGroup))
    }

    fun toggleEquipment(equipment: Equipment) = modifyProfile {
        it.copy(availableEquipment = it.availableEquipment.toggled(equipment))
    }

    fun setNotes(notes: String) = modifyProfile { it.copy(notes = notes) }

    fun saveClaudeApiKey(apiKey: String) {
        if (apiKey.isBlank()) return
        saveEvenIfScreenIsLeft { secretStore.write(Secret.CLAUDE_API_KEY, apiKey.trim()) }
    }

    fun removeClaudeApiKey() = saveEvenIfScreenIsLeft { secretStore.delete(Secret.CLAUDE_API_KEY) }

    fun setClaudeModel(modelId: String) = saveEvenIfScreenIsLeft { appSettingsRepository.setClaudeModel(modelId) }

    private fun modifyProfile(change: (ProfilePrefs) -> ProfilePrefs) = saveEvenIfScreenIsLeft { profileRepository.modify(change) }

    private fun saveEvenIfScreenIsLeft(save: suspend () -> Unit) {
        viewModelScope.launch { withContext(NonCancellable) { save() } }
    }

    private fun <T> Set<T>.toggled(item: T): Set<T> = if (item in this) this - item else this + item
}
