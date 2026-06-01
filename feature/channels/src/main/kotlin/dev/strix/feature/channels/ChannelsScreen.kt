package dev.strix.feature.channels

import android.view.LayoutInflater
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.ImageLoader
import coil.compose.AsyncImage
import dev.strix.core.common.epg.NowNext
import dev.strix.core.common.model.Channel
import dev.strix.core.ui.focus.focusRing
import dev.strix.core.ui.glass.glass
import dev.strix.core.ui.image.rememberStrixImageLoader
import dev.strix.core.ui.theme.StrixTheme
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

/**
 * Channel browser: a clean glass list, one channel per row, with a canonical
 * category rail and debounced FTS search. Rows are fixed-height and logos load
 * once (Coil cache), so scrolling stays smooth on low-RAM TVs.
 */
@Composable
fun ChannelsScreen(
    onChangeSource: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChannelsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val previewChannel by viewModel.previewChannel.collectAsStateWithLifecycle()
    val previewEpg by viewModel.previewEpg.collectAsStateWithLifecycle()
    val channels = viewModel.pagedChannels.collectAsLazyPagingItems()
    val imageLoader = rememberStrixImageLoader()
    val searchFocus = remember { FocusRequester() }

    val previewPlayer = remember { viewModel.createPreviewPlayer() }
    var showVideo by remember { mutableStateOf(false) }
    var playbackError by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var playerChannelId by remember { mutableStateOf<String?>(null) }
    var focusedChannelId by remember { mutableStateOf<String?>(null) }
    val fullscreenFocus = remember { FocusRequester() }
    val rowFocus = remember { FocusRequester() }

    // Remote keypad: isolated state so typing digits only recomposes the overlay.
    val keypad = rememberKeypadZapState()
    var searchFocused by remember { mutableStateOf(false) }
    // Highest channel number, so the keypad knows when a typed number is final.
    var maxNumber by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { maxNumber = viewModel.maxChannelNumber() }
    // Row to scroll to after a keypad zap (-1 = none); the list shows placeholders
    // so this jumps straight to the target however far down it is.
    var pendingScrollIndex by remember { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // Returning from fullscreen, land back on the channel. After a zap we jump the
    // list to the target's row, wait for that row to actually load into the window
    // (Paging fills it near-instantly thanks to the indexed query + jump), then
    // focus it.
    LaunchedEffect(expanded) {
        if (expanded || focusedChannelId == null) return@LaunchedEffect
        val scrollTo = pendingScrollIndex
        pendingScrollIndex = -1
        if (scrollTo >= 0) {
            // Land the target in the middle of the list (not glued to an edge), so
            // focusing it afterwards doesn't visibly re-scroll.
            val viewportPx = listState.layoutInfo.viewportSize.height
            val rowPx = with(density) { (ROW_HEIGHT + ROW_SPACING).dp.roundToPx() }
            val centerOffset = if (viewportPx > rowPx) -((viewportPx - rowPx) / 2) else 0
            runCatching { listState.scrollToItem(scrollTo, centerOffset) }
        }
        var tries = 0
        while (tries < FOCUS_RETRIES) {
            val rowReady = scrollTo < 0 || (scrollTo < channels.itemCount && channels.peek(scrollTo) != null)
            if (rowReady && runCatching { rowFocus.requestFocus() }.isSuccess) break
            delay(FOCUS_RETRY_MS)
            tries++
        }
    }
    var slotRect by remember { mutableStateOf(Rect.Zero) }
    var rootWidth by remember { mutableIntStateOf(0) }
    var rootHeight by remember { mutableIntStateOf(0) }
    val lifecycleOwner = LocalLifecycleOwner.current

    // Retries left for the current stream (live feeds often need one) before it's
    // declared unavailable. A plain holder, reset per channel.
    val streamRetries = remember { intArrayOf(0) }

    // Plays a channel on the shared preview player (no-op if already playing it).
    fun playOn(channel: Channel) {
        if (playerChannelId == channel.id.value) return
        showVideo = false
        playbackError = false
        streamRetries[0] = 0
        previewPlayer.setMediaItem(MediaItem.fromUri(channel.streamUrl))
        previewPlayer.prepare()
        previewPlayer.playWhenReady = true
        playerChannelId = channel.id.value
    }

    DisposableEffect(previewPlayer) {
        val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) {
                        showVideo = true
                        playbackError = false
                        streamRetries[0] = 0
                    }
                }

                override fun onPlayerError(error: PlaybackException) {
                    showVideo = false
                    // Retry once or twice before giving up — many IPTV feeds drop
                    // the first connection. Then surface a clear "unavailable".
                    if (streamRetries[0] < MAX_STREAM_RETRIES) {
                        streamRetries[0]++
                        previewPlayer.prepare()
                    } else {
                        playbackError = true
                    }
                }
            }
        previewPlayer.addListener(listener)
        onDispose {
            previewPlayer.removeListener(listener)
            previewPlayer.release()
        }
    }
    DisposableEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                // Pause (and mute the audio leak) when leaving for the full player;
                // resume when coming back so the preview keeps its sound.
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> previewPlayer.pause()
                    Lifecycle.Event.ON_RESUME -> if (previewPlayer.mediaItemCount > 0) previewPlayer.play()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // After a pause on the focused channel, play its stream in the preview. While
    // expanded, the fullscreen view drives the player instead.
    LaunchedEffect(previewChannel) {
        if (expanded) return@LaunchedEffect
        showVideo = false
        previewPlayer.stop()
        playerChannelId = null
        val channel = previewChannel ?: return@LaunchedEffect
        delay(PREVIEW_VIDEO_DELAY_MS)
        playOn(channel)
    }

    // Deterministic fullscreen feedback: a stream is "unavailable" if it errors out
    // (after retries) or never becomes ready within a timeout — so a zap to a dead
    // channel shows a clear message instead of an endless spinner.
    val unavailable by produceState(false, expanded, showVideo, playbackError, playerChannelId) {
        value = false
        if (!expanded || showVideo) return@produceState
        if (playbackError) {
            value = true
            return@produceState
        }
        delay(STREAM_UNAVAILABLE_TIMEOUT_MS)
        value = true
    }

    // Resolves a typed number and opens it fullscreen. Single place, called by the
    // keypad overlay once a number is final.
    suspend fun zapTo(number: Int) {
        val zap = viewModel.resolveZap(number) ?: return
        // Keep the list/preview state in sync so returning from fullscreen is coherent.
        // The preview LaunchedEffect above is guarded by `expanded`, so this won't
        // trigger a stop/restart of the stream we're about to play.
        viewModel.onIntent(ChannelsIntent.ChannelFocused(zap.channel))
        focusedChannelId = zap.channel.id.value
        pendingScrollIndex = zap.listIndex
        playOn(zap.channel)
        expanded = true
    }

    StrixTheme {
        Box(
            modifier =
                modifier
                    .fillMaxSize()
                    .onGloballyPositioned {
                        rootWidth = it.size.width
                        rootHeight = it.size.height
                    }
                    // Remote keypad capture: digits anywhere (except while typing a
                    // search) buffer a channel number instead of moving focus.
                    .onPreviewKeyEvent { event ->
                        if (event.type != KeyEventType.KeyDown || searchFocused) {
                            return@onPreviewKeyEvent false
                        }
                        val digit = event.key.digitChar() ?: return@onPreviewKeyEvent false
                        keypad.append(digit)
                        true
                    },
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(listOf(BACKGROUND_TOP, BACKGROUND)))
                        .padding(horizontal = 40.dp, vertical = 28.dp),
            ) {
                Header(
                    query = state.query,
                    count = channels.itemCount,
                    searchFocus = searchFocus,
                    onQueryChange = { viewModel.onIntent(ChannelsIntent.SearchChanged(it)) },
                    onSearchFocusChanged = { searchFocused = it },
                    onChangeSource = onChangeSource,
                )
                if (categories.isNotEmpty()) {
                    CategoryRail(
                        categories = categories,
                        selected = state.selectedCategory,
                        onSelect = { viewModel.onIntent(ChannelsIntent.CategorySelected(it)) },
                    )
                }
                state.errorMessage?.let { message ->
                    Text(text = message, color = ERROR_RED, modifier = Modifier.padding(top = 12.dp))
                }

                val refreshing = channels.loadState.refresh is LoadState.Loading
                Row(modifier = Modifier.fillMaxSize().padding(top = 20.dp)) {
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .onPreviewKeyEvent { event ->
                                    // Left from anywhere in the list jumps to search/filters.
                                    if (event.type == KeyEventType.KeyDown && event.key == Key.DirectionLeft) {
                                        runCatching { searchFocus.requestFocus() }
                                        true
                                    } else {
                                        false
                                    }
                                },
                    ) {
                        when {
                            channels.itemCount == 0 && refreshing -> CenterMessage("Chargement…")
                            channels.itemCount == 0 ->
                                CenterMessage("Aucune chaîne. Change la source pour en importer.")
                            else ->
                                LazyColumn(
                                    state = listState,
                                    verticalArrangement = Arrangement.spacedBy(ROW_SPACING.dp),
                                    contentPadding = PaddingValues(bottom = 56.dp),
                                ) {
                                    items(
                                        count = channels.itemCount,
                                        key = channels.itemKey { it.id.value },
                                        // Same content type for placeholder and real rows
                                        // so the LazyColumn reuses the slot when a row loads.
                                        contentType = { "channel" },
                                    ) { index ->
                                        val channel = channels[index]
                                        if (channel == null) {
                                            ChannelRowSkeleton()
                                        } else {
                                            ChannelRow(
                                                channel = channel,
                                                imageLoader = imageLoader,
                                                focusRequester =
                                                    if (channel.id.value == focusedChannelId) rowFocus else null,
                                                onFocused = {
                                                    focusedChannelId = channel.id.value
                                                    viewModel.onIntent(ChannelsIntent.ChannelFocused(channel))
                                                },
                                                onClick = {
                                                    playOn(channel)
                                                    expanded = true
                                                },
                                            )
                                        }
                                    }
                                }
                        }
                    }
                    AnimatedVisibility(
                        visible = previewChannel != null,
                        enter = slideInHorizontally(tween(280)) { it / 3 } + fadeIn(tween(280)),
                        exit = slideOutHorizontally(tween(220)) { it / 3 } + fadeOut(tween(220)),
                    ) {
                        PreviewPanel(
                            channel = previewChannel,
                            epg = previewEpg,
                            imageLoader = imageLoader,
                            onSlotBounds = { slotRect = it },
                            modifier = Modifier.width(PREVIEW_WIDTH.dp).fillMaxHeight().padding(start = 28.dp),
                        )
                    }
                }
            }

            // Scrim + docked/fullscreen player + buffering/unavailable feedback.
            // Self-contained so the per-frame zoom animation recomposes only this
            // subtree, not the whole screen (header, list, rail stay put).
            FullscreenOverlay(
                player = previewPlayer,
                hasPreview = previewChannel != null,
                expanded = expanded,
                showVideo = showVideo,
                unavailable = unavailable,
                slotRect = slotRect,
                rootWidth = rootWidth,
                rootHeight = rootHeight,
                focus = fullscreenFocus,
                density = density,
                onBack = { expanded = false },
                onToggle = { if (previewPlayer.isPlaying) previewPlayer.pause() else previewPlayer.play() },
            )

            // Typed channel number (bottom-right), isolated so digits don't recompose
            // the screen; commits the final number via zapTo.
            KeypadOverlay(state = keypad, maxNumber = maxNumber, onCommit = { zapTo(it) })
        }
    }
}

