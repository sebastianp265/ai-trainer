package com.github.sebastianp265.aitrainer.feature.health.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.integration.health.HealthConnectAccess

@Composable
internal fun HealthConnectCardContent(
    access: HealthConnectAccess,
    permissions: Set<String>,
    actions: HealthConnectCardActions,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.health_connect), style = MaterialTheme.typography.titleMedium)
            val description = when (access) {
                HealthConnectAccess.UNAVAILABLE -> R.string.health_connect_unavailable
                HealthConnectAccess.UPDATE_REQUIRED -> R.string.health_connect_update_required
                HealthConnectAccess.NOT_CONNECTED -> R.string.health_connect_not_connected
                HealthConnectAccess.CONNECTED -> null
            }
            description?.let { Text(stringResource(it)) }
            when (access) {
                HealthConnectAccess.NOT_CONNECTED ->
                    Button(onClick = { actions.connect(permissions) }) { Text(stringResource(R.string.health_connect_connect)) }
                HealthConnectAccess.UPDATE_REQUIRED ->
                    Button(onClick = actions::update) { Text(stringResource(R.string.health_connect_update)) }
                HealthConnectAccess.UNAVAILABLE, HealthConnectAccess.CONNECTED -> Unit
            }
        }
    }
}

internal interface HealthConnectCardActions {
    fun connect(permissions: Set<String>)
    fun update()
}

@PreviewLightDark
@Composable
private fun HealthConnectCardNotConnectedPreview() {
    MaterialTheme {
        Surface {
            HealthConnectCardContent(
                access = HealthConnectAccess.NOT_CONNECTED,
                permissions = emptySet(),
                actions = object : HealthConnectCardActions {
                    override fun connect(permissions: Set<String>) = Unit
                    override fun update() = Unit
                },
            )
        }
    }
}
