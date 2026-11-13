package com.github.sebastianp265.aitrainer.feature.workout.impl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import kotlin.time.Clock
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

@Composable
internal fun rememberElapsedSeconds(startedAt: Instant): Long {
    var elapsedSeconds by remember { mutableLongStateOf(0) }
    LaunchedEffect(startedAt) {
        while (true) {
            elapsedSeconds = (Clock.System.now() - startedAt).inWholeSeconds.coerceAtLeast(0)
            delay(1.seconds)
        }
    }
    return elapsedSeconds
}

internal fun formatElapsed(totalSeconds: Long): String {
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%02d:%02d".format(minutes, seconds)
}