/** Maps a D-pad/keypad digit key (top row or num-pad) to its character, else null. */
private fun Key.digitChar(): Char? =
    when (this) {
        Key.Zero, Key.NumPad0 -> '0'
        Key.One, Key.NumPad1 -> '1'
        Key.Two, Key.NumPad2 -> '2'
        Key.Three, Key.NumPad3 -> '3'
        Key.Four, Key.NumPad4 -> '4'
        Key.Five, Key.NumPad5 -> '5'
        Key.Six, Key.NumPad6 -> '6'
        Key.Seven, Key.NumPad7 -> '7'
        Key.Eight, Key.NumPad8 -> '8'
        Key.Nine, Key.NumPad9 -> '9'
        else -> null
    }

@Composable
private fun FullscreenOverlay(
    player: ExoPlayer,
    hasPreview: Boolean,
    expanded: Boolean,
    showVideo: Boolean,
    unavailable: Boolean,
    slotRect: Rect,
    rootWidth: Int,
    rootHeight: Int,
    focus: FocusRequester,
    density: Density,
    onBack: () -> Unit,
    onToggle: () -> Unit,
) {
    // Per-frame animation state lives here, not in the parent, so the zoom only
    // recomposes this subtree.
    val expandProgress by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = tween(durationMillis = 300, easing = LinearEasing),
        label = "expand",
    )
    val videoAlpha by animateFloatAsState(
        targetValue = if (showVideo) 1f else 0f,
        animationSpec = tween(durationMillis = if (showVideo) 400 else 0, easing = LinearEasing),
        label = "videoReveal",
    )

    // Black scrim behind the player as it grows to fullscreen.
    if (expandProgress > 0.001f) {
        Box(modifier = Modifier.fillMaxSize().alpha(expandProgress).background(Color.Black))
    }

    // Fullscreen feedback while there's no picture: a spinner while it connects,
    // or a clear message once it's declared unavailable — never an endless spinner.
    if (expanded && !showVideo) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            if (unavailable) {
                Text(text = "Chaîne indisponible", color = MUTED, fontSize = 16.sp)
            } else {
                LoadingSpinner()
            }
        }
    }

    // One player view: docked over the preview slot, animating its bounds to
    // fullscreen on open. One surface throughout → no black flash, no jump. Renders
    // while expanded even without a preview (keypad zap to an off-list channel),
    // falling back to a full-screen origin so there's no jump from a stale slot.
    if ((hasPreview || expanded) && rootWidth > 0) {
        val fullRect = Rect(0f, 0f, rootWidth.toFloat(), rootHeight.toFloat())
        val origin = if (slotRect != Rect.Zero) slotRect else fullRect
        FloatingPlayer(
            player = player,
            rect = lerpRect(origin, fullRect, expandProgress),
            cornerRadius = lerp(PREVIEW_RADIUS, 0f, expandProgress).dp,
            videoAlpha = videoAlpha,
            expanded = expanded,
            focus = focus,
            density = density,
            onBack = onBack,
            onToggle = onToggle,
        )
    }
}

