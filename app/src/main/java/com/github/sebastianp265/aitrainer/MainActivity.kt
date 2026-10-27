package com.github.sebastianp265.aitrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.sebastianp265.aitrainer.sync.HealthConnectSyncScheduler
import com.github.sebastianp265.aitrainer.ui.navigation.AppNavigation
import com.github.sebastianp265.aitrainer.ui.theme.AITrainerTheme
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val healthConnectSyncScheduler: HealthConnectSyncScheduler by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AITrainerTheme {
                AppNavigation()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        healthConnectSyncScheduler.syncNow()
    }
}
