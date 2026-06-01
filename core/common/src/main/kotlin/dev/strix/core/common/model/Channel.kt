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
    /**
     * Fixed catalogue number, stable across every category view (Général channels
     * first in base order, then the rest). 0 until assigned at import. Typing it on
     * the remote zaps straight to the channel.
     */
    val channelNumber: Int = 0,
    /** Quality label of this variant (e.g. "FHD"), or null if unmarked. */
    val qualityLabel: String? = null,
    /** Provider EPG id (Xtream `epg_channel_id` / M3U `tvg-id`), for grouping/EPG. */
    val epgChannelId: String? = null,
    /** Cleaned name for display (country/quality/junk stripped); falls back to [name]. */
    val displayName: String = "",
    /** Canonical category label resolved at import (iptv-org / classifier), or empty. */
    val category: String = "",
)
