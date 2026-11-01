package com.github.sebastianp265.aitrainer.feature.plan.impl

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R

@Composable
internal fun PlaceholderScreen(@StringRes title: Int, @StringRes description: Int) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(stringResource(title), style = MaterialTheme.typography.headlineSmall)
        Text(stringResource(description), style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center)
    }
}

@PreviewLightDark
@Composable
private fun PlaceholderScreenPreview() {
    MaterialTheme {
        Surface {
            PlaceholderScreen(R.string.nav_plan, R.string.plan_placeholder)
        }
    }
}
