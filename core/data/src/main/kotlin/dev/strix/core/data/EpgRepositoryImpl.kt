package dev.strix.core.data

import dev.strix.core.common.dispatcher.DispatcherProvider
import dev.strix.core.common.epg.EpgProgramme
import dev.strix.core.common.epg.EpgRepository
import dev.strix.core.common.epg.NowNext
import dev.strix.core.common.model.Channel
import dev.strix.core.common.model.StreamSourceConfig
import dev.strix.core.common.onboarding.CredentialStore
import dev.strix.core.network.xtream.XtreamClient
import dev.strix.core.network.xtream.XtreamEpgEntry
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Now/next EPG via Xtream `get_short_epg`, fetched lazily per channel and cached
 * in memory with a short TTL so opening or focusing a channel costs at most one
 * request. Plain M3U sources (no EPG API) and non-Xtream channels return null.
 */
@Singleton
class EpgRepositoryImpl
    @Inject
    constructor(
        private val credentialStore: CredentialStore,
        private val xtreamClient: XtreamClient,
        private val dispatchers: DispatcherProvider,
    ) : EpgRepository {
        private val cache = ConcurrentHashMap<String, Cached>()

        override suspend fun nowNext(channel: Channel): NowNext? =
            withContext(dispatchers.io) {
                val id = channel.id.value
                if (!id.startsWith(XTREAM_PREFIX)) return@withContext null
                val streamId = id.substringAfterLast(':').toLongOrNull() ?: return@withContext null
                val config = credentialStore.current() as? StreamSourceConfig.Xtream
                    ?: return@withContext null

                cache[id]?.takeIf { it.expiresAt > now() }?.let { return@withContext it.value }

                val nowNext =
                    try {
                        toNowNext(xtreamClient.shortEpg(config, streamId, EPG_LIMIT))
                    } catch (e: IOException) {
                        null
                    }
                if (nowNext != null) cache[id] = Cached(nowNext, now() + TTL_MS)
                nowNext
            }

        private fun toNowNext(entries: List<XtreamEpgEntry>): NowNext {
            val nowSec = now() / 1000
            val programmes = entries.mapNotNull { it.toProgramme() }.sortedBy { it.startEpochSec }
            return NowNext(
                current = programmes.firstOrNull { nowSec in it.startEpochSec until it.endEpochSec },
                next = programmes.firstOrNull { it.startEpochSec > nowSec },
            )
        }

        private fun XtreamEpgEntry.toProgramme(): EpgProgramme? {
            val start = startTs ?: return null
            val end = stopTs ?: return null
            val title = titleBase64?.let(::decodeBase64)?.takeIf { it.isNotBlank() } ?: return null
            return EpgProgramme(title = title, startEpochSec = start, endEpochSec = end)
        }

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
        }
    }
