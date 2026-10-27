package com.github.sebastianp265.aitrainer.ui.healthconnect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.github.sebastianp265.aitrainer.R
import com.github.sebastianp265.aitrainer.ui.components.BackTopBar
import com.github.sebastianp265.aitrainer.ui.theme.AITrainerTheme

class HealthDataUsageActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AITrainerTheme {
                HealthDataUsageScreen(onBack = ::finish)
            }
        }
    }
}

@Composable
fun HealthDataUsageScreen(onBack: () -> Unit) {
    Scaffold(topBar = { BackTopBar(R.string.health_data_usage_title, onBack) }) { padding ->
        Column(
            modifier = Modifier.padding(padding).padding(16.dp).fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            listOf(
                R.string.health_data_usage_what,
                R.string.health_data_usage_why,
                R.string.health_data_usage_storage,
                R.string.health_data_usage_ai,
                R.string.health_data_usage_revoke,
            ).forEach { Text(stringResource(it), style = MaterialTheme.typography.bodyLarge) }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HealthDataUsagePreview() {
    AITrainerTheme { HealthDataUsageScreen(onBack = {}) }
}
