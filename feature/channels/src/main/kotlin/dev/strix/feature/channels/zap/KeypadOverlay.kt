@file:Suppress("MatchingDeclarationName") // overlay belongs with the zap package

package dev.strix.feature.channels.zap

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import dev.strix.core.designsystem.theme.StrixPalette

/**
 * Bottom-right OSD showing the digits being typed and, once they resolve, the channel
 * they point at ("→ TF1") so the user can confidently take their time on long numbers.
 *
 * Purely presentational: it reads [input] and [tuning] and nothing else, so typing a
 * digit recomposes only this overlay — never the list, header or rail.
 */
@Composable
fun BoxScope.KeypadOverlay(
    input: String,
    tuning: String?,
    modifier: Modifier = Modifier,
) {
    if (input.isEmpty()) return
    Column(
        modifier = modifier.align(Alignment.BottomEnd).padding(36.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .shadow(16.dp, RoundedCornerShape(16.dp), clip = false)
                    .background(StrixPalette.NumberOsd, RoundedCornerShape(16.dp))
                    .padding(horizontal = 26.dp, vertical = 14.dp),
        ) {
            Text(text = input, color = StrixPalette.OnBackground, fontSize = 40.sp, fontWeight = FontWeight.Bold)
        }
        if (tuning != null) {
            Text(text = "→ $tuning", color = StrixPalette.Muted, fontSize = 14.sp, maxLines = 1)
        }
    }
}
