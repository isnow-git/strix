package dev.strix.core.model.epg

import java.text.Normalizer

// Compiled once: this runs per XMLTV programme during a guide ingest (tens of
// thousands of calls), so building a fresh Regex per call would dominate the cost.
private val combiningMarks = Regex("""\p{M}+""")
private val nonAlphaNum = Regex("""[^a-z0-9]""")

/**
 * Normalizes an EPG/XMLTV channel id for matching across sources whose ids differ
 * cosmetically (e.g. provider `TF1SeriesFilms.fr` vs XMLTV `TF1.Séries-Films.fr`):
 * lower-cased, accents removed, non-alphanumerics dropped → both become
 * `tf1seriesfilmsfr`.
 */
fun normalizeEpgId(id: String): String =
    Normalizer
        .normalize(id, Normalizer.Form.NFD)
        .replace(combiningMarks, "")
        .lowercase()
        .replace(nonAlphaNum, "")
