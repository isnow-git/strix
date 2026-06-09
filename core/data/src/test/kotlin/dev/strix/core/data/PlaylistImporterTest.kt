package dev.strix.core.data

import com.google.common.truth.Truth.assertThat
import dev.strix.core.database.entity.ChannelEntity
import dev.strix.core.database.entity.ChannelFtsEntity
import dev.strix.core.model.Channel
import dev.strix.core.model.ChannelId
import kotlinx.coroutines.test.runTest
import org.junit.Test

class PlaylistImporterTest {
    private fun channels(count: Int): Sequence<Channel> =
        (0 until count).asSequence().map {
            Channel(id = ChannelId("id$it"), name = "Ch $it", streamUrl = "http://host/$it")
        }

    private class RecordingSink {
        val batches = mutableListOf<List<ChannelEntity>>()
        val ftsBatches = mutableListOf<List<ChannelFtsEntity>>()
        val sink: suspend (List<ChannelEntity>, List<ChannelFtsEntity>) -> Unit = { c, f ->
            batches += c
            ftsBatches += f
        }
        val allChannels get() = batches.flatten()
    }

    @Test
    fun `chunks the sequence into fixed-size batches and reports the total`() =
        runTest {
            val sink = RecordingSink()
            val total = PlaylistImporter(batchSize = 100).import(channels(250), sink.sink)

            assertThat(total).isEqualTo(250)
            assertThat(sink.batches.map { it.size }).containsExactly(100, 100, 50).inOrder()
            assertThat(sink.ftsBatches.map { it.size }).containsExactly(100, 100, 50).inOrder()
        }

    @Test
    fun `assigns a contiguous sortIndex in playlist order`() =
        runTest {
            val sink = RecordingSink()
            PlaylistImporter(batchSize = 10).import(channels(25), sink.sink)

            val sortIndices = sink.allChannels.map { it.sortIndex }
            assertThat(sortIndices).isEqualTo((0 until 25).toList())
        }

    @Test
    fun `emits a single trailing batch when smaller than batchSize`() =
        runTest {
            val sink = RecordingSink()
            val total = PlaylistImporter(batchSize = 500).import(channels(3), sink.sink)

            assertThat(total).isEqualTo(3)
            assertThat(sink.batches).hasSize(1)
            assertThat(sink.batches.single()).hasSize(3)
        }

    @Test
    fun `writes nothing for an empty playlist`() =
        runTest {
            val sink = RecordingSink()
            val total = PlaylistImporter().import(emptySequence(), sink.sink)

            assertThat(total).isEqualTo(0)
            assertThat(sink.batches).isEmpty()
        }

    @Test
    fun `rejects a non-positive batch size`() {
        assertThat(runCatching { PlaylistImporter(batchSize = 0) }.exceptionOrNull())
            .isInstanceOf(IllegalArgumentException::class.java)
    }
}
