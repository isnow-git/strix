package dev.strix.feature.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.strix.core.common.result.StrixResult
import dev.strix.core.data.ChannelPagingRepository
import dev.strix.core.domain.epg.EpgRepository
import dev.strix.core.domain.onboarding.CredentialStore
import dev.strix.core.domain.repository.ChannelRepository
import dev.strix.core.model.Channel
import dev.strix.core.model.ChannelCategory
import dev.strix.core.model.StreamSourceConfig
import dev.strix.core.model.epg.NowNext
import dev.strix.core.player.StrixPlayerFactory
import dev.strix.core.player.playback.StreamPlaybackController
import dev.strix.feature.channels.state.ChannelsIntent
import dev.strix.feature.channels.state.ChannelsUiState
import dev.strix.feature.channels.state.PendingLanding
import dev.strix.feature.channels.state.ScreenMode
import dev.strix.feature.channels.zap.KeypadZapController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Single source of truth for the channels screen.
 *
 * Everything that the old screen scattered across composable-local `mutableStateOf` +
 * `LaunchedEffect`s — the player, the preview/fullscreen mode, the zap commit, the
 * "unavailable" timeout — lives here as explicit, sequenced state, so behaviour is
 * deterministic (no self-cancelling effects, no stale callbacks flipping UI state).
 *
 * - Search is debounced before it re-queries FTS.
 * - The paged list is `cachedIn(viewModelScope)` so it survives recomposition.
 * - One [StreamPlaybackController] owns the single ExoPlayer (preview *and* fullscreen
 *   are the same surface) and exposes an explicit [StreamPlaybackController.Picture].
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class, UnstableApi::class)
@HiltViewModel
class ChannelsViewModel
    @Inject
    constructor(
        private val pagingRepository: ChannelPagingRepository,
        private val channelRepository: ChannelRepository,
        private val epgRepository: EpgRepository,
        playerFactory: StrixPlayerFactory,
        private val credentialStore: CredentialStore,
    ) : ViewModel() {
        private val controller = StreamPlaybackController(playerFactory.createPreview(), viewModelScope)

        /** The single ExoPlayer instance the UI binds its PlayerView to. */
        val player: ExoPlayer get() = controller.player

        /** Explicit playback picture state (Connecting / Playing / Unavailable). */
        val picture: StateFlow<StreamPlaybackController.Picture> = controller.picture

        /** Whether the current stream is actively playing (drives the fullscreen overlay). */
        val isPlaying: StateFlow<Boolean> = controller.isPlaying

        /** Deterministic keypad entry; commits straight to [zap] on the VM scope. */
        val keypad = KeypadZapController(viewModelScope, onCommit = ::zap)

        /** The channel a typed-but-uncommitted keypad number points at, for the OSD hint. */
        val keypadPreview: StateFlow<String?> =
            keypad.input
                .debounce(KEYPAD_PREVIEW_DEBOUNCE_MS)
                .flatMapLatest { text ->
                    val number = text.toIntOrNull()
                    if (number == null) {
                        flowOf<String?>(null)
                    } else {
                        flow { emit(channelRepository.channelByNumber(number)?.label) }
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

        private val query = MutableStateFlow("")
        private val selectedCategory = MutableStateFlow<String?>(null)
        private val refreshing = MutableStateFlow(false)

        // Holds a @StringRes id (resolved by the screen) so the VM stays free of Context.
        private val errorRes = MutableStateFlow<Int?>(null)
        private val focusedChannel = MutableStateFlow<Channel?>(null)

        private val mutableMode = MutableStateFlow(ScreenMode.Browsing)
        val mode: StateFlow<ScreenMode> = mutableMode.asStateFlow()

        private val mutableLanding = MutableStateFlow<PendingLanding?>(null)

        /** Set after a zap/open; the screen scrolls + focuses the row, then [consumeLanding]. */
        val pendingLanding: StateFlow<PendingLanding?> = mutableLanding.asStateFlow()

        @Volatile
        private var highestNumber = 0

        val uiState: StateFlow<ChannelsUiState> =
            combine(query, selectedCategory, refreshing, errorRes) { query, category, refreshing, errorRes ->
                ChannelsUiState(
                    query = query,
                    selectedCategory = category,
                    isRefreshing = refreshing,
                    errorRes = errorRes,
                )
            }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ChannelsUiState())

        val categories: StateFlow<List<String>> =
            pagingRepository
                .categories()
                .map { labels -> labels.sortedBy { categoryOrder[it] ?: Int.MAX_VALUE } }
                .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

        val pagedChannels: Flow<PagingData<Channel>> =
            combine(
                query.debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged(),
                selectedCategory,
            ) { query, category -> query to category }
                .flatMapLatest { (query, category) ->
                    pagingRepository.pagedChannels(query.ifBlank { null }, category)
                }.cachedIn(viewModelScope)

        private val previewSource = focusedChannel.debounce(PREVIEW_DEBOUNCE_MS).distinctUntilChanged()

        /** The channel shown in the side preview (debounced focus). */
        val previewChannel: StateFlow<Channel?> =
            previewSource.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

        /** now/next EPG for the previewed channel; refetched as focus settles. */
        val previewEpg: StateFlow<NowNext?> =
            previewSource
                .flatMapLatest { channel ->
                    if (channel == null) {
                        flowOf<NowNext?>(null)
                    } else {
                        flow { emit(epgRepository.nowNext(channel)) }
                    }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

        init {
            viewModelScope.launch {
                highestNumber = channelRepository.channelCount()
                // Self-heal an empty catalogue (first run after a reset) from the stored source.
                if (highestNumber == 0) credentialStore.current()?.let { refresh(it) }
            }
            // Drive the side-preview stream from the settled focus, but only while
            // browsing — in fullscreen the same player is already showing the channel.
            viewModelScope.launch {
                previewChannel.collectLatest { channel ->
                    if (mutableMode.value == ScreenMode.Browsing && channel != null) {
                        controller.play(channel.streamUrl)
                    }
                }
            }
        }

        fun onIntent(intent: ChannelsIntent) {
            when (intent) {
                is ChannelsIntent.SearchChanged -> {
                    query.value = intent.query
                    if (intent.query.isNotBlank()) selectedCategory.value = null
                }
                is ChannelsIntent.CategorySelected -> {
                    selectedCategory.value = intent.category
                    query.value = ""
                }
                is ChannelsIntent.ChannelFocused -> focusedChannel.value = intent.channel
                is ChannelsIntent.Refresh -> refresh(intent.source)
            }
        }

        /** Opens [channel] fullscreen from a focused row at [listIndex] (for return landing). */
        fun openFullscreen(
            channel: Channel,
            listIndex: Int,
        ) {
            focusedChannel.value = channel
            mutableLanding.value = PendingLanding(listIndex, channel.id.value)
            controller.play(channel.streamUrl)
            mutableMode.value = ScreenMode.Fullscreen
        }

        /** Returns to the list; the preview keeps playing the focused channel. */
        fun exitFullscreen() {
            mutableMode.value = ScreenMode.Browsing
        }

        /** The screen calls this once it has landed focus after a return/zap. */
        fun consumeLanding() {
            mutableLanding.value = null
        }

        fun togglePlayPause() = controller.togglePlayPause()

        fun onAppPaused() = controller.pause()

        fun onAppResumed() = controller.resume()

        /**
         * Resolves a typed keypad [number] to its channel and opens it fullscreen. If the
         * channel isn't in the current category the view switches to "Toutes" (where its
         * row index is its fixed number − 1); otherwise it stays and uses the in-category
         * position. Runs entirely on [viewModelScope] so nothing in the UI can cancel it.
         */
        fun zap(number: Int) {
            viewModelScope.launch {
                val target = channelRepository.channelByNumber(number) ?: return@launch
                val current = selectedCategory.value
                val leaveCategory = current != null && target.category != current
                val newCategory = if (leaveCategory) null else current
                val index =
                    if (newCategory == null) {
                        (target.channelNumber - 1).coerceAtLeast(0)
                    } else {
                        channelRepository.positionInCategory(newCategory, target.id)
                    }
                if (newCategory != current) selectedCategory.value = newCategory
                openFullscreen(target, index)
            }
        }

        private fun refresh(source: StreamSourceConfig) {
            viewModelScope.launch {
                refreshing.value = true
                errorRes.value = null
                when (channelRepository.refreshFrom(source)) {
                    is StrixResult.Success -> {
                        highestNumber = channelRepository.channelCount()
                        // Defer the heavy EPG ingest so it doesn't compete with the first
                        // browse right after import (keeps the initial scroll smooth).
                        launch {
                            delay(EPG_INGEST_DELAY_MS)
                            epgRepository.refresh()
                        }
                    }
                    is StrixResult.Failure -> errorRes.value = R.string.channels_refresh_failed
                }
                refreshing.value = false
            }
        }

        override fun onCleared() {
            controller.release()
        }

        private companion object {
            val categoryOrder: Map<String, Int> =
                ChannelCategory.entries.withIndex().associate { it.value.label to it.index }

            const val SEARCH_DEBOUNCE_MS = 300L
            const val PREVIEW_DEBOUNCE_MS = 350L
            const val KEYPAD_PREVIEW_DEBOUNCE_MS = 150L
            const val STOP_TIMEOUT_MS = 5_000L

            // Wait past the first browse before ingesting EPG (download + parse) so the
            // initial scroll/zap stays smooth on a cold start.
            const val EPG_INGEST_DELAY_MS = 8_000L
        }
    }
