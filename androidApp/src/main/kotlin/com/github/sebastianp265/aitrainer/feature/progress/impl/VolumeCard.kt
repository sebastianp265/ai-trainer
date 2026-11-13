package com.github.sebastianp265.aitrainer.feature.progress.impl

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.shared.design.CardLabel
import com.github.sebastianp265.aitrainer.shared.design.InfoNote
import com.github.sebastianp265.aitrainer.integration.volume.DEFAULT_MAX_WEEKLY_SETS
import com.github.sebastianp265.aitrainer.integration.volume.DEFAULT_MIN_WEEKLY_SETS
import com.github.sebastianp265.aitrainer.shared.localization.displayName
import com.github.sebastianp265.aitrainer.shared.localization.formatWeight
import com.github.sebastianp265.aitrainer.shared.design.DesignIcons

@Composable
internal fun VolumeCard(rows: List<VolumeRow>, showTargets: Boolean, onOpenTargets: () -> Unit) {
    var infoOpen by rememberSaveable { mutableStateOf(false) }
    Card(Modifier.fillMaxWidth().testTag(ProgressTags.VOLUME_CARD)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CardLabel(stringResource(R.string.volume_card_title), Modifier.weight(1f))
                TextButton(onClick = onOpenTargets, Modifier.testTag(ProgressTags.VOLUME_TARGETS_LINK)) { Text(stringResource(R.string.volume_targets_link)) }
                IconButton(onClick = { infoOpen = !infoOpen }) {
                    Icon(DesignIcons.Info, contentDescription = stringResource(R.string.volume_how_we_count), tint = MaterialTheme.colorScheme.primary)
                }
            }
            VolumeBars(rows, showTargets)
            if (showTargets) VolumeLegend()
            if (infoOpen) VolumeInfoBox()
            if (showTargets) Text(volumeSummary(rows), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun VolumeInfoBox() {
    InfoNote(stringResource(R.string.volume_info_title), stringResource(R.string.volume_info_counting)) {
        Text(
            pluralStringResource(R.plurals.volume_info_target, DEFAULT_MAX_WEEKLY_SETS, DEFAULT_MIN_WEEKLY_SETS, DEFAULT_MAX_WEEKLY_SETS),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.85f),
        )
    }
}

@Composable
internal fun volumeSummary(rows: List<VolumeRow>): String {
    val locale = LocalConfiguration.current.locales[0]
    val onTarget = rows.count { it.status() == TargetStatus.IN_TARGET }
    val first = pluralStringResource(R.plurals.volume_on_target, rows.size, onTarget, rows.size)
    val outside = rows.filter { it.status() != TargetStatus.IN_TARGET }.map { row ->
        pluralStringResource(
            if (row.status() == TargetStatus.BELOW) R.plurals.volume_below else R.plurals.volume_above,
            row.maxSets,
            row.group.displayName(locale),
            formatWeight(row.done),
            row.minSets,
            row.maxSets,
        )
    }
    return (listOf(first) + outside).joinToString(" ")
}
