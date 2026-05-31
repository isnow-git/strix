package dev.strix.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.strix.core.common.model.Channel
import dev.strix.core.common.model.ChannelId
import dev.strix.core.common.repository.ChannelRepository
import dev.strix.core.player.StrixPlayerFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * Holds the channel to play (resolved from the nav arg) and vends configured
 * [ExoPlayer] instances. The player itself is owned and released by the screen
 * via the lifecycle (ADR-0004) — the ViewModel never holds it, so a backgrounded
 * or popped screen frees the decoder immediately.
 */
@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        private val playerFactory: StrixPlayerFactory,
        channelRepository: ChannelRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val channelId = ChannelId(savedStateHandle.get<String>(ARG_CHANNEL_ID).orEmpty())

        val channel: StateFlow<Channel?> =
            flow { emit(channelRepository.channelById(channelId)) }
                .stateIn(
                    scope = viewModelScope,
                    started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
                    initialValue = null,
                )

        @UnstableApi
        fun createPlayer(): ExoPlayer = playerFactory.create()

        companion object {
            const val ARG_CHANNEL_ID = "channelId"
            private const val STOP_TIMEOUT_MS = 5_000L
        }
    }
