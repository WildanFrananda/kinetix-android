package com.kinetix.shop.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

@Composable
fun KinetixTheme(
    content: @Composable () -> Unit,
) {
    val colorScheme = darkColorScheme(
        primary = KinetixPalette.ElectricVolt,
        onPrimary = KinetixPalette.ObsidianSurface,
        secondary = KinetixPalette.CyanPulse,
        onSecondary = KinetixPalette.ObsidianSurface,
        background = KinetixPalette.ObsidianSurface,
        onBackground = KinetixPalette.TextPrimary,
        surface = KinetixPalette.ElevatedSlate,
        onSurface = KinetixPalette.TextPrimary,
        surfaceVariant = KinetixPalette.CardSlate,
        onSurfaceVariant = KinetixPalette.TextSecondary,
        error = KinetixPalette.DangerCoral,
        outline = KinetixPalette.BorderHairline,
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KinetixTypography.Value,
        content = content,
    )
}
