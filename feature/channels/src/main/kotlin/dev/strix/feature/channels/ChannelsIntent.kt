package dev.strix.feature.channels

import dev.strix.core.common.model.Channel
import dev.strix.core.common.model.StreamSourceConfig

/** User intents for the channels screen (MVI). */
sealed interface ChannelsIntent {
    /** The search text changed. */
    data class SearchChanged(
        val query: String,
    ) : ChannelsIntent

    /** D-pad focus landed on a channel — drives debounced zapping/preview. */
    data class ChannelFocused(
        val channel: Channel,
    ) : ChannelsIntent

    /** Re-import channels from [source]. */
    data class Refresh(
        val source: StreamSourceConfig,
    ) : ChannelsIntent
}
