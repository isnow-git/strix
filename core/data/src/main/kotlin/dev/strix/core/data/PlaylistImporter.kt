package dev.strix.core.data

import dev.strix.core.common.model.Channel
import dev.strix.core.database.entity.ChannelEntity
import dev.strix.core.database.entity.ChannelFtsEntity
import dev.strix.core.database.mapper.toEntity
import dev.strix.core.database.mapper.toFtsEntity

/**
 * Drains a lazily-parsed [Channel] sequence into fixed-size batches and hands
 * each batch to [sink] for a single transactional write.
 *
 * This is the heart of the O(1)-memory import: the source sequence is consumed
 * incrementally and only [batchSize] entities are held at once, so a playlist of
 * any size never lands fully in RAM. `sortIndex` is assigned here so the stored
 * order matches the playlist without any runtime sort.
 */
class PlaylistImporter(
    private val batchSize: Int = DEFAULT_BATCH_SIZE,
) {
    init {
        require(batchSize > 0) { "batchSize must be > 0, was $batchSize" }
    }

    /** @return total number of channels written. */
    suspend fun import(
        channels: Sequence<Channel>,
        sink: suspend (channels: List<ChannelEntity>, fts: List<ChannelFtsEntity>) -> Unit,
    ): Int {
        var index = 0
        val channelBatch = ArrayList<ChannelEntity>(batchSize)
        val ftsBatch = ArrayList<ChannelFtsEntity>(batchSize)

        for (channel in channels) {
            channelBatch += channel.toEntity(sortIndex = index)
            ftsBatch += channel.toFtsEntity()
            index++
            if (channelBatch.size >= batchSize) {
                sink(channelBatch.toList(), ftsBatch.toList())
                channelBatch.clear()
                ftsBatch.clear()
            }
        }
        if (channelBatch.isNotEmpty()) {
            sink(channelBatch.toList(), ftsBatch.toList())
        }
        return index
    }

    private companion object {
        const val DEFAULT_BATCH_SIZE = 500
    }
}
