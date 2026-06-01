package dev.strix.feature.channels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
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

/**
 * Channel browser: a clean glass list, one channel per row, with a canonical
 * category rail and debounced FTS search. Rows are fixed-height and logos load
 * once (Coil cache), so scrolling stays smooth on low-RAM TVs.
 */
@Composable
fun ChannelsScreen(
    onPlay: (Channel) -> Unit,
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

    val previewPlayer = remember { viewModel.createPreviewPlayer().apply { volume = 0f } }
    var showVideo by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(previewPlayer) {
        val listener =
            object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_READY) showVideo = true
                }

                override fun onPlayerError(error: PlaybackException) {
                    showVideo = false
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
                if (event == Lifecycle.Event.ON_STOP) previewPlayer.pause()
            }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // After a longer pause on the focused channel, play its stream (muted) in the
    // preview; the logo shows until the video is ready, then it fades in.
    LaunchedEffect(previewChannel) {
        showVideo = false
        previewPlayer.stop()
        val channel = previewChannel ?: return@LaunchedEffect
        delay(PREVIEW_VIDEO_DELAY_MS)
        previewPlayer.setMediaItem(MediaItem.fromUri(channel.streamUrl))
        previewPlayer.prepare()
        previewPlayer.playWhenReady = true
    }

    StrixTheme {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(listOf(BACKGROUND_TOP, BACKGROUND)))
                    .padding(horizontal = 40.dp, vertical = 28.dp),
        ) {
            Header(
                query = state.query,
                count = channels.itemCount,
                searchFocus = searchFocus,
                onQueryChange = { viewModel.onIntent(ChannelsIntent.SearchChanged(it)) },
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
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                contentPadding = PaddingValues(bottom = 56.dp),
                            ) {
                                items(
                                    count = channels.itemCount,
                                    key = channels.itemKey { it.id.value },
                                    contentType = channels.itemContentType { "channel" },
                                ) { index ->
                                    val channel = channels[index] ?: return@items
                                    ChannelRow(
                                        position = index + 1,
                                        channel = channel,
                                        imageLoader = imageLoader,
                                        onFocused = {
                                            viewModel.onIntent(ChannelsIntent.ChannelFocused(channel))
                                        },
                                        onClick = { onPlay(channel) },
                                    )
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
                        player = previewPlayer,
                        showVideo = showVideo,
                        modifier = Modifier.width(PREVIEW_WIDTH.dp).fillMaxHeight().padding(start = 28.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun PreviewPanel(
    channel: Channel?,
    epg: NowNext?,
    imageLoader: ImageLoader,
    player: ExoPlayer,
    showVideo: Boolean,
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
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(16.dp)),
        ) {
            // Video sits behind, filling the frame (cropped to fill).
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    PlayerView(context).apply {
                        useController = false
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                        setPlayer(player)
                    }
                },
                update = { it.setPlayer(player) },
            )
            // Logo on top; fades out to reveal the video once it's ready.
            val logoAlpha by animateFloatAsState(
                targetValue = if (showVideo) 0f else 1f,
                animationSpec = tween(durationMillis = 450),
                label = "previewLogo",
            )
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .alpha(logoAlpha)
                        .background(LOGO_BG)
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
                        text = channel.displayName.ifBlank { channel.name }.take(1).uppercase(),
                        color = Color.White,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
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
            modifier = Modifier.weight(1f).focusRequester(searchFocus),
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
private fun ChannelRow(
    position: Int,
    channel: Channel,
    imageLoader: ImageLoader,
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
                text = "$position",
                color = MUTED,
                fontSize = 13.sp,
                maxLines = 1,
                modifier = Modifier.width(40.dp),
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
                text = channel.displayName.ifBlank { channel.name }.take(1).uppercase(),
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

private val BACKGROUND = Color(0xFF0B0B0F)
private val BACKGROUND_TOP = Color(0xFF15151F)
private val PRIMARY = Color(0xFF6C8CFF)
private val ON_PRIMARY = Color(0xFF0B0B0F)
private val LOGO_BG = Color(0x2EFFFFFF)
private val LOGO_BORDER = Color(0x40FFFFFF)
private val MUTED = Color(0xFFB6B6C2)
private val ERROR_RED = Color(0xFFFF6B6B)
