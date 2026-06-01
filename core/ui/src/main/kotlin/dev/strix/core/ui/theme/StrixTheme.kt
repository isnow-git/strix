package dev.strix.core.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.text.TextStyle
import androidx.tv.material3.LocalTextStyle
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

private val Outfit = StrixFont.Outfit.fontFamily()

// Outfit across the whole type scale.
private val StrixTypography =
    Typography().run {
        copy(
            displayLarge = displayLarge.copy(fontFamily = Outfit),
            displayMedium = displayMedium.copy(fontFamily = Outfit),
            displaySmall = displaySmall.copy(fontFamily = Outfit),
            headlineLarge = headlineLarge.copy(fontFamily = Outfit),
            headlineMedium = headlineMedium.copy(fontFamily = Outfit),
            headlineSmall = headlineSmall.copy(fontFamily = Outfit),
            titleLarge = titleLarge.copy(fontFamily = Outfit),
            titleMedium = titleMedium.copy(fontFamily = Outfit),
            titleSmall = titleSmall.copy(fontFamily = Outfit),
            bodyLarge = bodyLarge.copy(fontFamily = Outfit),
            bodyMedium = bodyMedium.copy(fontFamily = Outfit),
            bodySmall = bodySmall.copy(fontFamily = Outfit),
            labelLarge = labelLarge.copy(fontFamily = Outfit),
            labelMedium = labelMedium.copy(fontFamily = Outfit),
            labelSmall = labelSmall.copy(fontFamily = Outfit),
        )
    }

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
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = Outfit),
            content = content,
        )
    }
}
