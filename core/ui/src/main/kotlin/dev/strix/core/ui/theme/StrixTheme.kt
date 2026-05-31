package dev.strix.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Typography
import androidx.tv.material3.darkColorScheme

private val StrixColorScheme =
    darkColorScheme(
        primary = StrixPalette.Primary,
        onPrimary = StrixPalette.OnPrimary,
        background = StrixPalette.Background,
        onBackground = StrixPalette.OnBackground,
        surface = StrixPalette.Surface,
        onSurface = StrixPalette.OnSurface,
        surfaceVariant = StrixPalette.SurfaceVariant,
        onSurfaceVariant = StrixPalette.OnSurfaceVariant,
        border = StrixPalette.Border,
    )

private val StrixTypography = Typography()

/**
 * Root theme for every Strix screen. Wraps the Compose-for-TV [MaterialTheme]
 * with the dark Strix color scheme so focus and contrast read well on a TV from
 * across the room.
 */
@Composable
fun StrixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StrixColorScheme,
        typography = StrixTypography,
        content = content,
    )
}
