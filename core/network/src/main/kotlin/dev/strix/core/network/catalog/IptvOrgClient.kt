package dev.strix.core.network.catalog

import dev.strix.core.common.epg.normalizeEpgId
import dev.strix.core.common.model.ChannelCategory
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

/** Minimal view of an iptv-org channel record. */
@kotlinx.serialization.Serializable
private data class IptvOrgChannel(
    val id: String? = null,
    val categories: List<String> = emptyList(),
)

/**
 * Fetches iptv-org's community channel catalogue to classify channels accurately
 * by their canonical id (the same xmltv id we already use), instead of guessing
 * from the name. Falls back to the keyword classifier when a channel isn't found.
 */
class IptvOrgClient
    @Inject
    constructor(
        private val client: OkHttpClient,
    ) {
        private val json = Json { ignoreUnknownKeys = true }

        /** @return normalized channel id -> canonical category label. Blocking. */
        @OptIn(ExperimentalSerializationApi::class)
        fun categoryMap(): Map<String, String> {
            val request = Request.Builder().url(CHANNELS_URL).build()
            return client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected HTTP ${response.code}")
                // channels.json is multi-MB; decode off the stream rather than
                // buffering the whole payload into a String first.
                val stream = response.body?.byteStream() ?: throw IOException("Empty iptv-org response")
                val channels =
                    try {
                        json.decodeFromStream<List<IptvOrgChannel>>(stream)
                    } catch (e: SerializationException) {
                        throw IOException("Invalid iptv-org payload", e)
                    }
                val out = HashMap<String, String>(channels.size)
                for (channel in channels) {
                    val id = channel.id
                    val category = id?.let { ChannelCategory.fromIptvOrg(channel.categories) }
                    if (id != null && category != null) {
                        out[normalizeEpgId(id)] = category.label
                    }
                }
                out
            }
        }

        private companion object {
            const val CHANNELS_URL = "https://iptv-org.github.io/api/channels.json"
        }
    }
