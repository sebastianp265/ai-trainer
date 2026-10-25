package com.github.sebastianp265.aitrainer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

@Composable
fun Stepper(
    value: String,
    valueTag: String,
    unit: String,
    decreaseDescription: String,
    increaseDescription: String,
    onMinus: () -> Unit,
    onPlus: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        FilledTonalIconButton(onClick = onMinus, modifier = Modifier.semantics { contentDescription = decreaseDescription }) {
            Text("−")
        }
        Column(Modifier.widthIn(min = 40.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, Modifier.testTag(valueTag), style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(unit, style = MaterialTheme.typography.labelSmall)
        }
        FilledTonalIconButton(onClick = onPlus, modifier = Modifier.semantics { contentDescription = increaseDescription }) {
            Text("+")
        }
    }
}
