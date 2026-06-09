package dev.strix.feature.epg

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.strix.core.data.ChannelPagingRepository
import dev.strix.core.domain.epg.EpgRepository
import dev.strix.core.model.Channel
import dev.strix.core.model.epg.EpgProgramme
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Drives the program guide. Channels page vertically (the proven keyset source); each
 * row asks for its programmes over the shared, fixed time window.
 */
@HiltViewModel
class EpgTimelineViewModel
    @Inject
    constructor(
        pagingRepository: ChannelPagingRepository,
        private val epgRepository: EpgRepository,
    ) : ViewModel() {
        /** Window start, floored to the half-hour so the axis ticks read cleanly. */
        val windowStartSec: Long = (System.currentTimeMillis() / 1000 / HALF_HOUR_SEC) * HALF_HOUR_SEC
        val windowDurationSec: Long = WINDOW_HOURS * SECONDS_PER_HOUR
        val windowEndSec: Long get() = windowStartSec + windowDurationSec

        /** Current wall-clock second, for the "now" indicator. */
        fun nowSec(): Long = System.currentTimeMillis() / 1000

        val channels: Flow<PagingData<Channel>> =
            pagingRepository.pagedChannels(query = null, category = null).cachedIn(viewModelScope)

        /** Programmes for [channel] within the guide window (empty if no EPG). */
        suspend fun timeline(channel: Channel): List<EpgProgramme> =
            epgRepository.timeline(channel, windowStartSec, windowEndSec)

        private companion object {
            const val WINDOW_HOURS = 4L
            const val SECONDS_PER_HOUR = 3600L
            const val HALF_HOUR_SEC = 1800L
        }
    }
