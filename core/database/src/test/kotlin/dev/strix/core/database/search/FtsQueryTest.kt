package dev.strix.core.database.search

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class FtsQueryTest {
    @Test
    fun `single token becomes a prefix term`() {
        assertThat(FtsQuery.prefixMatch("bbc")).isEqualTo("bbc*")
    }

    @Test
    fun `multiple tokens are anded as prefixes`() {
        assertThat(FtsQuery.prefixMatch("BBC News")).isEqualTo("BBC* News*")
    }

    @Test
    fun `extra whitespace is collapsed`() {
        assertThat(FtsQuery.prefixMatch("  bbc   ne ")).isEqualTo("bbc* ne*")
    }

    @Test
    fun `blank input yields null`() {
        assertThat(FtsQuery.prefixMatch("")).isNull()
        assertThat(FtsQuery.prefixMatch("   ")).isNull()
    }

    @Test
    fun `fts special characters are stripped, never forming operators`() {
        // *, ", -, (), :, ^ must not survive as operators.
        assertThat(FtsQuery.prefixMatch("""news-24 "live"""")).isEqualTo("news* 24* live*")
        assertThat(FtsQuery.prefixMatch("a*b:c")).isEqualTo("a* b* c*")
    }

    @Test
    fun `input of only special characters yields null`() {
        assertThat(FtsQuery.prefixMatch("*\"-()")).isNull()
    }
}