private fun lerpRect(
    a: Rect,
    b: Rect,
    t: Float,
): Rect =
    Rect(
        left = lerp(a.left, b.left, t),
        top = lerp(a.top, b.top, t),
        right = lerp(a.right, b.right, t),
        bottom = lerp(a.bottom, b.bottom, t),
    )

@Composable
private fun FloatingPlayer(
    player: ExoPlayer,
    rect: Rect,
    cornerRadius: androidx.compose.ui.unit.Dp,
    videoAlpha: Float,
    expanded: Boolean,
    focus: FocusRequester,
    density: Density,
    onBack: () -> Unit,
    onToggle: () -> Unit,
) {
    LaunchedEffect(expanded) { if (expanded) runCatching { focus.requestFocus() } }
    val width = with(density) { rect.width.toDp() }
    val height = with(density) { rect.height.toDp() }
    val keys =
        if (expanded) {
            Modifier
                .focusRequester(focus)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.Back -> {
                            onBack()
                            true
                        }
                        Key.DirectionCenter, Key.Enter -> {
                            onToggle()
                            true
                        }
                        else -> false
                    }
                }
        } else {
            Modifier
        }
    Box(
        modifier =
            Modifier
                .offset { IntOffset(rect.left.roundToInt(), rect.top.roundToInt()) }
                .size(width, height)
                .alpha(videoAlpha)
                .clip(RoundedCornerShape(cornerRadius))
                .then(keys),
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                (LayoutInflater.from(context).inflate(R.layout.strix_player_view, null) as PlayerView).apply {
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setPlayer(player)
                }
            },
            update = { it.setPlayer(player) },
        )
    }
}

