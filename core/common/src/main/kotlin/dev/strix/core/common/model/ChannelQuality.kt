package dev.strix.core.common.model

/** Parsed quality info for grouping a channel's variants. */
data class QualityInfo(
    /** Normalized, quality-stripped key shared by all variants of one channel. */
    val baseKey: String,
    /** Human label of this variant's quality (e.g. "FHD"), or null if unmarked. */
    val qualityLabel: String?,
    /** Sort rank, lower = better, used to pick the representative variant. */
    val qualityRank: Int,
)

/**
 * Splits a channel name into a quality label and a quality-independent base key,
 * so "BeIN Sports 1 FHD" and "BeIN Sports 1 HD" group under the same channel.
 *
 * Pure and allocation-light; runs once per channel at import.
 */
object ChannelQuality {
    const val RANK_NONE = 3

    private data class Token(
        val regex: Regex,
        val label: String,
        val rank: Int,
    )

    // Order matters: more specific / higher quality first.
    private val tokens =
        listOf(
            Token(Regex("""(?i)\b(4k|uhd|2160p)\b"""), "4K", 0),
            Token(Regex("""(?i)\b(fhd|fullhd|full hd|1080p?)\b"""), "FHD", 1),
            Token(Regex("""(?i)\b(hd|720p?)\b"""), "HD", 2),
            Token(Regex("""(?i)\b(sd|480p?|360p?)\b"""), "SD", 4),
            Token(Regex("""(?i)\b(lq|low)\b"""), "LQ", 5),
        )

    private val codecs = Regex("""(?i)\b(h265|hevc|h264|avc|ts|raw)\b""")
    private val brackets = Regex("""[\[\](){}|]""")
    private val spaces = Regex("""\s+""")
    private val nonAlphaNum = Regex("""[^a-z0-9]""")

    fun parse(name: String): QualityInfo {
        var label: String? = null
        var rank = RANK_NONE
        for (token in tokens) {
            if (token.regex.containsMatchIn(name)) {
                label = token.label
                rank = token.rank
                break
            }
        }

        var base = name
        for (token in tokens) base = token.regex.replace(base, " ")
        base = codecs.replace(base, " ")
        base = brackets.replace(base, " ")
        base = spaces.replace(base, " ").trim()

        val key = base.lowercase().replace(nonAlphaNum, "")
        return QualityInfo(
            baseKey = key.ifEmpty { name.lowercase().replace(nonAlphaNum, "") },
            qualityLabel = label,
            qualityRank = rank,
        )
    }
}
