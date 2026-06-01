package dev.strix.core.common.model

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ChannelQualityTest {
    @Test
    fun `variants of the same channel share a base key`() {
        val fhd = ChannelQuality.parse("BeIN Sports 1 FHD")
        val hd = ChannelQuality.parse("BeIN Sports 1 HD")
        val sd = ChannelQuality.parse("BeIN Sports 1 SD")

        assertThat(fhd.baseKey).isEqualTo("beinsports1")
        assertThat(hd.baseKey).isEqualTo(fhd.baseKey)
        assertThat(sd.baseKey).isEqualTo(fhd.baseKey)
    }

    @Test
    fun `quality labels and ranks are parsed`() {
        assertThat(ChannelQuality.parse("TF1 4K").qualityLabel).isEqualTo("4K")
        assertThat(ChannelQuality.parse("TF1 FHD").qualityLabel).isEqualTo("FHD")
        assertThat(ChannelQuality.parse("TF1 HD").qualityLabel).isEqualTo("HD")
        assertThat(ChannelQuality.parse("TF1 SD").qualityLabel).isEqualTo("SD")

        // 4K is a better (lower) rank than SD.
        assertThat(ChannelQuality.parse("TF1 4K").qualityRank)
            .isLessThan(ChannelQuality.parse("TF1 SD").qualityRank)
    }

    @Test
    fun `unmarked channel has no label and the neutral rank`() {
        val info = ChannelQuality.parse("Arte")
        assertThat(info.qualityLabel).isNull()
        assertThat(info.qualityRank).isEqualTo(ChannelQuality.RANK_NONE)
        assertThat(info.baseKey).isEqualTo("arte")
    }

    @Test
    fun `codec and bracket noise is stripped from the key`() {
        assertThat(ChannelQuality.parse("Canal+ [HEVC] FHD").baseKey)
            .isEqualTo(ChannelQuality.parse("Canal+ HD").baseKey)
    }

    @Test
    fun `time-shift is parsed and a bare plus is not a shift`() {
        assertThat(ChannelQuality.parse("FR - TF1 +1 FHD").timeshift).isEqualTo(1)
        assertThat(ChannelQuality.parse("TF1 +6h").timeshift).isEqualTo(6)
        assertThat(ChannelQuality.parse("TF1+ Star Academy").timeshift).isEqualTo(0)
        assertThat(ChannelQuality.parse("TF1 HD").timeshift).isEqualTo(0)
    }

    @Test
    fun `epg id groups variants and separates same-name different channels`() {
        val tf1Fhd = ChannelQuality.parse("FR - TF1 FHD")
        val tf1Uhd = ChannelQuality.parse("FR - TF1 4K")
        // Same epg id + same time-shift = same group.
        assertThat(ChannelQuality.groupKey("TF1.fr", tf1Fhd))
            .isEqualTo(ChannelQuality.groupKey("TF1.fr", tf1Uhd))
        // Same name but different epg id = different group (Discovery UK vs FR).
        val disco = ChannelQuality.parse("Discovery HD")
        assertThat(ChannelQuality.groupKey("discovery.uk", disco))
            .isNotEqualTo(ChannelQuality.groupKey("discovery.fr", disco))
    }

    @Test
    fun `a time-shifted feed never groups with the live one`() {
        val live = ChannelQuality.parse("TF1 FHD")
        val plus1 = ChannelQuality.parse("TF1 +1 FHD")
        assertThat(ChannelQuality.groupKey("TF1.fr", live))
            .isNotEqualTo(ChannelQuality.groupKey("TF1.fr", plus1))
    }

    @Test
    fun `display name strips country prefix, quality and junk`() {
        assertThat(ChannelQuality.displayName("FR - TF1 HEVC")).isEqualTo("TF1")
        assertThat(ChannelQuality.displayName("FR - TF1 FHD ◉")).isEqualTo("TF1")
        assertThat(ChannelQuality.displayName("BE | TF1 HD")).isEqualTo("TF1")
        assertThat(ChannelQuality.displayName("FR - TF1 FILMS & SÉRIES HEVC"))
            .isEqualTo("TF1 FILMS & SÉRIES")
    }

    @Test
    fun `display name keeps the time-shift marker`() {
        assertThat(ChannelQuality.displayName("FR - TF1 +1 FHD")).isEqualTo("TF1 +1")
    }

    @Test
    fun `clean category strips leading hashes`() {
        assertThat(ChannelQuality.cleanCategory("### FR SPORT")).isEqualTo("FR SPORT")
        assertThat(ChannelQuality.cleanCategory("##### Adultes")).isEqualTo("Adultes")
        assertThat(ChannelQuality.cleanCategory("News")).isEqualTo("News")
    }
}