@Composable
private fun PreviewPanel(
    channel: Channel?,
    epg: NowNext?,
    imageLoader: ImageLoader,
    onSlotBounds: (Rect) -> Unit,
    modifier: Modifier = Modifier,
) {
    channel ?: return
    val current = epg?.current
    val description = current?.description

    val descScroll = rememberScrollState()
    // After a pause, slowly scroll the description so it can be read in full.
    LaunchedEffect(description) {
        descScroll.scrollTo(0)
        if (!description.isNullOrBlank()) {
            delay(DESC_SCROLL_DELAY_MS)
            val max = descScroll.maxValue
            if (max > 0) {
                descScroll.animateScrollTo(
                    max,
                    tween(durationMillis = max * DESC_SCROLL_MS_PER_PX, easing = LinearEasing),
                )
            }
        }
    }

    Column(
        modifier = modifier.glass(RoundedCornerShape(24.dp)).padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // The video is drawn by a single floating player positioned over this slot;
        // here we just show the logo and report the slot's bounds for docking.
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(16.dp))
                    .onGloballyPositioned { onSlotBounds(it.boundsInRoot()) }
                    .background(PREVIEW_LOGO_BG)
                    .border(1.dp, LOGO_BORDER, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            if (channel.logoUrl != null) {
                AsyncImage(
                    model = channel.logoUrl,
                    imageLoader = imageLoader,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                )
            } else {
                Text(
                    text =
                        channel.displayName
                            .ifBlank { channel.name }
                            .take(1)
                            .uppercase(),
                    color = Color.White,
                    fontSize = 44.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }

        if (current != null) {
            Text(text = current.title, color = Color.White, fontSize = 16.sp, maxLines = 2)
        } else {
            Text(text = "Programme indisponible", color = MUTED, fontSize = 13.sp)
        }

        if (!description.isNullOrBlank()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(descScroll)) {
                Text(text = description, color = MUTED, fontSize = 13.sp, lineHeight = 19.sp)
            }
        } else {
            Box(modifier = Modifier.weight(1f))
        }

        epg?.next?.let { next ->
            Text(text = "Puis · ${next.title}", color = MUTED, fontSize = 12.sp, maxLines = 1)
        }
    }
}

