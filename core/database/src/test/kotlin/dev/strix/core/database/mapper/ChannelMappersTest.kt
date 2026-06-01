package dev.strix.core.database.mapper

import com.google.common.truth.Truth.assertThat
import dev.strix.core.common.model.Channel
import dev.strix.core.common.model.ChannelId
import org.junit.Test

class ChannelMappersTest {
    private val channel =
        Channel(
            id = ChannelId("uk.bbc.one"),
            name = "BBC One",
            streamUrl = "http://example.test/bbc1.m3u8",
            logoUrl = "http://example.test/bbc1.png",
            group = "UK",
            number = 101,
        )

    @Test
    fun `entity round-trips back to the same domain channel`() {
        // displayName is derived on read; everything else round-trips unchanged.
        val restored = channel.toEntity(sortIndex = 5).toDomain()
        assertThat(restored).isEqualTo(channel.copy(displayName = "BBC One"))
    }

    @Test
    fun `toEntity carries the supplied sortIndex and leaves rowid as default`() {
        val entity = channel.toEntity(sortIndex = 42)
        assertThat(entity.sortIndex).isEqualTo(42)
        assertThat(entity.rowid).isEqualTo(0L)
        assertThat(entity.channelId).isEqualTo("uk.bbc.one")
        assertThat(entity.groupTitle).isEqualTo("UK")
    }

    @Test
    fun `fts row mirrors the searchable fields`() {
        val fts = channel.toFtsEntity()
        assertThat(fts.channelId).isEqualTo("uk.bbc.one")
        assertThat(fts.name).isEqualTo("BBC One")
        assertThat(fts.groupTitle).isEqualTo("UK")
    }

    @Test
    fun `nullable fields survive the round-trip`() {
        val minimal =
            Channel(
                id = ChannelId("x"),
                name = "X",
                streamUrl = "http://x.test/x",
            )
        assertThat(minimal.toEntity(sortIndex = 0).toDomain())
            .isEqualTo(minimal.copy(displayName = "X"))
    }
}
