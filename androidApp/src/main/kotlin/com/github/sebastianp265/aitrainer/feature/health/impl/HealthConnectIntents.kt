package com.github.sebastianp265.aitrainer.feature.health.impl

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.health.connect.client.HealthConnectClient

private const val HEALTH_CONNECT_PLAY_STORE_URI =
    "market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"

internal fun Context.openHealthConnectInPlayStore() {
    try {
        startActivity(Intent(Intent.ACTION_VIEW, HEALTH_CONNECT_PLAY_STORE_URI.toUri()))
    } catch (_: ActivityNotFoundException) {
    }
}

internal fun Context.openHealthConnectSettings() {
    try {
        startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
    } catch (_: ActivityNotFoundException) {
    }
}
