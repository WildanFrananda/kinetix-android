package com.kinetix.shop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun KinetixTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = KinetixPalette.Accent,
        onPrimary = KinetixPalette.Canvas,
        primaryContainer = KinetixPalette.AccentLow,
        onPrimaryContainer = KinetixPalette.AccentHigh,
        inversePrimary = KinetixPalette.Accent,
        secondary = KinetixPalette.InfoBlue,
        onSecondary = KinetixPalette.Canvas,
        secondaryContainer = KinetixPalette.SurfaceRaised,
        onSecondaryContainer = KinetixPalette.TextPrimary,
        tertiary = KinetixPalette.SuccessMint,
        onTertiary = KinetixPalette.Canvas,
        tertiaryContainer = KinetixPalette.SurfaceRaised,
        onTertiaryContainer = KinetixPalette.SuccessMint,
        background = KinetixPalette.Canvas,
        onBackground = KinetixPalette.TextPrimary,
        surface = KinetixPalette.Surface,
        onSurface = KinetixPalette.TextPrimary,
        surfaceVariant = KinetixPalette.SurfaceRaised,
        onSurfaceVariant = KinetixPalette.TextSecondary,
        surfaceTint = Color.Transparent,
        inverseSurface = KinetixPalette.Tile,
        inverseOnSurface = KinetixPalette.Canvas,
        error = KinetixPalette.DangerRed,
        onError = KinetixPalette.Canvas,
        errorContainer = KinetixPalette.DangerLow,
        onErrorContainer = KinetixPalette.TextPrimary,
        outline = KinetixPalette.Rule,
        outlineVariant = KinetixPalette.RuleStrong,
        scrim = KinetixPalette.Canvas,
        surfaceBright = KinetixPalette.SurfaceRaised,
        surfaceDim = KinetixPalette.Canvas,
        surfaceContainerLowest = KinetixPalette.Canvas,
        surfaceContainerLow = KinetixPalette.Surface,
        surfaceContainer = KinetixPalette.Surface,
        surfaceContainerHigh = KinetixPalette.SurfaceRaised,
        surfaceContainerHighest = KinetixPalette.SurfaceRaised,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KinetixTypography.Value,
        shapes = KinetixShapes.Value,
        content = content,
    )
}
