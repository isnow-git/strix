package dev.strix.core.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import dev.strix.core.common.dispatcher.DispatcherProvider
import dev.strix.core.common.model.Channel
import dev.strix.core.common.model.ChannelId
import dev.strix.core.common.model.StreamSourceConfig
import dev.strix.core.common.repository.ChannelRepository
import dev.strix.core.common.result.StrixError
import dev.strix.core.common.result.StrixResult
import dev.strix.core.common.result.asFailure
import dev.strix.core.common.result.asSuccess
import dev.strix.core.database.dao.ChannelDao
import dev.strix.core.database.mapper.toDomain
import dev.strix.core.database.search.FtsQuery
import dev.strix.core.network.playlist.M3uParser
import dev.strix.core.network.resilience.retryWithBackoff
import dev.strix.core.network.xtream.XtreamClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import javax.inject.Inject

/**
 * Default [ChannelRepository] + [ChannelPagingRepository].
 *
 * Reads come straight from Room (paging + flows); a refresh streams the playlist
 * over OkHttp, parses it line by line, and writes it to Room in batches
 * ([PlaylistImporter]) so a large playlist is never held whole in RAM. The
 * Xtream path resolves channels through [XtreamClient] instead of parsing M3U.
 */
class ChannelRepositoryImpl
    @Inject
    constructor(
        private val dao: ChannelDao,
        private val okHttpClient: OkHttpClient,
        private val xtreamClient: XtreamClient,
        private val dispatchers: DispatcherProvider,
    ) : ChannelRepository,
        ChannelPagingRepository {
        private val parser = M3uParser()
        private val importer = PlaylistImporter()

        override fun observeChannels(query: String?): Flow<List<Channel>> {
            val match = query?.takeUnless { it.isBlank() }?.let(FtsQuery::prefixMatch)
            val rows = if (match == null) dao.observeAll() else dao.searchObserve(match)
            return rows.map { list -> list.map { it.toDomain() } }
        }

        override suspend fun channelById(id: ChannelId): Channel? =
            withContext(dispatchers.io) {
                dao.findByChannelId(id.value)?.toDomain()
            }

        override suspend fun nextChannel(id: ChannelId): Channel? =
            withContext(dispatchers.io) {
                dao.findNext(id.value)?.toDomain()
            }

        override suspend fun previousChannel(id: ChannelId): Channel? =
            withContext(dispatchers.io) {
                dao.findPrevious(id.value)?.toDomain()
            }

        override fun pagedChannels(
            query: String?,
            category: String?,
        ): Flow<PagingData<Channel>> {
            val match = query?.takeUnless { it.isBlank() }?.let(FtsQuery::prefixMatch)
            val group = category?.takeUnless { it.isBlank() }
            return Pager(
                config = PagingConfig(pageSize = PAGE_SIZE, enablePlaceholders = false),
                pagingSourceFactory = {
                    when {
                        match != null -> dao.searchPagingSource(match)
                        group != null -> dao.pagingSourceByGroup(group)
                        else -> dao.pagingSource()
                    }
                },
            ).flow.map { pagingData -> pagingData.map { it.toDomain() } }
        }

        override fun categories(): Flow<List<String>> = dao.observeCategories()

        override suspend fun refreshFrom(source: StreamSourceConfig): StrixResult<Int> =
            withContext(dispatchers.io) {
                when (source) {
                    is StreamSourceConfig.M3u -> importM3u(source.url)
                    is StreamSourceConfig.Xtream -> importXtream(source)
                }
            }

        private suspend fun importM3u(url: String): StrixResult<Int> =
            try {
                retryWithBackoff(
                    maxAttempts = MAX_FETCH_ATTEMPTS,
                    shouldRetry = { it is IOException },
                ) { fetchAndImportM3u(url) }.asSuccess()
            } catch (e: IOException) {
                StrixError.Network(message = e.message, cause = e).asFailure()
            }

        private suspend fun fetchAndImportM3u(url: String): Int {
            val request = Request.Builder().url(url).build()
            return okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Unexpected HTTP ${response.code}")
                val body = response.body ?: throw IOException("Empty response body")
                // Replace the catalogue: clear, then stream the new entries in batches.
                dao.clearChannels()
                dao.clearFts()
                body.charStream().buffered().useLines { lines ->
                    importer.import(parser.parse(lines)) { channels, fts ->
                        dao.insertBatch(channels, fts)
                    }
                }
            }
        }

        private suspend fun importXtream(config: StreamSourceConfig.Xtream): StrixResult<Int> =
            try {
                retryWithBackoff(
                    maxAttempts = MAX_FETCH_ATTEMPTS,
                    shouldRetry = { it is IOException },
                ) {
                    val channels = xtreamClient.liveChannels(config)
                    dao.clearChannels()
                    dao.clearFts()
                    importer.import(channels.asSequence()) { c, fts ->
                        dao.insertBatch(c, fts)
                    }
                }.asSuccess()
            } catch (e: IOException) {
                StrixError.Network(message = e.message, cause = e).asFailure()
            }

        private companion object {
            const val PAGE_SIZE = 40
            const val MAX_FETCH_ATTEMPTS = 3
        }
    }
