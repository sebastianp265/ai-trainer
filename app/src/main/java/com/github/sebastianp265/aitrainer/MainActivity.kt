package com.github.sebastianp265.aitrainer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.sebastianp265.aitrainer.ui.navigation.AppNavigation
import com.github.sebastianp265.aitrainer.ui.theme.AITrainerTheme

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
