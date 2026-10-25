package com.github.sebastianp265.aitrainer.ui.debug

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.data.demo.DemoDataSeeder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DebugUiState(
    val isReplacingData: Boolean = false,
    val showDemoDataCreated: Boolean = false,
)

class DebugViewModel(
    private val demoDataSeeder: DemoDataSeeder,
) : ViewModel() {

    private val state = MutableStateFlow(DebugUiState())
    val uiState: StateFlow<DebugUiState> = state.asStateFlow()

    fun replaceWithDemoData() {
        state.update { it.copy(isReplacingData = true) }
        viewModelScope.launch {
            demoDataSeeder.replaceWithDemoData()
            state.update { it.copy(isReplacingData = false, showDemoDataCreated = true) }
        }
    }

    fun demoDataCreatedShown() {
        state.update { it.copy(showDemoDataCreated = false) }
    }
}
