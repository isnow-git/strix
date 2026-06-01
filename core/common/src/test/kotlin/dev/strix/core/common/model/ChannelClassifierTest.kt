package dev.strix.core.common.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChannelClassifierTest {
    @Test
    fun `classifies common channels from name and provider category`() {
        assertThat(ChannelClassifier.classify("BeIN Sports 1", "FR| SPORT")).isEqualTo(ChannelCategory.Sport)
        assertThat(ChannelClassifier.classify("BFM TV", "Actu")).isEqualTo(ChannelCategory.News)
        assertThat(ChannelClassifier.classify("Canal+ Cinéma", null)).isEqualTo(ChannelCategory.Movies)
        assertThat(ChannelClassifier.classify("Disney Junior", "ENFANTS")).isEqualTo(ChannelCategory.Kids)
        assertThat(ChannelClassifier.classify("National Geographic", null)).isEqualTo(ChannelCategory.Docs)
        assertThat(ChannelClassifier.classify("MTV Hits", null)).isEqualTo(ChannelCategory.Music)
    }

    @Test
    fun `matching is accent and case insensitive`() {
        assertThat(ChannelClassifier.classify("CINE+ PREMIER", null)).isEqualTo(ChannelCategory.Movies)
    }

    @Test
    fun `unknown channel falls back to general`() {
        assertThat(ChannelClassifier.classify("Zzz Channel", null)).isEqualTo(ChannelCategory.General)
    }

    @Test
    fun `iptv-org categories map to canonical, most specific wins`() {
        assertThat(ChannelCategory.fromIptvOrg(listOf("entertainment"))).isEqualTo(ChannelCategory.Entertainment)
        assertThat(ChannelCategory.fromIptvOrg(listOf("general"))).isEqualTo(ChannelCategory.Entertainment)
        assertThat(ChannelCategory.fromIptvOrg(listOf("entertainment", "sports"))).isEqualTo(ChannelCategory.Sport)
        assertThat(ChannelCategory.fromIptvOrg(listOf("unknownX"))).isNull()
    }
}
