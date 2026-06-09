package dev.strix.feature.channels

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.Text
import dev.strix.core.designsystem.theme.StrixPalette
import dev.strix.feature.channels.components.CategoryRail
import dev.strix.feature.channels.components.CenterMessage
import dev.strix.feature.channels.components.ChannelListDefaults
import dev.strix.feature.channels.components.ChannelRow
import dev.strix.feature.channels.components.ChannelRowSkeleton
import dev.strix.feature.channels.components.ChannelsHeader
import dev.strix.feature.channels.components.PreviewPanel
import dev.strix.feature.channels.playback.FullscreenPlayerScene
import dev.strix.feature.channels.state.ChannelsIntent
import dev.strix.feature.channels.state.ScreenMode
import dev.strix.feature.channels.zap.KeypadOverlay
import kotlinx.coroutines.delay

/**
 * The channel browser. A pure layout over [ChannelsViewModel]: it collects the VM's
 * state slices and hands stable parameters to the decomposed components, so strong
 * skipping keeps the header, rail and rows from recomposing on focus/scroll. The
 * per-frame morph lives inside [FullscreenPlayerScene]; the keypad OSD inside
 * [KeypadOverlay]; both recompose in isolation.
 */
@OptIn(UnstableApi::class)
@Composable
fun ChannelsScreen(
    viewModel: ChannelsViewModel,
    onChangeSource: () -> Unit,
    onOpenGuide: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val previewChannel by viewModel.previewChannel.collectAsStateWithLifecycle()
    val previewEpg by viewModel.previewEpg.collectAsStateWithLifecycle()
    val mode by viewModel.mode.collectAsStateWithLifecycle()
    val picture by viewModel.picture.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val landing by viewModel.pendingLanding.collectAsStateWithLifecycle()
    val keypadInput by viewModel.keypad.input.collectAsStateWithLifecycle()
    val keypadTuning by viewModel.keypadPreview.collectAsStateWithLifecycle()
    val channels = viewModel.pagedChannels.collectAsLazyPagingItems()

    val searchFocus = remember { FocusRequester() }
    val firstRowFocus = remember { FocusRequester() }
    val landingFocus = remember { FocusRequester() }
    val fullscreenFocus = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    var searchFocused by remember { mutableStateOf(false) }
    var slotRect by remember { mutableStateOf(Rect.Zero) }
    var rootWidth by remember { mutableIntStateOf(0) }
    var rootHeight by remember { mutableIntStateOf(0) }
    var initialFocusDone by remember { mutableStateOf(false) }

    // Pause the player when leaving the app; resume on return (keeps preview audio).
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        val observer =
            LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> viewModel.onAppPaused()
                    Lifecycle.Event.ON_RESUME -> viewModel.onAppResumed()
                    else -> Unit
                }
            }
        lifecycleOwner.lifecycle.addObserver(observer)
    }

    // Land focus on the first row once the catalogue first loads.
    LaunchedEffect(channels.itemCount) {
        if (!initialFocusDone && landing == null && channels.itemCount > 0) {
            initialFocusDone = true
            runCatching { firstRowFocus.requestFocus() }
        }
    }

    // After returning from fullscreen (or a zap), scroll the target into view and focus
    // it — event-driven, replacing the old fixed retry loop.
    LaunchedEffect(landing, mode) {
        val target = landing ?: return@LaunchedEffect
        if (mode != ScreenMode.Browsing) return@LaunchedEffect
        val viewportPx = listState.layoutInfo.viewportSize.height
        val rowPx = with(density) { (ChannelListDefaults.ROW_HEIGHT + ChannelListDefaults.ROW_SPACING).dp.roundToPx() }
        val centerOffset = if (viewportPx > rowPx) -((viewportPx - rowPx) / 2) else 0
        runCatching { listState.scrollToItem(target.index, centerOffset) }
        var tries = 0
        while (tries < LANDING_FOCUS_TRIES) {
            // The row must be loaded (a real ChannelRow, not a placeholder skeleton) AND
            // laid out in the viewport before its FocusRequester is attached. Checking only
            // data-loaded raced ahead of composition on far jumps (e.g. a zap to ch. 500),
            // so focus silently failed even though the list had scrolled there.
            val loaded = target.index < channels.itemCount && channels.peek(target.index) != null
            val laidOut = listState.layoutInfo.visibleItemsInfo.any { it.index == target.index }
            if (loaded && laidOut && runCatching { landingFocus.requestFocus() }.isSuccess) break
            delay(LANDING_RETRY_MS)
            tries++
        }
        viewModel.consumeLanding()
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onGloballyPositioned {
                    rootWidth = it.size.width
                    rootHeight = it.size.height
                }
                // Remote keypad: digits anywhere (except while typing a search) buffer a
                // channel number; OK commits it, Back/Del edits it.
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown || searchFocused) return@onPreviewKeyEvent false
                    val digit = event.key.digitChar()
                    when {
                        digit != null -> {
                            viewModel.keypad.append(digit)
                            true
                        }
                        keypadInput.isEmpty() -> false
                        event.key == Key.Backspace || event.key == Key.Delete -> {
                            viewModel.keypad.backspace()
                            true
                        }
                        event.key == Key.DirectionCenter || event.key == Key.Enter -> {
                            viewModel.keypad.commitNow()
                            true
                        }
                        else -> false
                    }
                },
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(listOf(StrixPalette.BackgroundElevated, StrixPalette.Background)),
                    ).padding(horizontal = 40.dp, vertical = 28.dp),
        ) {
            ChannelsHeader(
                query = state.query,
                channelCount = channels.itemCount,
                searchFocus = searchFocus,
                onQueryChange = { viewModel.onIntent(ChannelsIntent.SearchChanged(it)) },
                onSearchFocusChanged = { searchFocused = it },
                onChangeSource = onChangeSource,
                onOpenGuide = onOpenGuide,
            )
            if (categories.isNotEmpty()) {
                CategoryRail(
                    categories = categories,
                    selected = state.selectedCategory,
                    onSelect = { viewModel.onIntent(ChannelsIntent.CategorySelected(it)) },
                )
            }
            state.errorMessage?.let { message ->
                Text(text = message, color = StrixPalette.Error, modifier = Modifier.padding(top = 12.dp))
            }

            val refreshing = channels.loadState.refresh is LoadState.Loading
            Row(modifier = Modifier.fillMaxSize().padding(top = 20.dp)) {
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .onPreviewKeyEvent { event ->
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
                        channels.itemCount == 0 -> CenterMessage("Aucune chaîne. Change la source pour en importer.")
                        else ->
                            LazyColumn(
                                state = listState,
                                verticalArrangement = Arrangement.spacedBy(ChannelListDefaults.ROW_SPACING.dp),
                                contentPadding = PaddingValues(bottom = 56.dp),
                            ) {
                                items(
                                    count = channels.itemCount,
                                    key = channels.itemKey { it.id.value },
                                    contentType = { "channel" },
                                ) { index ->
                                    val channel = channels[index]
                                    if (channel == null) {
                                        ChannelRowSkeleton()
                                    } else {
                                        val rowFocus =
                                            when {
                                                channel.id.value == landing?.channelId -> landingFocus
                                                index == 0 -> firstRowFocus
                                                else -> null
                                            }
                                        ChannelRow(
                                            channel = channel,
                                            focusRequester = rowFocus,
                                            onFocused = { viewModel.onIntent(ChannelsIntent.ChannelFocused(channel)) },
                                            onClick = { viewModel.openFullscreen(channel, index) },
                                        )
                                    }
                                }
                            }
                    }
                }
                AnimatedVisibility(
                    visible = previewChannel != null,
                    enter = slideInHorizontally(tween(PREVIEW_ANIM_MS)) { it / 3 } + fadeIn(tween(PREVIEW_ANIM_MS)),
                    exit = slideOutHorizontally(tween(PREVIEW_EXIT_MS)) { it / 3 } + fadeOut(tween(PREVIEW_EXIT_MS)),
                ) {
                    PreviewPanel(
                        channel = previewChannel,
                        epg = previewEpg,
                        onSlotBounds = { slotRect = it },
                        modifier = Modifier.width(PREVIEW_WIDTH.dp).fillMaxHeight().padding(start = 28.dp),
                    )
                }
            }
        }

        FullscreenPlayerScene(
            player = viewModel.player,
            expanded = mode == ScreenMode.Fullscreen,
            hasPreview = previewChannel != null,
            picture = picture,
            channel = previewChannel,
            epg = previewEpg,
            isPlaying = isPlaying,
            slotRect = slotRect,
            rootWidth = rootWidth,
            rootHeight = rootHeight,
            focus = fullscreenFocus,
            onBack = { viewModel.exitFullscreen() },
            onToggle = { viewModel.togglePlayPause() },
        )

        KeypadOverlay(input = keypadInput, tuning = keypadTuning)
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

private const val PREVIEW_WIDTH = 380
private const val PREVIEW_ANIM_MS = 280
private const val PREVIEW_EXIT_MS = 220

// Generous budget so a cold far jump (paging load + compose) still lands focus.
private const val LANDING_FOCUS_TRIES = 120
private const val LANDING_RETRY_MS = 25L
