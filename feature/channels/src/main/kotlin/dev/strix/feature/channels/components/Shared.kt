package dev.strix.feature.channels.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceColors
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import dev.strix.core.designsystem.focus.focusRing
import dev.strix.core.designsystem.glass.glass
import dev.strix.core.designsystem.theme.StrixPalette

/** A focusable glass pill button. */
@Composable
fun GlassButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = transparentSurfaceColors(),
        modifier = modifier.focusRing(cornerRadius = 14.dp),
    ) {
        Box(modifier = Modifier.glass(RoundedCornerShape(14.dp)).padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text(text = label, color = StrixPalette.OnBackground, fontSize = 14.sp)
        }
    }
}

/** A centred status line (loading / empty). */
@Composable
fun CenterMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = StrixPalette.Muted, fontSize = 16.sp)
    }
}

/** A lightweight indeterminate spinner (one arc, no per-item allocations). */
@Composable
fun LoadingSpinner() {
    val transition = rememberInfiniteTransition(label = "spinner")
    val angle by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(SPINNER_PERIOD_MS, easing = LinearEasing)),
        label = "angle",
    )
    Canvas(modifier = Modifier.size(48.dp)) {
        rotate(angle) {
            drawArc(
                color = StrixPalette.OnBackground,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

/** Transparent surface colors so our own glass/background shows through the TV Surface. */
@Composable
fun transparentSurfaceColors(): ClickableSurfaceColors =
    ClickableSurfaceDefaults.colors(
        containerColor = androidx.compose.ui.graphics.Color.Transparent,
        focusedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
        pressedContainerColor = androidx.compose.ui.graphics.Color.Transparent,
    )

private const val SPINNER_PERIOD_MS = 900
