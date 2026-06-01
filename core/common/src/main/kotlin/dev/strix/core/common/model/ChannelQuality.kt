package dev.strix.core.common.model

/** Parsed quality info for grouping a channel's variants. */
data class QualityInfo(
    /** Normalized, quality-stripped key shared by all variants of one channel. */
    val baseKey: String,
    /** Human label of this variant's quality (e.g. "FHD"), or null if unmarked. */
    val qualityLabel: String?,
    /** Sort rank, lower = better, used to pick the representative variant. */
    val qualityRank: Int,
    /** Time-shift in hours (0 = live, >0 = delayed, -1 = delayed unknown amount). */
    val timeshift: Int,
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

    // Time-shift markers: "+1", "+6h", "+ 2 h" (a number after a plus).
    private val timeshiftNum = Regex("""\+\s?(\d{1,2})\s?h?\b""", RegexOption.IGNORE_CASE)

    // Worded delays with no explicit amount.
    private val timeshiftWord = Regex("""(?i)\b(differ[eé]|timeshift|rewind|replay)\b""")

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

        val shift =
            timeshiftNum.find(name)?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: if (timeshiftWord.containsMatchIn(name)) UNKNOWN_SHIFT else 0

        var base = name
        for (token in tokens) base = token.regex.replace(base, " ")
        base = timeshiftNum.replace(base, " ")
        base = timeshiftWord.replace(base, " ")
        base = codecs.replace(base, " ")
        base = brackets.replace(base, " ")
        base = spaces.replace(base, " ").trim()

        val key = base.lowercase().replace(nonAlphaNum, "")
        return QualityInfo(
            baseKey = key.ifEmpty { name.lowercase().replace(nonAlphaNum, "") },
            qualityLabel = label,
            qualityRank = rank,
            timeshift = shift,
        )
    }

    /**
     * The grouping key joining variants of the same channel: the provider EPG id
     * when present (strongest signal), else the normalized name; always split by
     * time-shift so a delayed feed never groups with the live one.
     */
    fun groupKey(
        epgChannelId: String?,
        info: QualityInfo,
    ): String {
        val base =
            epgChannelId?.trim()?.takeIf { it.isNotEmpty() }?.let { "e:${it.lowercase()}" }
                ?: "n:${info.baseKey}"
        return "$base|ts${info.timeshift}"
    }

    const val UNKNOWN_SHIFT = -1
}
