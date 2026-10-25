package com.github.sebastianp265.aitrainer.ui

import kotlinx.coroutines.flow.SharingStarted

private const val KEEP_QUERIES_ALIVE_AFTER_UI_STOPS_MS = 5_000L

val WhileUiSubscribed: SharingStarted = SharingStarted.WhileSubscribed(KEEP_QUERIES_ALIVE_AFTER_UI_STOPS_MS)
