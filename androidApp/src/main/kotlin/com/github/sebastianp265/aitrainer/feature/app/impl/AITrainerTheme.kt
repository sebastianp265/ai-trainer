package com.github.sebastianp265.aitrainer.feature.app.impl

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.github.sebastianp265.aitrainer.shared.design.DarkExtendedColors
import com.github.sebastianp265.aitrainer.shared.design.LightExtendedColors
import com.github.sebastianp265.aitrainer.shared.design.LocalExtendedColors
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = Color(0xFF3A6A4C),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFBCEFC9),
    onPrimaryContainer = Color(0xFF00210F),
    inversePrimary = Color(0xFFA0D3B0),
    secondary = Color(0xFF4F6353),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD2E8D4),
    onSecondaryContainer = Color(0xFF0D1F13),
    tertiary = Color(0xFF2E6576),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC9E6F0),
    onTertiaryContainer = Color(0xFF001F27),
    error = Color(0xFF9C3D2E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    background = Color(0xFFF7FAF5),
    onBackground = Color(0xFF191C1A),
    surface = Color(0xFFF7FAF5),
    onSurface = Color(0xFF191C1A),
    surfaceVariant = Color(0xFFDDE5DB),
    onSurfaceVariant = Color(0xFF414942),
    surfaceTint = Color(0xFF3A6A4C),
    inverseSurface = Color(0xFF2E312E),
    inverseOnSurface = Color(0xFFEFF2EC),
    outline = Color(0xFF717971),
    outlineVariant = Color(0xFFC1C9BF),
    scrim = Color(0xFF191C1A),
    surfaceBright = Color(0xFFF7FAF5),
    surfaceDim = Color(0xFFD7DBD5),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F5EF),
    surfaceContainer = Color(0xFFECEFEA),
    surfaceContainerHigh = Color(0xFFE9EEE7),
    surfaceContainerHighest = Color(0xFFECEFEA),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFA0D3B0),
    onPrimary = Color(0xFF033920),
    primaryContainer = Color(0xFF215136),
    onPrimaryContainer = Color(0xFFBCEFC9),
    inversePrimary = Color(0xFF3A6A4C),
    secondary = Color(0xFFB6CCB8),
    onSecondary = Color(0xFF223526),
    secondaryContainer = Color(0xFF384B3C),
    onSecondaryContainer = Color(0xFFD2E8D4),
    tertiary = Color(0xFFA2CEDD),
    onTertiary = Color(0xFF033542),
    tertiaryContainer = Color(0xFF1F4D5D),
    onTertiaryContainer = Color(0xFFC9E6F0),
    error = Color(0xFFFFB4A6),
    onError = Color(0xFF5F1509),
    errorContainer = Color(0xFF7A2C20),
    onErrorContainer = Color(0xFFF9DEDC),
    background = Color(0xFF101411),
    onBackground = Color(0xFFE0E4DE),
    surface = Color(0xFF101411),
    onSurface = Color(0xFFE0E4DE),
    surfaceVariant = Color(0xFF414942),
    onSurfaceVariant = Color(0xFFC1C9BF),
    surfaceTint = Color(0xFFA0D3B0),
    inverseSurface = Color(0xFFE0E4DE),
    inverseOnSurface = Color(0xFF2E312E),
    outline = Color(0xFF8B938A),
    outlineVariant = Color(0xFF414942),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF353A36),
    surfaceDim = Color(0xFF101411),
    surfaceContainerLowest = Color(0xFF0B0F0C),
    surfaceContainerLow = Color(0xFF181D19),
    surfaceContainer = Color(0xFF1C211D),
    surfaceContainerHigh = Color(0xFF262B27),
    surfaceContainerHighest = Color(0xFF1C211D),
)

private val DesignShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val DefaultTypography = Typography()

private val DesignTypography = DefaultTypography.copy(
    titleMedium = DefaultTypography.titleMedium.copy(fontSize = 18.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
)

@Composable
internal fun AITrainerTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    CompositionLocalProvider(LocalExtendedColors provides if (dark) DarkExtendedColors else LightExtendedColors) {
        MaterialTheme(
            colorScheme = if (dark) DarkColors else LightColors,
            shapes = DesignShapes,
            typography = DesignTypography,
            content = content,
        )
    }
}
