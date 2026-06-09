package dev.strix.feature.channels.playback

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.strix.core.designsystem.theme.StrixPalette
import dev.strix.core.model.Channel
import dev.strix.core.model.epg.NowNext
import dev.strix.feature.channels.R

/**
 * Fullscreen player overlay: a bottom info bar with play/pause state, channel logo,
 * number + name, and now/next EPG. Purely presentational — visibility and auto-hide
 * are owned by [FullscreenPlayerScene]. Sits over the video on a soft bottom scrim.
 */
@Composable
fun PlayerOverlay(
    channel: Channel,
    epg: NowNext?,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier =
            modifier
                .fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, SCRIM)))
                .padding(start = 48.dp, end = 48.dp, top = 96.dp, bottom = 44.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        PlayPauseGlyph(isPlaying = isPlaying)

        Box(
            modifier =
                Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(StrixPalette.LogoPlaceholder),
            contentAlignment = Alignment.Center,
        ) {
            val logo = channel.logoUrl
            if (logo != null) {
                AsyncImage(
                    model = logo,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                )
            } else {
                Text(channel.label.take(1).uppercase(), color = StrixPalette.OnBackground, fontSize = 26.sp)
            }
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "${channel.channelNumber}   ${channel.label}",
                color = StrixPalette.OnBackground,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
            )
            epg?.current?.let { Text(it.title, color = StrixPalette.Muted, fontSize = 18.sp, maxLines = 1) }
            epg?.next?.let {
                Text(
                    text = stringResource(R.string.channels_up_next, it.title),
                    color = StrixPalette.Muted,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

/** A small play (triangle) / pause (two bars) indicator drawn with Canvas. */
@Composable
private fun PlayPauseGlyph(isPlaying: Boolean) {
    Canvas(modifier = Modifier.size(34.dp)) {
        val color = Color.White
        if (isPlaying) {
            val barWidth = size.width * 0.26f
            drawRect(color, topLeft = Offset(size.width * 0.16f, 0f), size = Size(barWidth, size.height))
            drawRect(color, topLeft = Offset(size.width * 0.58f, 0f), size = Size(barWidth, size.height))
        } else {
            val path =
                Path().apply {
                    moveTo(size.width * 0.18f, 0f)
                    lineTo(size.width * 0.9f, size.height / 2f)
                    lineTo(size.width * 0.18f, size.height)
                    close()
                }
            drawPath(path, color)
        }
    }
}

private val SCRIM = Color(0xE6000000)

/** Full-screen container that places [PlayerOverlay] at the bottom (used by the scene). */
@Composable
fun PlayerOverlayContainer(
    channel: Channel,
    epg: NowNext?,
    isPlaying: Boolean,
) {
    Box(modifier = Modifier.fillMaxSize()) {
        PlayerOverlay(
            channel = channel,
            epg = epg,
            isPlaying = isPlaying,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}
