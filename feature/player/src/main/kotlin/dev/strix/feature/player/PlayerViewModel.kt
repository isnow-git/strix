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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Holds the currently playing channel (resolved from the nav arg, then updated by
 * D-pad zapping) and vends configured [ExoPlayer] instances. The player itself is
 * owned and released by the screen via the lifecycle (ADR-0004) — the ViewModel
 * never holds it, so a backgrounded or popped screen frees the decoder.
 */
@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        private val playerFactory: StrixPlayerFactory,
        private val channelRepository: ChannelRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val initialId = ChannelId(savedStateHandle.get<String>(ARG_CHANNEL_ID).orEmpty())

        private val _channel = MutableStateFlow<Channel?>(null)
        val channel: StateFlow<Channel?> = _channel.asStateFlow()

        init {
            viewModelScope.launch { _channel.value = channelRepository.channelById(initialId) }
        }

        /** Zaps to the next ([direction] > 0) or previous channel in playlist order. */
        fun zap(direction: Int) {
            val current = _channel.value ?: return
            viewModelScope.launch {
                val target =
                    if (direction > 0) {
                        channelRepository.nextChannel(current.id)
                    } else {
                        channelRepository.previousChannel(current.id)
                    }
                if (target != null) _channel.value = target
            }
        }

        @UnstableApi
        fun createPlayer(): ExoPlayer = playerFactory.create()

        companion object {
            const val ARG_CHANNEL_ID = "channelId"
        }
    }
