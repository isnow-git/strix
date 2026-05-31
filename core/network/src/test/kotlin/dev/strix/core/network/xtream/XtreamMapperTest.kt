package dev.strix.core.network.xtream

import com.google.common.truth.Truth.assertThat
import dev.strix.core.common.model.StreamSourceConfig
import kotlinx.serialization.json.Json
import org.junit.Test

class XtreamMapperTest {
    private val config = StreamSourceConfig.Xtream(host = "dinotv.online", username = "u", password = "p")
    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun `normalizes a bare host to an http base`() {
        assertThat(normalizeXtreamBase("dinotv.online/")).isEqualTo("http://dinotv.online")
        assertThat(normalizeXtreamBase("https://x.tv:8080")).isEqualTo("https://x.tv:8080")
    }

    @Test
    fun `builds the live ts url`() {
        val url = xtreamLiveUrl("http://dinotv.online", config, streamId = 42)
        assertThat(url).isEqualTo("http://dinotv.online/live/u/p/42.ts")
    }

    @Test
    fun `maps a stream with its category name`() {
        val stream = XtreamStream(streamId = 7, name = "BBC", streamIcon = "logo.png", categoryId = "3", number = 12)
        val channel = stream.toChannel("http://dinotv.online", config, mapOf("3" to "UK"))

        assertThat(channel).isNotNull()
        assertThat(channel!!.name).isEqualTo("BBC")
        assertThat(channel.streamUrl).isEqualTo("http://dinotv.online/live/u/p/7.ts")
        assertThat(channel.group).isEqualTo("UK")
        assertThat(channel.number).isEqualTo(12)
        assertThat(channel.id.value).isEqualTo("xtream:dinotv.online:7")
    }

    @Test
    fun `drops a stream without a usable name`() {
        val stream = XtreamStream(streamId = 9, name = "  ")
        assertThat(stream.toChannel("http://h", config, emptyMap())).isNull()
    }

    @Test
    fun `parses a get_live_streams payload ignoring unknown fields`() {
        val payload =
            """[{"num":1,"name":"A","stream_id":100,"stream_icon":"a.png",
               "category_id":"5","added":"123","extra":"ignored"}]"""
        val streams = json.decodeFromString<List<XtreamStream>>(payload)

        assertThat(streams).hasSize(1)
        assertThat(streams[0].streamId).isEqualTo(100)
        assertThat(streams[0].name).isEqualTo("A")
    }
}
