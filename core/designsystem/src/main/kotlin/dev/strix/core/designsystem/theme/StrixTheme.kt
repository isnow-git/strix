package dev.strix.core.designsystem.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
        onSurfaceVariant = StrixPalette.Muted,
        border = StrixPalette.Border,
    )

// Outfit applied across the entire TV Material type scale.
private val StrixTypography =
    Typography().run {
        copy(
            displayLarge = displayLarge.copy(fontFamily = OutfitFamily),
            displayMedium = displayMedium.copy(fontFamily = OutfitFamily),
            displaySmall = displaySmall.copy(fontFamily = OutfitFamily),
            headlineLarge = headlineLarge.copy(fontFamily = OutfitFamily),
            headlineMedium = headlineMedium.copy(fontFamily = OutfitFamily),
            headlineSmall = headlineSmall.copy(fontFamily = OutfitFamily),
            titleLarge = titleLarge.copy(fontFamily = OutfitFamily),
            titleMedium = titleMedium.copy(fontFamily = OutfitFamily),
            titleSmall = titleSmall.copy(fontFamily = OutfitFamily),
            bodyLarge = bodyLarge.copy(fontFamily = OutfitFamily),
            bodyMedium = bodyMedium.copy(fontFamily = OutfitFamily),
            bodySmall = bodySmall.copy(fontFamily = OutfitFamily),
            labelLarge = labelLarge.copy(fontFamily = OutfitFamily),
            labelMedium = labelMedium.copy(fontFamily = OutfitFamily),
            labelSmall = labelSmall.copy(fontFamily = OutfitFamily),
        )
    }

/**
 * Root theme for every Strix screen. Wraps Compose-for-TV [MaterialTheme] with the
 * dark Strix scheme and Outfit so focus and contrast read well from across the room.
 */
@Composable
fun StrixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = StrixColorScheme,
        typography = StrixTypography,
    ) {
        CompositionLocalProvider(
            LocalTextStyle provides LocalTextStyle.current.copy(fontFamily = OutfitFamily),
            content = content,
        )
    }
}
