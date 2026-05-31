package dev.strix.core.database.search

/**
 * Turns raw user input into a safe FTS4 MATCH expression for instant prefix
 * search. Each whitespace-separated token becomes a prefix term (`tok*`) and the
 * tokens are AND-ed, so "bbc ne" matches "BBC News".
 *
 * FTS syntax characters are stripped so user input can never form an operator
 * (and never throws a malformed-MATCH error at query time).
 */
object FtsQuery {
    // Characters that carry meaning in an FTS4 MATCH expression.
    private val ftsSpecials = Regex("""["*():^\-]""")

    /**
     * @return a MATCH expression, or `null` when [raw] has no searchable token
     * (callers should then fall back to the unfiltered list).
     */
    fun prefixMatch(raw: String): String? {
        val tokens =
            raw
                .replace(ftsSpecials, " ")
                .split(' ', '\t', '\n')
                .filter { it.isNotBlank() }

        if (tokens.isEmpty()) return null
        return tokens.joinToString(separator = " ") { "$it*" }
    }
}
