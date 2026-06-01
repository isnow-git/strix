package dev.strix.core.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * "Liquid glass"-inspired surface, approximated for API 30 (no runtime blur): a
 * translucent top-lit gradient fill plus a hairline highlight border. Cheap (no
 * blur, single layer) so it stays smooth on low-RAM TVs; use sparingly to avoid
 * overdraw.
 */
fun Modifier.glass(
    shape: Shape = RoundedCornerShape(20.dp),
    tint: Color = Color.White,
): Modifier =
    this
        .clip(shape)
        .background(
            Brush.verticalGradient(
                listOf(
                    tint.copy(alpha = 0.10f),
                    tint.copy(alpha = 0.04f),
                ),
            ),
        ).border(1.dp, tint.copy(alpha = 0.16f), shape)
