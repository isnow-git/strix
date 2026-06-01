package dev.strix.core.common.model

private val separatorPrefix = Regex("""^[#=*_~|]{2,}""")

/**
 * Whether an entry is a separator/placeholder rather than a real channel:
 * playlists often inject group headers like "### SPORT ###" or entries with no
 * stream. These must never appear in the channel list.
 */
fun isSeparatorChannel(
    name: String,
    streamUrl: String,
): Boolean {
    if (streamUrl.isBlank()) return true
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return true
    if (separatorPrefix.containsMatchIn(trimmed)) return true
    return trimmed.none { it.isLetterOrDigit() }
}