@Composable
private fun Header(
    query: String,
    count: Int,
    searchFocus: FocusRequester,
    onQueryChange: (String) -> Unit,
    onSearchFocusChanged: (Boolean) -> Unit,
    onChangeSource: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Column {
            Text(text = "Strix", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            if (count > 0) {
                Text(text = "$count chaînes", color = MUTED, fontSize = 13.sp)
            }
        }
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            modifier =
                Modifier
                    .weight(1f)
                    .focusRequester(searchFocus)
                    .onFocusChanged { onSearchFocusChanged(it.isFocused) },
        )
        GlassButton(label = "Changer la source", onClick = onChangeSource)
    }
}

@Composable
private fun CategoryRail(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp),
    ) {
        item {
            CategoryChip(label = "Toutes", selected = selected == null, onClick = { onSelect(null) })
        }
        items(categories) { category ->
            CategoryChip(
                label = category,
                selected = selected == category,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = transparentSurfaceColors(),
        modifier = Modifier.focusRing(cornerRadius = 20.dp),
    ) {
        val fill =
            if (selected) {
                Modifier.background(PRIMARY, RoundedCornerShape(20.dp))
            } else {
                Modifier.glass(RoundedCornerShape(20.dp))
            }
        Box(modifier = fill.padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(
                text = label,
                color = if (selected) ON_PRIMARY else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun GlassButton(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = transparentSurfaceColors(),
        modifier = Modifier.focusRing(cornerRadius = 14.dp),
    ) {
        Box(modifier = Modifier.glass(RoundedCornerShape(14.dp)).padding(horizontal = 18.dp, vertical = 12.dp)) {
            Text(text = label, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun LoadingSpinner() {
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
                color = Color.White,
                startAngle = 0f,
                sweepAngle = 270f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
        }
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = MUTED, fontSize = 16.sp)
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
        cursorBrush = SolidColor(Color.White),
        modifier =
            modifier
                .focusRing(cornerRadius = 12.dp)
                .glass(RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (query.isEmpty()) {
                Text(text = "Rechercher une chaîne", color = MUTED, fontSize = 16.sp)
            }
            inner()
        },
    )
}

@Composable
private fun ChannelRowSkeleton() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth().height(ROW_HEIGHT.dp).padding(horizontal = 14.dp),
    ) {
        Spacer(Modifier.width(44.dp))
        Box(Modifier.size(LOGO_SIZE.dp).clip(RoundedCornerShape(8.dp)).background(LOGO_BG))
        Box(
            Modifier
                .width(160.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(LOGO_BG),
        )
    }
}

@Composable
private fun ChannelRow(
    channel: Channel,
    imageLoader: ImageLoader,
    focusRequester: FocusRequester?,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    var focused by remember { mutableStateOf(false) }
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors = transparentSurfaceColors(),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT.dp)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged {
                    focused = it.isFocused
                    if (it.isFocused) onFocused()
                },
    ) {
        // Focused row lifts onto a glass panel; the rest stay flat (cheap).
        val surface = if (focused) Modifier.glass(RoundedCornerShape(14.dp)) else Modifier
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = surface.fillMaxSize().padding(horizontal = 14.dp),
        ) {
            Text(
                text = channel.channelNumber.toString(),
                color = MUTED,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.width(44.dp),
            )
            LogoBox(channel = channel, imageLoader = imageLoader)
            Text(
                text = channel.displayName.ifBlank { channel.name },
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun LogoBox(
    channel: Channel,
    imageLoader: ImageLoader,
) {
    Box(
        modifier =
            Modifier
                .size(LOGO_SIZE.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(LOGO_BG)
                .border(1.dp, LOGO_BORDER, RoundedCornerShape(8.dp)),
        contentAlignment = Alignment.Center,
    ) {
        if (channel.logoUrl != null) {
            AsyncImage(
                model = channel.logoUrl,
                imageLoader = imageLoader,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(6.dp),
            )
        } else {
            Text(
                text =
                    channel.displayName
                        .ifBlank { channel.name }
                        .take(1)
                        .uppercase(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun transparentSurfaceColors() =
    ClickableSurfaceDefaults.colors(
        containerColor = Color.Transparent,
        focusedContainerColor = Color.Transparent,
        pressedContainerColor = Color.Transparent,
    )

private const val ROW_HEIGHT = 64
private const val LOGO_SIZE = 52
private const val PREVIEW_WIDTH = 380
private const val PREVIEW_VIDEO_DELAY_MS = 1_300L
private const val DESC_SCROLL_DELAY_MS = 2_000L
private const val DESC_SCROLL_MS_PER_PX = 40
private const val PREVIEW_RADIUS = 16f

private const val SPINNER_PERIOD_MS = 900

// Stream retries before a channel is declared unavailable, and how long to wait
// for a first picture before showing that message rather than spinning forever.
private const val MAX_STREAM_RETRIES = 2
private const val STREAM_UNAVAILABLE_TIMEOUT_MS = 12_000L

// Returning from a zap, retry focusing the target row while its page finishes
// loading (generous budget so a cold jump still lands).
private const val FOCUS_RETRIES = 120
private const val FOCUS_RETRY_MS = 25L

// Vertical gap between channel rows (dp); also used to center a zapped row.
private const val ROW_SPACING = 6

private val BACKGROUND = Color(0xFF0B0B0F)
private val BACKGROUND_TOP = Color(0xFF15151F)
private val PRIMARY = Color(0xFF6C8CFF)
private val ON_PRIMARY = Color(0xFF0B0B0F)
private val LOGO_BG = Color(0x2EFFFFFF)
private val LOGO_BORDER = Color(0x40FFFFFF)
private val PREVIEW_LOGO_BG = Color(0xFF2C2C34)
private val MUTED = Color(0xFFB6B6C2)
private val ERROR_RED = Color(0xFFFF6B6B)
