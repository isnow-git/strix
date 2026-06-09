package dev.strix.core.designsystem.theme

import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import dev.strix.core.designsystem.R

/**
 * Outfit — the locked Strix display family. A single variable font file backs every
 * weight (one asset, smaller APK) via [FontVariation].
 */
@OptIn(ExperimentalTextApi::class)
private fun outfit(weight: FontWeight): Font =
    Font(
        resId = R.font.outfit,
        weight = weight,
        variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
    )

/** The [FontFamily] used across the whole Strix type scale. */
val OutfitFamily: FontFamily =
    FontFamily(
        outfit(FontWeight.Normal),
        outfit(FontWeight.Medium),
        outfit(FontWeight.SemiBold),
        outfit(FontWeight.Bold),
    )
