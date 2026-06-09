package dev.strix.feature.channels.playback

import android.view.LayoutInflater
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Text
import dev.strix.core.designsystem.theme.StrixPalette
import dev.strix.core.model.Channel
import dev.strix.core.model.epg.NowNext
import dev.strix.core.player.playback.StreamPlaybackController.Picture
import dev.strix.feature.channels.R
import dev.strix.feature.channels.components.LoadingSpinner
import kotlinx.coroutines.delay

/**
 * The single floating player, its preview->fullscreen morph, and the fullscreen overlay.
 *
 * The PlayerView always lays out at full-screen size and a [graphicsLayer] scales +
 * translates it into the preview slot when collapsed, animating to identity when
 * expanded — nothing is re-measured per frame, so the morph is smooth on a 2 GB TV and
 * only this subtree recomposes during it. One surface throughout = no black flash.
 *
 * When expanded, a transparent focusable layer captures the remote: Back exits (or hides
 * the overlay first), OK toggles play/pause, and any D-pad press reveals the
 * [PlayerOverlay] (channel + now/next), which auto-hides after a fixed idle window.
 */
@UnstableApi
@Composable
fun FullscreenPlayerScene(
    player: ExoPlayer,
    expanded: Boolean,
    hasPreview: Boolean,
    picture: Picture,
    channel: Channel?,
    epg: NowNext?,
    isPlaying: Boolean,
    slotRect: Rect,
    rootWidth: Int,
    rootHeight: Int,
    focus: FocusRequester,
    onBack: () -> Unit,
    onToggle: () -> Unit,
) {
    val progress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = MORPH_MS, easing = LinearEasing),
        label = "morph",
    )

    var overlayVisible by remember { mutableStateOf(false) }
    var interaction by remember { mutableIntStateOf(0) }

    // Show the overlay when opening fullscreen; clear it when leaving.
    LaunchedEffect(expanded) {
        overlayVisible = expanded
        if (expanded) interaction++
    }
    // Deterministic auto-hide: every interaction restarts a single fixed timer.
    LaunchedEffect(interaction, overlayVisible) {
        if (overlayVisible) {
            delay(OVERLAY_TIMEOUT_MS)
            overlayVisible = false
        }
    }

    // Keep the screen awake while fullscreen so the TV never sleeps mid-stream.
    // Tied to [expanded]: the flag clears the moment we leave fullscreen (or dispose).
    val view = LocalView.current
    DisposableEffect(expanded) {
        view.keepScreenOn = expanded
        onDispose { view.keepScreenOn = false }
    }

    // Black scrim behind the player as it grows to fullscreen.
    if (progress > 0.001f) {
        Box(modifier = Modifier.fillMaxSize().alpha(progress).background(Color.Black))
    }

    // Feedback while there is no picture in fullscreen: a spinner, or a clear message
    // once the channel is declared unavailable — never an endless spinner.
    if (expanded && picture != Picture.Playing) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (picture == Picture.Unavailable) {
                Text(
                    text = stringResource(R.string.channels_channel_unavailable),
                    color = StrixPalette.Muted,
                    fontSize = 16.sp,
                )
            } else {
                LoadingSpinner()
            }
        }
    }

    // The morphing player surface (input is handled by the overlay layer below).
    val canRenderPlayer = (hasPreview || expanded) && rootWidth > 0 && rootHeight > 0
    if (canRenderPlayer) {
        val full = Rect(0f, 0f, rootWidth.toFloat(), rootHeight.toFloat())
        val origin = if (slotRect != Rect.Zero) slotRect else full
        FloatingPlayer(
            player = player,
            origin = origin,
            full = full,
            progress = progress,
            visible = picture == Picture.Playing,
        )
    }

    // Fullscreen input + overlay.
    if (expanded) {
        LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }
        val current = channel
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .focusRequester(focus)
                    .focusable()
                    .onKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                        when (event.key) {
                            Key.Back -> {
                                // First Back hides the overlay; a second one exits fullscreen.
                                if (overlayVisible) overlayVisible = false else onBack()
                                true
                            }
                            Key.DirectionCenter, Key.Enter -> {
                                onToggle()
                                overlayVisible = true
                                interaction++
                                true
                            }
                            Key.DirectionUp, Key.DirectionDown, Key.DirectionLeft, Key.DirectionRight -> {
                                overlayVisible = true
                                interaction++
                                true
                            }
                            else -> false
                        }
                    },
        ) {
            AnimatedVisibility(
                visible = overlayVisible && current != null,
                enter = fadeIn(tween(OVERLAY_FADE_MS)),
                exit = fadeOut(tween(OVERLAY_FADE_MS)),
            ) {
                if (current != null) {
                    PlayerOverlayContainer(channel = current, epg = epg, isPlaying = isPlaying)
                }
            }
        }
    }
}

@UnstableApi
@Composable
private fun FloatingPlayer(
    player: ExoPlayer,
    origin: Rect,
    full: Rect,
    progress: Float,
    visible: Boolean,
) {
    val density = LocalDensity.current
    val cornerPx = with(density) { lerp(PREVIEW_RADIUS, 0f, progress).dp.toPx() }

    // Scale/translate the full-size view down into the slot at progress 0 → identity at 1.
    val scaleX = lerp(origin.width / full.width, 1f, progress)
    val scaleY = lerp(origin.height / full.height, 1f, progress)
    val tx = lerp(origin.center.x - full.center.x, 0f, progress)
    val ty = lerp(origin.center.y - full.center.y, 0f, progress)
    val clipShape = if (cornerPx > 0f) RoundedCornerShape(cornerPx) else RectangleShape

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .graphicsLayer {
                    this.scaleX = scaleX
                    this.scaleY = scaleY
                    translationX = tx
                    translationY = ty
                    // Reveal the video only once the first frame is ready; the slot's logo
                    // (drawn by PreviewPanel underneath) shows until then.
                    alpha = if (visible) 1f else 0f
                    clip = true
                    shape = clipShape
                },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                (LayoutInflater.from(context).inflate(R.layout.strix_player_view, null) as PlayerView).apply {
                    setPlayer(player)
                }
            },
            update = { it.setPlayer(player) },
        )
    }
}

private const val MORPH_MS = 300
private const val PREVIEW_RADIUS = 16f
private const val OVERLAY_FADE_MS = 200
private const val OVERLAY_TIMEOUT_MS = 4_000L
