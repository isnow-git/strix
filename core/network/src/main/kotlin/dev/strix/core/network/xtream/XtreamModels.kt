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
)
