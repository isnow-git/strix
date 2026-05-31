package dev.strix.feature.channels

import androidx.compose.runtime.Immutable

/**
 * Immutable MVI state for the channels screen. The channel list itself flows
 * separately as `PagingData` (see [ChannelsViewModel.pagedChannels]) and is not
 * part of this state, so recomposition from list updates stays localised.
 */
@Immutable
data class ChannelsUiState(
    val query: String = "",
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
)
