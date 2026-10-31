package com.github.sebastianp265.aitrainer.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.sebastianp265.aitrainer.app.navigation.AppNavigation
import com.github.sebastianp265.aitrainer.core.ui.theme.AITrainerTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AITrainerTheme {
                AppNavigation()
            }
        }
    }
}
