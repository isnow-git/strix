package dev.strix.core.network.playlist

import dev.strix.core.common.model.Channel
import dev.strix.core.common.model.ChannelId

/**
 * Streaming M3U/M3U8 parser.
 *
 * Consumes the playlist **line by line** and yields one [Channel] at a time as a
 * lazy [Sequence], so an arbitrarily large playlist is never materialised in RAM
 * (O(1) memory — a hard requirement on low-RAM TVs). The caller drives the
 * sequence and writes channels to Room in batches.
 *
 * Expected shape:
 * ```
 * #EXTM3U
 * #EXTINF:-1 tvg-id="bbc1" tvg-logo="..." group-title="UK",BBC One
 * http://host/stream/bbc1
 * ```
 */
class M3uParser {
    private val attribute = Regex("""([A-Za-z0-9_-]+)="([^"]*)"""")

    /**
     * Lazily parses [lines] into channels. Malformed entries (an `#EXTINF`
     * without a following URL, or a URL without metadata) are skipped rather
     * than aborting the whole import.
     */
    fun parse(lines: Sequence<String>): Sequence<Channel> =
        sequence {
            var pending: ExtInf? = null
            for (rawLine in lines) {
                val line = rawLine.trim()
                when {
                    line.isEmpty() -> Unit
                    line.startsWith("#EXTM3U") -> Unit
                    line.startsWith("#EXTINF") -> pending = parseExtInf(line)
                    line.startsWith("#") -> Unit // other directives (#EXTGRP, #EXTVLCOPT, …)
                    else -> {
                        val info = pending
                        if (info != null) {
                            yield(info.toChannel(streamUrl = line))
                            pending = null
                        }
                    }
                }
            }
        }

    private fun parseExtInf(line: String): ExtInf {
        val attrs = attribute.findAll(line).associate { it.groupValues[1] to it.groupValues[2] }
        // Display name is the text after the last comma on the line.
        val displayName = line.substringAfterLast(',', missingDelimiterValue = "").trim()
        return ExtInf(
            attrs = attrs,
            displayName = displayName,
        )
    }

    private data class ExtInf(
        val attrs: Map<String, String>,
        val displayName: String,
    ) {
        fun toChannel(streamUrl: String): Channel {
            val tvgName = attrs["tvg-name"]?.takeIf { it.isNotBlank() }
            val name = displayName.takeIf { it.isNotBlank() } ?: tvgName ?: streamUrl
            // Stable id: prefer the provider's tvg-id, else the URL (always unique).
            val id = attrs["tvg-id"]?.takeIf { it.isNotBlank() } ?: streamUrl
            return Channel(
                id = ChannelId(id),
                name = name,
                streamUrl = streamUrl,
                logoUrl = attrs["tvg-logo"]?.takeIf { it.isNotBlank() },
                group = attrs["group-title"]?.takeIf { it.isNotBlank() },
                number = attrs["tvg-chno"]?.toIntOrNull(),
            )
        }
    }
}
