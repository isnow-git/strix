package dev.strix.core.network.xtream

import dev.strix.core.model.Channel
import dev.strix.core.model.StreamSourceConfig
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.io.InputStream
import java.net.URLEncoder
import javax.inject.Inject

/**
 * Reads a provider's live channels through the Xtream Codes API (`player_api.php`)
 * and turns them into [Channel]s.
 *
 * Categories are fetched first (for group names), then the stream list; each stream
 * becomes a `/<type>/<user>/<pass>/<id>.ts` URL ExoPlayer can play. The whole stream
 * list is parsed at once (one JSON array, unavoidable with this API) but only
 * transiently, during import — the caller still writes it to Room in batches.
 */
class XtreamClient
    @Inject
    constructor(
        private val client: OkHttpClient,
    ) {
        private val json = Json { ignoreUnknownKeys = true }

        /** @return the provider's live channels. Blocking; call on an IO dispatcher. */
        fun liveChannels(config: StreamSourceConfig.Xtream): List<Channel> {
            val base = normalizeXtreamBase(config.host)
            val categoryNames =
                decodeList<XtreamCategory>(apiUrl(base, config, "get_live_categories"))
                    .mapNotNull { c -> c.categoryId?.let { it to c.categoryName.orEmpty() } }
                    .toMap()
            return decodeList<XtreamStream>(apiUrl(base, config, "get_live_streams"))
                .mapNotNull { stream -> stream.toChannel(base, config, categoryNames) }
        }

        /** Now/next EPG listings for a stream (`get_short_epg`). Blocking. */
        fun shortEpg(
            config: StreamSourceConfig.Xtream,
            streamId: Long,
            limit: Int,
        ): List<XtreamEpgEntry> {
            val base = normalizeXtreamBase(config.host)
            val url = apiUrl(base, config, "get_short_epg") + "&stream_id=$streamId&limit=$limit"
            return decode<XtreamEpgResponse>(url).listings
        }

        /** Full-day EPG for a stream (`get_simple_data_table`); used for shifted feeds. */
        fun fullEpg(
            config: StreamSourceConfig.Xtream,
            streamId: Long,
        ): List<XtreamEpgEntry> {
            val base = normalizeXtreamBase(config.host)
            val url = apiUrl(base, config, "get_simple_data_table") + "&stream_id=$streamId"
            return decode<XtreamEpgResponse>(url).listings
        }

        @OptIn(ExperimentalSerializationApi::class)
        private inline fun <reified T> decodeList(url: String): List<T> =
            decodeBody(url) { json.decodeFromStream<List<T>>(it) }

        @OptIn(ExperimentalSerializationApi::class)
        private inline fun <reified T> decode(url: String): T = decodeBody(url) { json.decodeFromStream<T>(it) }

        // Decodes straight off the response stream — the live-stream list can be several
        // MB, and materializing it as a String first would double the peak memory for
        // nothing on a low-RAM TV.
        private inline fun <T> decodeBody(
            url: String,
            decode: (InputStream) -> T,
        ): T {
            val request = Request.Builder().url(url).build()
            return client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected HTTP ${response.code}")
                val stream = response.body?.byteStream() ?: throw IOException("Empty Xtream response")
                try {
                    decode(stream)
                } catch (e: SerializationException) {
                    throw IOException("Réponse Xtream invalide (provider injoignable ?)", e)
                }
            }
        }

        private fun apiUrl(
            base: String,
            config: StreamSourceConfig.Xtream,
            action: String,
        ): String =
            "$base/player_api.php?username=${enc(config.username)}" +
                "&password=${enc(config.password)}&action=$action"

        private fun enc(value: String): String = URLEncoder.encode(value, "UTF-8")
    }
