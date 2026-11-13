package com.github.sebastianp265.aitrainer.shared.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
internal fun LinkRow(
    title: String,
    tag: String,
    action: String?,
    onClick: () -> Unit,
    divider: Boolean = true,
    subtitle: @Composable () -> Unit,
) {
    GroupRow(Modifier.testTag(tag), onClick = onClick, divider = divider) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            subtitle()
        }
        if (action != null) {
            Text(action, Modifier.padding(end = 12.dp), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        } else {
            Icon(DesignIcons.ChevronRight, contentDescription = null, Modifier.padding(end = 12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
internal fun Subtitle(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
