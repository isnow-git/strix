package dev.strix.core.model

/** Stable identifier for a channel, derived from its playlist entry. */
@JvmInline
value class ChannelId(
    val value: String,
)

/**
 * A single playable channel.
 *
 * Intentionally flat (no nested objects) so list rendering and diffing stay
 * allocation-cheap on low-RAM TVs. Immutable: marked stable for Compose via
 * `config/compose/stability_config.conf` since this module is not Compose-processed.
 */
data class Channel(
    val id: ChannelId,
    val name: String,
    val streamUrl: String,
    val logoUrl: String? = null,
    val group: String? = null,
    val number: Int? = null,
    /**
     * Fixed catalogue number, stable across every category view (generalist channels
     * first, then playlist order). 0 until assigned at import. Typing it on the remote
     * zaps straight to the channel, and it doubles as the row index (position + 1).
     */
    val channelNumber: Int = 0,
    /** Quality label of this variant (e.g. "FHD"), or null if unmarked. */
    val qualityLabel: String? = null,
    /** Provider EPG id (Xtream `epg_channel_id` / M3U `tvg-id`), for grouping + EPG. */
    val epgChannelId: String? = null,
    /** Cleaned display name (country/quality/junk stripped); falls back to [name]. */
    val displayName: String = "",
    /** Canonical category label resolved at import (iptv-org / classifier), or empty. */
    val category: String = "",
) {
    /** The label to render in lists: [displayName] when present, otherwise [name]. */
    val label: String get() = displayName.ifBlank { name }
}
