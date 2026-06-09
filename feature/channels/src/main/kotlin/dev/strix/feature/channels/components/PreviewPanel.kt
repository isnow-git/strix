package dev.strix.feature.channels.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.strix.core.designsystem.glass.glass
import dev.strix.core.designsystem.theme.StrixPalette
import dev.strix.core.model.Channel
import dev.strix.core.model.epg.NowNext
import kotlinx.coroutines.delay

/**
 * The side preview: logo slot (the floating player docks over it), now/next title,
 * a slowly auto-scrolling synopsis, and the upcoming programme. Reports its logo-slot
 * bounds via [onSlotBounds] so the fullscreen scene knows where to morph from.
 */
@Composable
fun PreviewPanel(
    channel: Channel?,
    epg: NowNext?,
    onSlotBounds: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    channel ?: return
    val current = epg?.current
    val description = current?.description
    val descScroll = rememberScrollState()

    // After focus settles, slowly scroll the synopsis so it can be read in full.
    LaunchedEffect(description) {
        descScroll.scrollTo(0)
        if (!description.isNullOrBlank()) {
            delay(DESC_SCROLL_DELAY_MS)
            val max = descScroll.maxValue
            if (max > 0) {
                descScroll.animateScrollTo(max, tween(max * DESC_SCROLL_MS_PER_PX, easing = LinearEasing))
            }
        }
    }

    Column(
        modifier = modifier.glass(RoundedCornerShape(24.dp)).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .onGloballyPositioned { onSlotBounds(it.boundsInRoot()) }
                    .background(StrixPalette.LogoPlaceholder)
                    .border(1.dp, StrixPalette.LogoBorder, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            val logo = channel.logoUrl
            if (logo != null) {
                AsyncImage(
                    model = logo,
                    contentDescription = channel.name,
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                )
            } else {
                Text(
                    text = channel.label.take(1).uppercase(),
                    color = StrixPalette.OnBackground,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (current != null) {
            Text(text = current.title, color = StrixPalette.OnBackground, fontSize = 16.sp, maxLines = 2)
        } else {
            Text(text = "Programme indisponible", color = StrixPalette.Muted, fontSize = 13.sp)
        }

        if (!description.isNullOrBlank()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(descScroll)) {
                Text(text = description, color = StrixPalette.Muted, fontSize = 13.sp, lineHeight = 19.sp)
            }
        } else {
            Box(modifier = Modifier.weight(1f))
        }

        epg?.next?.let { next ->
            Text(text = "Puis · ${next.title}", color = StrixPalette.Muted, fontSize = 12.sp, maxLines = 1)
        }
    }
}

private const val DESC_SCROLL_DELAY_MS = 2_000L
private const val DESC_SCROLL_MS_PER_PX = 40
