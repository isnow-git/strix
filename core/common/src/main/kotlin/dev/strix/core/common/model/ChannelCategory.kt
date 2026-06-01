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
    Adult("Adulte"),
    General("Général"),
    ;

    companion object {
        // iptv-org category id -> our canonical bucket. Generalist/entertainment
        // buckets all fold into Général (kept deliberately broad).
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
                "entertainment" to General,
                "general" to General,
                "comedy" to General,
                "culture" to General,
                "lifestyle" to General,
                "cooking" to General,
                "travel" to General,
                "public" to General,
                "relax" to General,
            )

        // Most specific buckets win when a channel has several iptv-org categories.
        private val priority =
            listOf(Adult, Sport, News, Kids, Docs, Music, Movies, Series, General)

        /** Maps iptv-org categories to a canonical category, or null if none apply. */
        fun fromIptvOrg(categories: List<String>): ChannelCategory? {
            val mapped = categories.mapNotNull { iptvMap[it.lowercase()] }.toSet()
            return priority.firstOrNull { it in mapped }
        }
    }
}

// Compiled once; reused for every channel classified during an import.
private val diacritics = Regex("""\p{M}+""")

private fun foldDiacritics(value: String): String =
    Normalizer
        .normalize(value, Normalizer.Form.NFD)
        .replace(diacritics, "")
        .lowercase()

/**
 * Fallback classifier from name + provider category, used only when iptv-org has
 * no match. Keyword lists are kept tight (strong, unambiguous terms) so an
 * unknown channel lands in [ChannelCategory.General] rather than a wrong bucket.
 */
object ChannelClassifier {
    // Keywords are diacritics-folded once at class load, not per channel: the
    // rule table is constant, so normalizing it on every classify() call (once
    // per unmatched channel, i.e. most of them) was pure waste.
    private val rules: List<Pair<ChannelCategory, List<String>>> =
        listOf(
            ChannelCategory.Adult to listOf("xxx", "porn", "adult", "adulte", "18+"),
            ChannelCategory.Kids to
                listOf("disney junior", "boomerang", "gulli", "cartoon", "nickelodeon", "piwi", "tiji", "junior"),
            ChannelCategory.Sport to
                listOf(
                    "sport",
                    "bein",
                    "eurosport",
                    "espn",
                    "dazn",
                    "rmc sport",
                    "foot",
                    "rugby",
                    "nba",
                    "ufc",
                    "tennis",
                    "roland garros",
                    "wimbledon",
                    "golf",
                    "f1",
                    "motogp",
                ),
            ChannelCategory.News to
                listOf("bfm", "lci", "cnn", "franceinfo", "france info", "euronews", "i24", "news", "actualit"),
            ChannelCategory.Docs to
                listOf("discovery", "national geo", "natgeo", "histoire", "planete", "rmc decouverte", "ushuaia"),
            ChannelCategory.Music to listOf("mtv", "trace", "vevo", "clubbing", "stingray", "musique", "music "),
            ChannelCategory.Movies to listOf("cinema", "cine+", "cine +", "ocs", "tcm", "paramount", "action"),
            ChannelCategory.Series to listOf("series", "warner tv", "novela"),
        ).map { (category, words) -> category to words.map(::foldDiacritics) }

    fun classify(
        name: String,
        providerCategory: String?,
    ): ChannelCategory {
        val haystack = foldDiacritics("${providerCategory.orEmpty()} $name")
        return rules
            .firstOrNull { (_, words) -> words.any { haystack.contains(it) } }
            ?.first
            ?: ChannelCategory.General
    }
}
