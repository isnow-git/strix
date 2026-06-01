package dev.strix.feature.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.strix.core.common.epg.EpgRepository
import dev.strix.core.common.epg.NowNext
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
 * Holds the current channel and its quality variants. Playback always targets
 * [current] (the selected variant); D-pad zapping changes the logical channel
 * (reloading its variants), and the user can cycle quality or the screen can
 * [fallback] to a lower quality automatically on error.
 *
 * The [ExoPlayer] is owned and released by the screen via the lifecycle
 * (ADR-0004); the ViewModel never holds it.
 */
@HiltViewModel
class PlayerViewModel
    @Inject
    constructor(
        private val playerFactory: StrixPlayerFactory,
        private val channelRepository: ChannelRepository,
        private val epgRepository: EpgRepository,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        private val initialId = ChannelId(savedStateHandle.get<String>(ARG_CHANNEL_ID).orEmpty())

        private val _channel = MutableStateFlow<Channel?>(null)

        /** The logical channel (representative variant), updated by zapping. */
        val channel: StateFlow<Channel?> = _channel.asStateFlow()

        private val _variants = MutableStateFlow<List<Channel>>(emptyList())

        /** Quality variants of [channel], best quality first. */
        val variants: StateFlow<List<Channel>> = _variants.asStateFlow()

        private val _current = MutableStateFlow<Channel?>(null)

        /** The variant actually playing (its [Channel.streamUrl] is loaded). */
        val current: StateFlow<Channel?> = _current.asStateFlow()

        private val _nowNext = MutableStateFlow<NowNext?>(null)

        /** Now/next EPG for the current channel, or null when unavailable. */
        val nowNext: StateFlow<NowNext?> = _nowNext.asStateFlow()

        init {
            viewModelScope.launch { selectChannel(channelRepository.channelById(initialId)) }
        }

        /** Zaps to the next ([direction] > 0) or previous channel in playlist order. */
        fun zap(direction: Int) {
            val cursor = _channel.value ?: return
            viewModelScope.launch {
                val target =
                    if (direction > 0) {
                        channelRepository.nextChannel(cursor.id)
                    } else {
                        channelRepository.previousChannel(cursor.id)
                    }
                if (target != null) selectChannel(target)
            }
        }

        /** Cycles quality: [step] +1 = lower, -1 = higher (variants are best-first). */
        fun cycleQuality(step: Int) {
            val list = _variants.value
            val playing = _current.value ?: return
            val index = list.indexOfFirst { it.id == playing.id }
            list.getOrNull(index + step)?.let { _current.value = it }
        }

        /** Switches to the next lower-quality variant; returns false if none left. */
        fun fallback(): Boolean {
            val list = _variants.value
            val playing = _current.value ?: return false
            val next = list.getOrNull(list.indexOfFirst { it.id == playing.id } + 1) ?: return false
            _current.value = next
            return true
        }

        private suspend fun selectChannel(channel: Channel?) {
            if (channel == null) return
            _channel.value = channel
            val list = channelRepository.variants(channel.id).ifEmpty { listOf(channel) }
            _variants.value = list
            _current.value = list.first()
            // Fetch EPG off the critical path so playback isn't delayed.
            _nowNext.value = null
            viewModelScope.launch { _nowNext.value = epgRepository.nowNext(channel) }
        }

        @UnstableApi
        fun createPlayer(): ExoPlayer = playerFactory.create()

        companion object {
            const val ARG_CHANNEL_ID = "channelId"
        }
    }
