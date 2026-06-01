package dev.strix.core.common.model

import java.text.Normalizer

/** A small, clean set of channel categories for a clutter-free browse rail. */
enum class ChannelCategory(
    val label: String,
) {
    Sport("Sport"),
    News("News"),
    Movies("Cinéma"),
    Series("Séries"),
    Kids("Enfants"),
    Music("Musique"),
    Docs("Docs"),
    Entertainment("Divertissement"),
    Adult("Adulte"),
    General("Autres"),
    ;

    companion object {
        // iptv-org category id -> our canonical bucket.
        private val iptvMap: Map<String, ChannelCategory> =
            mapOf(
                "sports" to Sport,
                "news" to News,
                "weather" to News,
                "movies" to Movies,
                "series" to Series,
                "kids" to Kids,
                "family" to Kids,
                "animation" to Kids,
                "music" to Music,
                "documentary" to Docs,
                "science" to Docs,
                "education" to Docs,
                "nature" to Docs,
                "xxx" to Adult,
                "entertainment" to Entertainment,
                "comedy" to Entertainment,
                "general" to Entertainment,
                "culture" to Entertainment,
                "lifestyle" to Entertainment,
                "cooking" to Entertainment,
                "travel" to Entertainment,
                "public" to Entertainment,
                "relax" to Entertainment,
            )

        // Most specific buckets win when a channel has several iptv-org categories.
        private val priority =
            listOf(Adult, Sport, News, Kids, Docs, Music, Movies, Series, Entertainment)

        /** Maps iptv-org categories to a canonical category, or null if none apply. */
        fun fromIptvOrg(categories: List<String>): ChannelCategory? {
            val mapped = categories.mapNotNull { iptvMap[it.lowercase()] }.toSet()
            return priority.firstOrNull { it in mapped }
        }
    }
}

/**
 * Classifies a channel into one canonical [ChannelCategory] from its name and the
 * provider's (messy) category, by keyword. Order matters: more specific buckets
 * are tested first. Pure and allocation-light; runs once per channel at import.
 */
object ChannelClassifier {
    private fun keywords(vararg words: String) = words.toList()

    // Tested in order; first match wins.
    private val rules: List<Pair<ChannelCategory, List<String>>> =
        listOf(
            ChannelCategory.Adult to keywords("xxx", "adult", "adulte", "porn", "18+"),
            ChannelCategory.Kids to
                keywords("kids", "enfant", "junior", "disney", "boomerang", "gulli", "cartoon", "nick", "baby"),
            ChannelCategory.Sport to
                keywords("sport", "bein", "foot", "rugby", "tennis", "ufc", "espn", "eurosport", "dazn", "match", "nba"),
            ChannelCategory.News to
                keywords("news", "info", "actu", "bfm", "lci", "cnn", "i24", "franceinfo", "euronews", "journal"),
            ChannelCategory.Docs to
                keywords("doc", "discovery", "natgeo", "national geo", "histoire", "history", "planete", "science", "nature"),
            ChannelCategory.Music to keywords("music", "musique", "mtv", "trace", "clubbing", "hits", "radio"),
            ChannelCategory.Movies to keywords("cine", "ciné", "film", "movie", "cinema", "ocs", "action", "thriller"),
            ChannelCategory.Series to keywords("serie", "série", "series", "tv show", "novela"),
            ChannelCategory.Entertainment to
                keywords("divertissement", "entertainment", "tf1", "m6", "w9", "tmc", "general", "généraliste", "tnt"),
        )

    fun classify(
        name: String,
        providerCategory: String?,
    ): ChannelCategory {
        val haystack = normalize("${providerCategory.orEmpty()} $name")
        return rules.firstOrNull { (_, words) -> words.any { haystack.contains(normalize(it)) } }
            ?.first
            ?: ChannelCategory.General
    }

    private fun normalize(value: String): String =
        Normalizer
            .normalize(value, Normalizer.Form.NFD)
            .replace(Regex("""\p{M}+"""), "")
            .lowercase()
}
