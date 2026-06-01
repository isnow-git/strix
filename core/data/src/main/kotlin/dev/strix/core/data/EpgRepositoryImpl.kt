package dev.strix.core.data

import dev.strix.core.common.dispatcher.DispatcherProvider
import dev.strix.core.common.epg.EpgProgramme
import dev.strix.core.common.epg.EpgRepository
import dev.strix.core.common.epg.NowNext
import dev.strix.core.common.epg.normalizeEpgId
import dev.strix.core.common.model.Channel
import dev.strix.core.common.model.StreamSourceConfig
import dev.strix.core.common.onboarding.CredentialStore
import dev.strix.core.database.dao.ChannelDao
import dev.strix.core.database.dao.EpgProgrammeDao
import dev.strix.core.database.entity.ChannelEntity
import dev.strix.core.database.entity.EpgProgrammeEntity
import dev.strix.core.network.xmltv.XmltvParser
import dev.strix.core.network.xtream.XtreamClient
import dev.strix.core.network.xtream.XtreamEpgEntry
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import java.util.zip.GZIPInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * EPG with two layers, best-effort and cheap on a TV:
 *
 * 1. **Authoritative XMLTV** (a trusted public guide), [refresh]ed into Room for
 *    the user's channels and matched by a normalized id — this corrects the
 *    provider's wrong/missing entries.
 * 2. **Provider fallback** (Xtream) when the XMLTV has nothing for a channel,
 *    with id inheritance from a live sibling and time-shift correction.
 *
 * now/next is resolved lazily per channel and cached with a short TTL.
 */
@Singleton
class EpgRepositoryImpl
    @Inject
    constructor(
        private val credentialStore: CredentialStore,
        private val xtreamClient: XtreamClient,
        private val channelDao: ChannelDao,
        private val epgDao: EpgProgrammeDao,
        private val okHttpClient: OkHttpClient,
        private val dispatchers: DispatcherProvider,
    ) : EpgRepository {
        private val xmltvParser = XmltvParser()
        private val cache = ConcurrentHashMap<String, Cached>()

        override suspend fun nowNext(channel: Channel): NowNext? =
            withContext(dispatchers.io) {
                val id = channel.id.value
                val row = channelDao.findByChannelId(id) ?: return@withContext null

                cache[id]?.takeIf { it.expiresAt > now() }?.let { return@withContext it.value }

                val offsetHours = row.timeshift.takeIf { it > 0 } ?: 0
                val refSec = now() / 1000 - offsetHours * SECONDS_PER_HOUR
                val epgId = row.epgChannelId ?: channelDao.epgSibling(row.epgBaseKey)?.epgChannelId

                val nowNext = fromXmltv(epgId, refSec) ?: fromProvider(row, offsetHours, refSec)
                if (nowNext != null) cache[id] = Cached(nowNext, now() + TTL_MS)
                nowNext
            }

        private suspend fun fromXmltv(
            epgId: String?,
            refSec: Long,
        ): NowNext? {
            val norm = epgId?.let(::normalizeEpgId)?.takeIf { it.isNotEmpty() } ?: return null
            val rows = epgDao.around(norm, refSec, AROUND_LIMIT)
            if (rows.isEmpty()) return null
            return NowNext(
                current = rows.firstOrNull { it.startSec <= refSec }?.toProgramme(),
                next = rows.firstOrNull { it.startSec > refSec }?.toProgramme(),
            )
        }

        private suspend fun fromProvider(
            row: ChannelEntity,
            offsetHours: Int,
            refSec: Long,
        ): NowNext? {
            if (!row.channelId.startsWith(XTREAM_PREFIX)) return null
            val config = credentialStore.current() as? StreamSourceConfig.Xtream ?: return null
            val ownStreamId = streamIdOf(row.channelId) ?: return null
            val sourceStreamId =
                if (row.epgChannelId != null) {
                    ownStreamId
                } else {
                    channelDao.epgSibling(row.epgBaseKey)?.let { streamIdOf(it.channelId) } ?: ownStreamId
                }
            return try {
                val entries =
                    if (offsetHours == 0) {
                        xtreamClient.shortEpg(config, sourceStreamId, EPG_LIMIT)
                    } else {
                        xtreamClient.fullEpg(config, sourceStreamId)
                    }
                toNowNext(entries, refSec)
            } catch (e: IOException) {
                null
            }
        }

        override suspend fun refresh() =
            withContext(dispatchers.io) {
                val ids = channelDao.distinctEpgChannelIds().mapTo(HashSet()) { normalizeEpgId(it) }
                if (ids.isEmpty()) return@withContext
                try {
                    val request = Request.Builder().url(XMLTV_URL).build()
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) return@withContext
                        val body = response.body ?: return@withContext
                        val collected = ArrayList<EpgProgrammeEntity>()
                        GZIPInputStream(body.byteStream()).use { stream ->
                            xmltvParser.parse(stream, keep = { it in ids }) { programme ->
                                collected +=
                                    EpgProgrammeEntity(
                                        normChannelId = programme.normChannelId,
                                        startSec = programme.startSec,
                                        stopSec = programme.stopSec,
                                        title = programme.title,
                                        description = programme.description,
                                    )
                            }
                        }
                        epgDao.clear()
                        collected.chunked(INSERT_BATCH).forEach { epgDao.insertBatch(it) }
                        cache.clear()
                    }
                } catch (e: IOException) {
                    // No XMLTV this time; the provider fallback still serves EPG.
                }
            }

        private fun toNowNext(
            entries: List<XtreamEpgEntry>,
            refSec: Long,
        ): NowNext {
            val programmes = entries.mapNotNull { it.toProgramme() }.sortedBy { it.startEpochSec }
            return NowNext(
                current = programmes.firstOrNull { refSec in it.startEpochSec until it.endEpochSec },
                next = programmes.firstOrNull { it.startEpochSec > refSec },
            )
        }

        private fun EpgProgrammeEntity.toProgramme() =
            EpgProgramme(title = title, startEpochSec = startSec, endEpochSec = stopSec, description = description)

        private fun XtreamEpgEntry.toProgramme(): EpgProgramme? {
            val start = startTs ?: return null
            val end = stopTs ?: return null
            val title = titleBase64?.let(::decodeBase64)?.takeIf { it.isNotBlank() } ?: return null
            val description = descriptionBase64?.let(::decodeBase64)?.takeIf { it.isNotBlank() }
            return EpgProgramme(title = title, startEpochSec = start, endEpochSec = end, description = description)
        }

        private fun streamIdOf(channelId: String): Long? = channelId.substringAfterLast(':').toLongOrNull()

        @Suppress("TooGenericExceptionCaught") // Malformed base64 must not break EPG.
        private fun decodeBase64(value: String): String? =
            try {
                String(Base64.getMimeDecoder().decode(value.trim()), Charsets.UTF_8)
            } catch (e: Exception) {
                null
            }

        private fun now(): Long = System.currentTimeMillis()

        private class Cached(
            val value: NowNext,
            val expiresAt: Long,
        )

        private companion object {
            const val XTREAM_PREFIX = "xtream:"
            const val EPG_LIMIT = 4
            const val AROUND_LIMIT = 6
            const val INSERT_BATCH = 500
            const val TTL_MS = 5 * 60 * 1000L
            const val SECONDS_PER_HOUR = 3600L
            const val XMLTV_URL = "https://epgshare01.online/epgshare01/epg_ripper_FR1.xml.gz"
        }
    }
