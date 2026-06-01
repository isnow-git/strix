package dev.strix.core.data

import dev.strix.core.common.dispatcher.DispatcherProvider
import dev.strix.core.common.epg.EpgProgramme
import dev.strix.core.common.epg.EpgRepository
import dev.strix.core.common.epg.NowNext
import dev.strix.core.common.model.Channel
import dev.strix.core.common.model.StreamSourceConfig
import dev.strix.core.common.onboarding.CredentialStore
import dev.strix.core.database.dao.ChannelDao
import dev.strix.core.network.xtream.XtreamClient
import dev.strix.core.network.xtream.XtreamEpgEntry
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Now/next EPG via Xtream, with two upgrades over the raw provider feed:
 *
 * - **Id inheritance**: a channel the provider didn't map to EPG (empty
 *   `epg_channel_id`, common for "+1"/backup feeds) borrows the EPG of a live
 *   sibling of the same logical channel ([ChannelDao.epgSibling]).
 * - **Time-shift correction**: for a delayed feed the reference time is moved
 *   back by the shift, and the full-day table ([XtreamClient.fullEpg]) is used so
 *   the programme that actually airs now is found.
 *
 * Lazy per channel and cached with a short TTL, so it stays cheap on a TV.
 */
@Singleton
class EpgRepositoryImpl
    @Inject
    constructor(
        private val credentialStore: CredentialStore,
        private val xtreamClient: XtreamClient,
        private val dao: ChannelDao,
        private val dispatchers: DispatcherProvider,
    ) : EpgRepository {
        private val cache = ConcurrentHashMap<String, Cached>()

        override suspend fun nowNext(channel: Channel): NowNext? =
            withContext(dispatchers.io) {
                val id = channel.id.value
                if (!id.startsWith(XTREAM_PREFIX)) return@withContext null
                val config = credentialStore.current() as? StreamSourceConfig.Xtream
                    ?: return@withContext null
                val ownStreamId = streamIdOf(id) ?: return@withContext null
                val row = dao.findByChannelId(id) ?: return@withContext null

                cache[id]?.takeIf { it.expiresAt > now() }?.let { return@withContext it.value }

                val offsetHours = row.timeshift.takeIf { it > 0 } ?: 0
                val sourceStreamId =
                    if (row.epgChannelId != null) {
                        ownStreamId
                    } else {
                        dao.epgSibling(row.epgBaseKey)?.let { streamIdOf(it.channelId) } ?: ownStreamId
                    }

                val nowNext =
                    try {
                        val entries =
                            if (offsetHours == 0) {
                                xtreamClient.shortEpg(config, sourceStreamId, EPG_LIMIT)
                            } else {
                                xtreamClient.fullEpg(config, sourceStreamId)
                            }
                        toNowNext(entries, referenceSec = now() / 1000 - offsetHours * SECONDS_PER_HOUR)
                    } catch (e: IOException) {
                        null
                    }
                if (nowNext != null) cache[id] = Cached(nowNext, now() + TTL_MS)
                nowNext
            }

        private fun toNowNext(
            entries: List<XtreamEpgEntry>,
            referenceSec: Long,
        ): NowNext {
            val programmes = entries.mapNotNull { it.toProgramme() }.sortedBy { it.startEpochSec }
            return NowNext(
                current = programmes.firstOrNull { referenceSec in it.startEpochSec until it.endEpochSec },
                next = programmes.firstOrNull { it.startEpochSec > referenceSec },
            )
        }

        private fun XtreamEpgEntry.toProgramme(): EpgProgramme? {
            val start = startTs ?: return null
            val end = stopTs ?: return null
            val title = titleBase64?.let(::decodeBase64)?.takeIf { it.isNotBlank() } ?: return null
            return EpgProgramme(title = title, startEpochSec = start, endEpochSec = end)
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
            const val TTL_MS = 5 * 60 * 1000L
            const val SECONDS_PER_HOUR = 3600L
        }
    }
