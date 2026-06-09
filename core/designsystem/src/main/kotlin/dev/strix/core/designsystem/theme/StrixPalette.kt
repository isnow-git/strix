package dev.strix.core.designsystem.theme

import androidx.compose.ui.graphics.Color

/**
 * The single source of truth for Strix colors. Dark by default — TVs are viewed in
 * dim rooms and the palette is OLED-friendly. Features must read colors from here
 * (never redefine literals locally) so the theme stays consistent and tweakable.
 */
object StrixPalette {
    // Surfaces
    val Background = Color(0xFF0B0B0F)
    val BackgroundElevated = Color(0xFF15151F)
    val Surface = Color(0xFF15151C)
    val SurfaceVariant = Color(0xFF22222C)

    // Brand
    val Primary = Color(0xFF6C8CFF)
    val OnPrimary = Color(0xFF0B0B0F)

    // Text
    val OnBackground = Color(0xFFECECF1)
    val OnSurface = Color(0xFFECECF1)
    val Muted = Color(0xFFB6B6C2)

    // Lines / accents
    val Border = Color(0xFF3A3A47)
    val FocusRing = Color(0xFFFFFFFF)
    val Error = Color(0xFFFF6B6B)

    // Translucent fills used by [dev.strix.core.designsystem.glass]
    val GlassHighlight = Color(0x1AFFFFFF) // ~10% white
    val GlassLowlight = Color(0x0AFFFFFF) // ~4% white
    val GlassBorder = Color(0x29FFFFFF) // ~16% white

    // Logo tiles
    val LogoScrim = Color(0x2EFFFFFF) // list logo background
    val LogoBorder = Color(0x40FFFFFF)
    val LogoPlaceholder = Color(0xFF2C2C34) // preview logo background

    // Strong scrim for the on-screen channel-number OSD.
    val NumberOsd = Color(0xE60B0B0F)
}
