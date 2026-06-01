package dev.strix.core.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import dev.strix.core.ui.R

/** Display fonts for the design system (Outfit is the locked default). */
enum class StrixFont(
    val label: String,
) {
    Outfit("Outfit"),
}

@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
private fun variable(
    resId: Int,
    weight: FontWeight,
) = Font(
    resId = resId,
    weight = weight,
    variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)),
)

private fun family(resId: Int): FontFamily =
    FontFamily(
        variable(resId, FontWeight.Normal),
        variable(resId, FontWeight.Medium),
        variable(resId, FontWeight.SemiBold),
        variable(resId, FontWeight.Bold),
    )

/** The [FontFamily] for a [StrixFont]. */
fun StrixFont.fontFamily(): FontFamily =
    when (this) {
        StrixFont.Outfit -> family(R.font.outfit)
    }
