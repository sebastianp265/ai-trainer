package com.github.sebastianp265.aitrainer.feature.ai.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.ai.AiService
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons
import com.github.sebastianp265.aitrainer.shared.design.GroupCard
import com.github.sebastianp265.aitrainer.shared.design.LinkRow
import com.github.sebastianp265.aitrainer.shared.design.Subtitle
import com.github.sebastianp265.aitrainer.shared.design.extendedColors
import com.github.sebastianp265.aitrainer.shared.state.WhileUiSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

internal data class AiSettingsUiState(
    val isKeyStored: Boolean = false,
    val model: String = AiService.DEFAULT_MODEL,
)

internal class AiSettingsViewModel(aiService: AiService) : ViewModel() {

    val uiState: StateFlow<AiSettingsUiState> = combine(
        aiService.observeIsApiKeyStored(),
        aiService.observeModel(),
    ) { isKeyStored, model -> AiSettingsUiState(isKeyStored, model) }
        .stateIn(viewModelScope, WhileUiSubscribed, AiSettingsUiState())
}

internal interface AiSettingsActions {
    fun openApiKey()
    fun openModel()
}

@Composable
internal fun AiSettingsContent(state: AiSettingsUiState, actions: AiSettingsActions) {
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            GroupCard {
                LinkRow(
                    title = stringResource(R.string.settings_claude_api_key),
                    tag = AiSettingsTags.API_KEY_ROW,
                    action = stringResource(R.string.settings_change),
                    onClick = actions::openApiKey,
                ) {
                    if (state.isKeyStored) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(DesignIcons.Check, contentDescription = null, Modifier.size(16.dp), tint = extendedColors.success)
                            Text(stringResource(R.string.settings_api_key_saved), style = MaterialTheme.typography.bodyMedium, color = extendedColors.success)
                        }
                    } else {
                        Subtitle(stringResource(R.string.settings_api_key_missing))
                    }
                }
                LinkRow(
                    title = stringResource(R.string.settings_claude_model),
                    tag = AiSettingsTags.MODEL_ROW,
                    action = stringResource(R.string.settings_change),
                    divider = false,
                    onClick = actions::openModel,
                ) { Subtitle(modelName(state.model)) }
            }
        }
        item {
            Text(
                stringResource(R.string.ai_settings_text),
                Modifier.padding(horizontal = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

internal object AiSettingsTags {
    const val API_KEY_ROW = "ai_api_key_row"
    const val MODEL_ROW = "ai_model_row"
}

@PreviewLightDark
@Composable
private fun AiSettingsPreview() {
    MaterialTheme {
        Surface {
            AiSettingsContent(
                state = AiSettingsUiState(isKeyStored = true, model = "claude-sonnet-5-5"),
                actions = object : AiSettingsActions {
                    override fun openApiKey() = Unit
                    override fun openModel() = Unit
                },
            )
        }
    }
}
