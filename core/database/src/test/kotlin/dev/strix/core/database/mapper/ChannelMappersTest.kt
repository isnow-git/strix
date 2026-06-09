package dev.strix.core.database.mapper

import com.google.common.truth.Truth.assertThat
import dev.strix.core.model.Channel
import dev.strix.core.model.ChannelClassifier
import dev.strix.core.model.ChannelId
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
        // displayName and category are derived at import; everything else is unchanged.
        val restored = channel.toEntity(sortIndex = 5).toDomain()
        val category = ChannelClassifier.classify(channel.name, channel.group).label
        assertThat(restored).isEqualTo(channel.copy(displayName = "BBC One", category = category))
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
        val category = ChannelClassifier.classify(minimal.name, minimal.group).label
        assertThat(minimal.toEntity(sortIndex = 0).toDomain())
            .isEqualTo(minimal.copy(displayName = "X", category = category))
    }
}
