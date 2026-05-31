package dev.strix.feature.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.upstream.DefaultBandwidthMeter
import androidx.media3.ui.PlayerView
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

/**
 * Fullscreen playback with a minimalist custom overlay (no stock controls): a
 * centre play/pause button, the live network bitrate top-right, and three
 * animated dots top-left while buffering. D-pad up/down zaps channels, OK toggles
 * play/pause; the overlay auto-hides while playing.
 *
 * The player is created once, paused on stop, and released on dispose so leaving
 * the screen never leaks the decoder.
 */
@OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    modifier: Modifier = Modifier,
    viewModel: PlayerViewModel = hiltViewModel(),
) {
    val channel by viewModel.channel.collectAsStateWithLifecycle()
    val current by viewModel.current.collectAsStateWithLifecycle()
    val variants by viewModel.variants.collectAsStateWithLifecycle()
    val player = remember { viewModel.createPlayer() }
    val context = LocalContext.current
    val bandwidthMeter = remember { DefaultBandwidthMeter.getSingletonInstance(context) }
    val lifecycleOwner = LocalLifecycleOwner.current

    var buffering by remember { mutableStateOf(true) }
    var isPlaying by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var controlsVisible by remember { mutableStateOf(true) }
    var poke by remember { mutableIntStateOf(0) }
    var bitrateBps by remember { mutableLongStateOf(0L) }

    val rootFocus = remember { FocusRequester() }
    val retryFocus = remember { FocusRequester() }

    fun play() {
        val url = current?.streamUrl ?: return
        error = null
        buffering = true
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = true
    }

    fun reveal() {
        controlsVisible = true
        poke++
    }

    DisposableEffect(player) {
        val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    buffering = state == Player.STATE_BUFFERING
                    if (state == Player.STATE_READY) error = null
                }

                override fun onIsPlayingChanged(playing: Boolean) {
                    isPlaying = playing
                }

                override fun onPlayerError(e: PlaybackException) {
                    buffering = false
                    // Auto-fall back to a lower quality before surfacing the error;
                    // the current-variant change re-triggers playback.
                    if (!viewModel.fallback()) error = playbackErrorMessage(e.errorCode)
                }
            }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }

    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) player.pause()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            player.release()
        }
    }

    // (Re)start playback whenever the selected variant changes (load, zap, or a
    // quality switch/fallback).
    LaunchedEffect(current) { play() }

    // Auto-hide the overlay a few seconds after the last interaction while playing.
    LaunchedEffect(poke, isPlaying, error) {
        if (error == null && isPlaying && controlsVisible) {
            delay(AUTO_HIDE_MS)
            controlsVisible = false
        }
    }

    // Keep D-pad focus on the right target. Guarded: the target may still be
    // animating in (AnimatedVisibility) and not yet attached.
    LaunchedEffect(error) {
        runCatching {
            if (error != null) retryFocus.requestFocus() else rootFocus.requestFocus()
        }
    }

    // Refresh the bitrate every second while the overlay is on screen. Prefer the
    // active stream's declared video bitrate (stable, meaningful per channel); a
    // paced live feed makes the raw network estimate hover near zero, so fall back
    // to it only when the format doesn't report a bitrate.
    LaunchedEffect(controlsVisible) {
        while (controlsVisible) {
            val streamBitrate = player.videoFormat?.bitrate?.takeIf { it > 0 }?.toLong()
            bitrateBps = streamBitrate ?: bandwidthMeter.bitrateEstimate
            delay(BITRATE_POLL_MS)
        }
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(Color.Black)
                .focusRequester(rootFocus)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> { viewModel.zap(1); reveal(); true }
                        Key.DirectionUp -> { viewModel.zap(-1); reveal(); true }
                        Key.DirectionLeft -> { viewModel.cycleQuality(-1); reveal(); true }
                        Key.DirectionRight -> { viewModel.cycleQuality(1); reveal(); true }
                        Key.DirectionCenter, Key.Enter -> {
                            if (player.isPlaying) player.pause() else player.play()
                            reveal()
                            true
                        }
                        else -> { reveal(); false }
                    }
                },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                    setPlayer(player)
                }
            },
            // keepScreenOn only while actually playing, so the TV can still sleep
            // when paused or stopped but never during live playback.
            update = { view ->
                view.setPlayer(player)
                view.keepScreenOn = isPlaying
            },
        )

        // Top scrim: a black→transparent gradient so the top info stays legible
        // over bright video. Shown whenever there's top info (overlay or loading).
        AnimatedVisibility(
            visible = error == null && (controlsVisible || buffering),
            enter = fadeIn(tween(FADE_MS)),
            exit = fadeOut(tween(FADE_MS)),
            modifier = Modifier.align(Alignment.TopCenter),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(TOP_SCRIM_HEIGHT.dp)
                        .background(
                            Brush.verticalGradient(listOf(Color(0xCC000000), Color.Transparent)),
                        ),
            )
        }

        // Loading: three animated dots, top-left.
        AnimatedVisibility(
            visible = buffering && error == null,
            enter = fadeIn(tween(FADE_MS)),
            exit = fadeOut(tween(FADE_MS)),
            modifier = Modifier.align(Alignment.TopStart).padding(28.dp),
        ) {
            LoadingDots()
        }

        // Network bitrate, top-right, refreshed while the overlay is visible.
        AnimatedVisibility(
            visible = controlsVisible && error == null,
            enter = fadeIn(tween(FADE_MS)),
            exit = fadeOut(tween(FADE_MS)),
            modifier = Modifier.align(Alignment.TopEnd).padding(28.dp),
        ) {
            Text(
                text = "%.1f Mb/s".format(bitrateBps / 1_000_000.0),
                color = Color.White,
                fontSize = 14.sp,
            )
        }

        // Channel name + quality, top-centre, with the overlay.
        AnimatedVisibility(
            visible = controlsVisible && error == null,
            enter = fadeIn(tween(FADE_MS)),
            exit = fadeOut(tween(FADE_MS)),
            modifier = Modifier.align(Alignment.TopCenter).padding(28.dp),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                (current ?: channel)?.let { Text(text = it.name, color = Color.White, fontSize = 18.sp) }
                if (variants.size > 1) {
                    // ◂ HD ▸ hints that left/right cycle the quality.
                    Text(
                        text = "◂ ${current?.qualityLabel ?: "auto"} ▸",
                        color = MUTED,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // Centre pause icon: shown only while paused, fading out when playback
        // resumes. No play icon — the overlay/OK already make resuming obvious.
        AnimatedVisibility(
            visible = error == null && !isPlaying,
            enter = fadeIn(tween(FADE_MS)),
            exit = fadeOut(tween(FADE_MS)),
            modifier = Modifier.align(Alignment.Center),
        ) {
            PauseIcon()
        }

        AnimatedVisibility(
            visible = error != null,
            enter = fadeIn(tween(FADE_MS)),
            exit = fadeOut(tween(FADE_MS)),
            modifier = Modifier.align(Alignment.Center).padding(24.dp),
        ) {
            ErrorPanel(
                channelName = channel?.name,
                message = error.orEmpty(),
                retryFocus = retryFocus,
                onRetry = ::play,
            )
        }
    }
}

@Composable
private fun LoadingDots(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "dots")
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(DOT_COUNT) { index ->
            val alpha by transition.animateFloat(
                initialValue = 0.3f,
                targetValue = 1f,
                animationSpec =
                    infiniteRepeatable(
                        animation = tween(durationMillis = 600, delayMillis = index * 200),
                        repeatMode = RepeatMode.Reverse,
                    ),
                label = "dot$index",
            )
            Box(
                modifier =
                    Modifier
                        .size(DOT_SIZE.dp)
                        .alpha(alpha)
                        .clip(CircleShape)
                        .background(LOADING_COLOR),
            )
        }
    }
}

