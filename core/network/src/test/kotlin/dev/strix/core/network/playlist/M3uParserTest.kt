package dev.strix.core.network.playlist

import com.google.common.truth.Truth.assertThat
import dev.strix.core.common.model.ChannelId
import org.junit.Test

class M3uParserTest {
    private val parser = M3uParser()

    private fun parse(text: String) = parser.parse(text.lineSequence()).toList()

    @Test
    fun `parses a full entry with all attributes`() {
        val channels =
            parse(
                """
                #EXTM3U
                #EXTINF:-1 tvg-id="bbc1" tvg-name="BBC One HD" tvg-logo="http://l/bbc1.png" group-title="UK" tvg-chno="101",BBC One
                http://host/stream/bbc1
                """.trimIndent(),
            )

        assertThat(channels).hasSize(1)
        val c = channels.single()
        assertThat(c.id).isEqualTo(ChannelId("bbc1"))
        assertThat(c.name).isEqualTo("BBC One")
        assertThat(c.streamUrl).isEqualTo("http://host/stream/bbc1")
        assertThat(c.logoUrl).isEqualTo("http://l/bbc1.png")
        assertThat(c.group).isEqualTo("UK")
        assertThat(c.number).isEqualTo(101)
    }

    @Test
    fun `falls back to the url as id when tvg-id is absent`() {
        val channels =
            parse(
                """
                #EXTINF:-1 group-title="News",CNN
                http://host/cnn
                """.trimIndent(),
            )
        assertThat(channels.single().id).isEqualTo(ChannelId("http://host/cnn"))
    }

    @Test
    fun `skips an EXTINF with no following url`() {
        val channels =
            parse(
                """
                #EXTINF:-1,Orphan
                #EXTINF:-1,Good
                http://host/good
                """.trimIndent(),
            )
        assertThat(channels.map { it.name }).containsExactly("Good")
    }

    @Test
    fun `ignores blank lines and unrelated directives`() {
        val channels =
            parse(
                """
                #EXTM3U

                #EXTINF:-1,One
                #EXTGRP:Sports
                #EXTVLCOPT:network-caching=1000
                http://host/one
                """.trimIndent(),
            )
        assertThat(channels).hasSize(1)
        assertThat(channels.single().streamUrl).isEqualTo("http://host/one")
    }

    @Test
    fun `parses multiple channels in order`() {
        val channels =
            parse(
                """
                #EXTINF:-1,A
                http://host/a
                #EXTINF:-1,B
                http://host/b
                """.trimIndent(),
            )
        assertThat(channels.map { it.name }).containsExactly("A", "B").inOrder()
    }
}
