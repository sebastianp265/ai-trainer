package com.github.sebastianp265.aitrainer.shared.design

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.github.sebastianp265.aitrainer.shared.design.impl.ExtendedColors

internal val LightExtendedColors = ExtendedColors(
    warning = Color(0xFF8A5300),
    warningContainer = Color(0xFFFFDDB8),
    onWarningContainer = Color(0xFF2A1700),
    success = Color(0xFF1E5A33),
    successContainer = Color(0xFFDDEEDD),
    recordContainer = Color(0xFFFFEEDB),
)

internal val DarkExtendedColors = ExtendedColors(
    warning = Color(0xFFFFB86B),
    warningContainer = Color(0xFF693F00),
    onWarningContainer = Color(0xFFFFDDB8),
    success = Color(0xFF9FD5AE),
    successContainer = Color(0xFF23462D),
    recordContainer = Color(0xFF3D2E1A),
)

internal val LocalExtendedColors = staticCompositionLocalOf { LightExtendedColors }

internal val extendedColors: ExtendedColors
    @Composable
    @ReadOnlyComposable
    get() = LocalExtendedColors.current
