package dev.strix.core.common.epg

import java.text.Normalizer

/**
 * Normalizes an EPG/XMLTV channel id for matching across sources, whose ids
 * differ cosmetically (e.g. provider `TF1SeriesFilms.fr` vs XMLTV
 * `TF1.Séries-Films.fr`): lower-cased, accents removed, non-alphanumerics
 * dropped → both become `tf1seriesfilmsfr`.
 */
fun normalizeEpgId(id: String): String =
    Normalizer
        .normalize(id, Normalizer.Form.NFD)
        .replace(Regex("""\p{M}+"""), "")
        .lowercase()
        .replace(Regex("""[^a-z0-9]"""), "")
