package dev.strix.core.common.model

/** Stable identifier for a channel (derived from the playlist entry). */
@JvmInline
value class ChannelId(
    val value: String,
)

/**
 * A single playable channel. Intentionally flat (no nested objects) so list
 * rendering and diffing stay allocation-cheap on low-RAM TVs.
 */
data class Channel(
    val id: ChannelId,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val group: String? = null,
    val number: Int? = null,
    /** Quality label of this variant (e.g. "FHD"), or null if unmarked. */
    val qualityLabel: String? = null,
)
