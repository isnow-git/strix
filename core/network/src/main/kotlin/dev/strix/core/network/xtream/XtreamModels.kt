package dev.strix.core.network.xtream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** A live-stream category from `action=get_live_categories`. */
@Serializable
data class XtreamCategory(
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("category_name") val categoryName: String? = null,
)

/** A live stream from `action=get_live_streams`. */
@Serializable
data class XtreamStream(
    @SerialName("stream_id") val streamId: Long,
    @SerialName("name") val name: String? = null,
    @SerialName("stream_icon") val streamIcon: String? = null,
    @SerialName("category_id") val categoryId: String? = null,
    @SerialName("num") val number: Int? = null,
    @SerialName("epg_channel_id") val epgChannelId: String? = null,
)

/** `get_short_epg` envelope. */
@Serializable
data class XtreamEpgResponse(
    @SerialName("epg_listings") val listings: List<XtreamEpgEntry> = emptyList(),
)

/** One EPG listing; [titleBase64] is base64-encoded, timestamps are epoch seconds. */
@Serializable
data class XtreamEpgEntry(
    @SerialName("title") val titleBase64: String? = null,
    @SerialName("start_timestamp") val startTs: Long? = null,
    @SerialName("stop_timestamp") val stopTs: Long? = null,
)
