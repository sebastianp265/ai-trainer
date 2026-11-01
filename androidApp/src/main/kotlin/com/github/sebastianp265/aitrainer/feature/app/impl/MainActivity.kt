package com.github.sebastianp265.aitrainer.feature.app.impl

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.github.sebastianp265.aitrainer.feature.app.impl.AppNavigation

internal class MainActivity : ComponentActivity() {

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
