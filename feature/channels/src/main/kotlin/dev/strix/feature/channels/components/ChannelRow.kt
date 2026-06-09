package dev.strix.feature.channels.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import dev.strix.core.designsystem.glass.glass
import dev.strix.core.designsystem.theme.StrixPalette
import dev.strix.core.model.Channel

/**
 * One channel row: number, logo, name. Fixed height and a single content type (shared
 * with [ChannelRowSkeleton]) so the LazyColumn reuses slots and scrolling stays smooth.
 * The focus flag is local, so moving focus only recomposes the row that gained/lost it.
 */
@Composable
fun ChannelRow(
    channel: Channel,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = transparentSurfaceColors(),
        modifier =
            modifier
                .fillMaxWidth()
                .height(ChannelListDefaults.ROW_HEIGHT.dp)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                },
    ) {
        // Only the focused row lifts onto a glass panel; the rest stay flat (cheap).
        val surface = if (focused) Modifier.glass(RoundedCornerShape(14.dp)) else Modifier
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = surface.fillMaxSize().padding(horizontal = 14.dp),
        ) {
            Text(
                text = channel.channelNumber.toString(),
                color = StrixPalette.Muted,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.width(44.dp),
            )
            ChannelLogo(channel)
            Text(
                text = channel.label,
                color = StrixPalette.OnBackground,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun ChannelLogo(channel: Channel) {
    Box(
        modifier =
            Modifier
                .size(ChannelListDefaults.LOGO_SIZE.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StrixPalette.LogoScrim)
                .border(1.dp, StrixPalette.LogoBorder, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        val logo = channel.logoUrl
        if (logo != null) {
            // Coil 3 decodes to the box constraints, so cached bitmaps stay small.
            AsyncImage(
                model = logo,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(6.dp),
            )
        } else {
            Text(
                text = channel.label.take(1).uppercase(),
                color = StrixPalette.OnBackground,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Placeholder row shown while a page loads; same shape as [ChannelRow] for slot reuse. */
@Composable
fun ChannelRowSkeleton() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(ChannelListDefaults.ROW_HEIGHT.dp)
                .padding(horizontal = 14.dp),
    ) {
        Spacer(Modifier.width(44.dp))
        Box(
            Modifier
                .size(ChannelListDefaults.LOGO_SIZE.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(StrixPalette.LogoScrim),
        )
        Box(
            Modifier
                .width(160.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(StrixPalette.LogoScrim),
        )
    }
}
