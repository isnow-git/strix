package dev.strix.feature.channels.state

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable

/**
 * Immutable MVI state for the top of the channels screen (search + category + status).
 *
 * The channel list itself flows separately as `PagingData` and the focused/preview
 * channel, playback picture and screen mode are their own `StateFlow`s — so a list
 * scroll, a focus change or a playback transition never recomposes this state, and
 * therefore never recomposes the header or category rail.
 */
@Immutable
data class ChannelsUiState(
    val query: String = "",
    val selectedCategory: String? = null,
    val isRefreshing: Boolean = false,
    @StringRes val errorRes: Int? = null,
)

/** Whether the screen is browsing the list (with a side preview) or fullscreen. */
enum class ScreenMode { Browsing, Fullscreen }

/**
 * A request to land focus on a channel after returning from fullscreen: scroll the
 * list to [index] then focus the row with [channelId]. Event-driven (consumed once),
 * replacing the old 120-iteration focus retry loop.
 */
@Immutable
data class PendingLanding(
    val index: Int,
    val channelId: String,
)