@Composable
private fun PauseIcon() {
    Canvas(modifier = Modifier.size(72.dp)) {
        val w = size.width
        val h = size.height
        val paint =
            android.graphics
                .Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                .apply {
                    color = android.graphics.Color.WHITE
                    // Soft drop shadow so the icon reads over any video frame.
                    setShadowLayer(
                        SHADOW_RADIUS.dp.toPx(),
                        0f,
                        SHADOW_DY.dp.toPx(),
                        android.graphics.Color.argb(200, 0, 0, 0),
                    )
                }
        val canvas = drawContext.canvas.nativeCanvas
        val barWidth = w * 0.26f
        val radius = barWidth * 0.25f
        canvas.drawRoundRect(w * 0.16f, h * 0.06f, w * 0.16f + barWidth, h * 0.94f, radius, radius, paint)
        canvas.drawRoundRect(w * 0.58f, h * 0.06f, w * 0.58f + barWidth, h * 0.94f, radius, radius, paint)
    }
}

@Composable
private fun ErrorPanel(
    channelName: String?,
    message: String,
    retryFocus: FocusRequester,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        channelName?.let { Text(text = it, color = Color.White, fontSize = 22.sp) }
        Text(text = message, color = ERROR_RED, textAlign = TextAlign.Center)
        Button(onClick = onRetry, modifier = Modifier.focusRequester(retryFocus)) {
            Text(text = "Réessayer")
        }
    }
}

private const val AUTO_HIDE_MS = 4_000L
private const val BITRATE_POLL_MS = 1_000L
private const val FADE_MS = 400
private const val DOT_COUNT = 3
private const val DOT_SIZE = 8
private const val TOP_SCRIM_HEIGHT = 140
private const val SHADOW_RADIUS = 7
private const val SHADOW_DY = 2

private val LOADING_COLOR = Color.White
private val MUTED = Color(0xFFB6B6C2)
private val SCRIM = Color(0x66000000)
private val ERROR_RED = Color(0xFFFF6B6B)
