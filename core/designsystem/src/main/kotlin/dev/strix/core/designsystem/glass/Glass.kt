package dev.strix.core.designsystem.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.strix.core.designsystem.theme.StrixPalette

/**
 * "Liquid glass"-inspired surface, approximated for Android TV 11 (API 30, no runtime
 * blur): a translucent top-lit gradient fill plus a hairline highlight border.
 *
 * Deliberately cheap — no `RenderEffect` blur (that needs API 31+ and is far too
 * heavy for a 2 GB TV), single draw layer — so it stays smooth even when many rows
 * use it. Use sparingly to keep overdraw down.
 */
fun Modifier.glass(shape: Shape = RoundedCornerShape(20.dp)): Modifier =
    this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(StrixPalette.GlassHighlight, StrixPalette.GlassLowlight),
            ),
        ).border(1.dp, StrixPalette.GlassBorder, shape)
