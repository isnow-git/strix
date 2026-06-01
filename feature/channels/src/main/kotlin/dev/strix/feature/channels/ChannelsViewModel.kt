package dev.strix.feature.channels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.strix.core.common.epg.EpgRepository
import dev.strix.core.common.epg.NowNext
import dev.strix.core.common.model.Channel
import dev.strix.core.common.model.ChannelCategory
import dev.strix.core.common.model.StreamSourceConfig
import dev.strix.core.common.onboarding.CredentialStore
import dev.strix.core.common.repository.ChannelRepository
import dev.strix.core.common.result.StrixResult
import dev.strix.core.data.ChannelPagingRepository
import dev.strix.core.player.StrixPlayerFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * MVI ViewModel for the channels screen.
 *
 * - Search is debounced so each keystroke doesn't re-query the FTS index.
 * - The paged list is `cachedIn(viewModelScope)` so it survives recomposition
 *   and config changes without reloading.
 * - [playbackTarget] debounces the focused channel: as the user sweeps the D-pad
 *   across the grid, only the channel they settle on (after [ZAP_DEBOUNCE_MS])
 *   is emitted, and the collector is expected to use `collectLatest` so an
 *   in-flight load is cancelled by the next selection (zapping).
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class ChannelsViewModel
    @Inject
    constructor(
        private val pagingRepository: ChannelPagingRepository,
        private val channelRepository: ChannelRepository,
        private val epgRepository: EpgRepository,
        private val playerFactory: StrixPlayerFactory,
        private val credentialStore: CredentialStore,
    ) : ViewModel() {
        init {
            // Self-heal an empty catalogue (first run after a schema reset) by
            // re-importing from the stored source, so the user never lands on an
            // empty list when a source is already configured.
            viewModelScope.launch {
                if (channelRepository.channelCount() == 0) {
                    credentialStore.current()?.let { refresh(it) }
                }
            }
        }

        /** A muted player for the side preview; owned/released by the screen. */
        @UnstableApi
        fun createPreviewPlayer(): ExoPlayer = playerFactory.create()

        /** Resolves a typed keypad number to its channel (remote zapping), or null. */
        suspend fun channelByNumber(number: Int): Channel? = channelRepository.channelByNumber(number)

        private val query = MutableStateFlow("")
        private val refreshing = MutableStateFlow(false)
        private val error = MutableStateFlow<String?>(null)
        private val focusedChannel = MutableStateFlow<Channel?>(null)
        private val selectedCategory = MutableStateFlow<String?>(null)

        val uiState: StateFlow<ChannelsUiState> =
            combine(query, refreshing, error, selectedCategory) { query, refreshing, error, category ->
                ChannelsUiState(
                    query = query,
                    isRefreshing = refreshing,
                    errorMessage = error,
                    selectedCategory = category,
                )
            }.stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                initialValue = ChannelsUiState(),
            )

        val categories: StateFlow<List<String>> =
            pagingRepository
                .categories()
                .map { labels -> labels.sortedBy { label -> categoryOrder[label] ?: Int.MAX_VALUE } }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    initialValue = emptyList(),
                )

        val pagedChannels: Flow<PagingData<Channel>> =
            combine(
                query.debounce(SEARCH_DEBOUNCE_MS).distinctUntilChanged(),
                selectedCategory,
            ) { query, category -> query to category }
                .flatMapLatest { (query, category) ->
                    pagingRepository.pagedChannels(query.ifBlank { null }, category)
                }.cachedIn(viewModelScope)

        val playbackTarget: Flow<Channel> =
            focusedChannel
                .filterNotNull()
                .debounce(ZAP_DEBOUNCE_MS)
                .distinctUntilChanged()

        private val previewSource =
            focusedChannel.debounce(PREVIEW_DEBOUNCE_MS).distinctUntilChanged()

        /** The channel shown in the side preview (debounced focus). */
        val previewChannel: StateFlow<Channel?> =
            previewSource.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

        /** now/next EPG for the previewed channel; refetched as focus settles. */
        val previewEpg: StateFlow<NowNext?> =
            previewSource
                .flatMapLatest { channel ->
                    if (channel == null) flowOf<NowNext?>(null) else flow { emit(epgRepository.nowNext(channel)) }
                }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

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

        private fun refresh(source: StreamSourceConfig) {
            viewModelScope.launch {
                refreshing.value = true
                error.value = null
                when (val result = channelRepository.refreshFrom(source)) {
                    is StrixResult.Success -> launch { epgRepository.refresh() }
                    is StrixResult.Failure -> error.value = result.error.message ?: "Refresh failed"
                }
                refreshing.value = false
            }
        }

        private companion object {
            val categoryOrder: Map<String, Int> =
                ChannelCategory.entries.withIndex().associate { it.value.label to it.index }

            const val SEARCH_DEBOUNCE_MS = 300L
            const val ZAP_DEBOUNCE_MS = 300L
            const val PREVIEW_DEBOUNCE_MS = 350L
            const val STOP_TIMEOUT_MS = 5_000L
        }
    }
