package dev.strix.feature.channels.state

import dev.strix.core.model.Channel
import dev.strix.core.model.StreamSourceConfig

/** User intents for the channels screen (MVI). */
sealed interface ChannelsIntent {
    /** The search text changed. */
    data class SearchChanged(
        val query: String,
    ) : ChannelsIntent

    /** A category was selected in the filter rail (null = all categories). */
    data class CategorySelected(
        val category: String?,
    ) : ChannelsIntent

    /** D-pad focus landed on a channel — drives the debounced side preview. */
    data class ChannelFocused(
        val channel: Channel,
    ) : ChannelsIntent

    /** Re-import channels from [source]. */
    data class Refresh(
        val source: StreamSourceConfig,
    ) : ChannelsIntent
}
