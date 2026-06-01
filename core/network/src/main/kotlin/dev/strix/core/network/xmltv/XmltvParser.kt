package dev.strix.core.network.xmltv

import android.util.Xml
import dev.strix.core.common.epg.normalizeEpgId
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.time.DateTimeException
import java.time.LocalDateTime
import java.time.ZoneOffset

/** One parsed XMLTV programme. Channel id is already normalized. */
data class XmltvProgramme(
    val normChannelId: String,
    val startSec: Long,
    val stopSec: Long,
    val title: String,
    val description: String?,
)

/**
 * Streaming XMLTV parser (pull-based, O(1) memory). Only programmes whose channel
 * passes [keep] are emitted, so ingestion stays bounded to the user's channels
 * even though a guide holds thousands.
 */
class XmltvParser {
    /**
     * Parses [input] and calls [onProgramme] for each kept programme. The callback
     * is `suspend` so the caller can flush each batch straight to storage from
     * inside the stream, keeping ingestion O(batch) instead of buffering the whole
     * guide. This never builds a list itself.
     */
    @Suppress("CyclomaticComplexMethod", "NestedBlockDepth") // irreducible pull-parser state machine
    suspend fun parse(
        input: InputStream,
        keep: (String) -> Boolean,
        onProgramme: suspend (XmltvProgramme) -> Unit,
    ) {
        val parser = Xml.newPullParser()
        parser.setInput(input, null)

        var channel: String? = null
        var startSec = 0L
        var stopSec = 0L
        var title: String? = null
        var description: String? = null
        var inTitle = false
        var inDesc = false

        var event = parser.eventType
        while (event != XmlPullParser.END_DOCUMENT) {
            when (event) {
                XmlPullParser.START_TAG ->
                    when (parser.name) {
                        TAG_PROGRAMME -> {
                            val norm = normalizeEpgId(parser.getAttributeValue(null, "channel") ?: "")
                            channel = norm.takeIf { it.isNotEmpty() && keep(it) }
                            startSec = parseTime(parser.getAttributeValue(null, "start"))
                            stopSec = parseTime(parser.getAttributeValue(null, "stop"))
                            title = null
                            description = null
                        }
                        TAG_TITLE -> if (channel != null) inTitle = true
                        TAG_DESC -> if (channel != null) inDesc = true
                    }
                XmlPullParser.TEXT -> {
                    if (inTitle && title == null) title = parser.text?.trim()
                    if (inDesc && description == null) description = parser.text?.trim()
                }
                XmlPullParser.END_TAG ->
                    when (parser.name) {
                        TAG_TITLE -> inTitle = false
                        TAG_DESC -> inDesc = false
                        TAG_PROGRAMME -> {
                            val ch = channel
                            val t = title
                            if (ch != null && !t.isNullOrBlank() && stopSec > startSec) {
                                onProgramme(XmltvProgramme(ch, startSec, stopSec, t, description))
                            }
                            channel = null
                        }
                    }
            }
            event = parser.next()
        }
    }

    // XMLTV time: "20260601181000 +0200" (or without offset → treated as UTC).
    // Parsed by hand straight to epoch seconds: this runs twice per programme
    // (tens of thousands of times on a full guide), so a SimpleDateFormat per call
    // — which builds a Calendar and re-parses its pattern every time — would be the
    // hot spot. No substring/format allocation here.
    private fun parseTime(value: String?): Long {
        val text = value ?: return 0L
        var i = 0
        while (i < text.length && text[i] == ' ') i++
        val f = parseDateTimeFields(text, i) ?: return 0L
        return try {
            LocalDateTime
                .of(f[YEAR], f[MONTH], f[DAY], f[HOUR], f[MINUTE], f[SECOND])
                .toEpochSecond(ZoneOffset.ofTotalSeconds(zoneOffsetSeconds(text, i + TS_DIGITS)))
        } catch (ignored: DateTimeException) {
            0L // out-of-range field or offset → treat as unknown time
        }
    }

    /** Parses the 14 contiguous digits "yyyyMMddHHmmss" from [from] into fields, or null. */
    private fun parseDateTimeFields(
        text: String,
        from: Int,
    ): IntArray? {
        val out = IntArray(FIELD_WIDTHS.size)
        var pos = from
        for (k in FIELD_WIDTHS.indices) {
            out[k] = parseDigits(text, pos, FIELD_WIDTHS[k]) ?: return null
            pos += FIELD_WIDTHS[k]
        }
        return out
    }

    /** Parses an optional XMLTV "[ ]±HHMM" timezone offset to seconds; 0 (UTC) if absent. */
    private fun zoneOffsetSeconds(
        text: String,
        from: Int,
    ): Int {
        var i = from
        while (i < text.length && text[i] == ' ') i++
        if (i >= text.length || (text[i] != '+' && text[i] != '-')) return 0
        val sign = if (text[i] == '-') -1 else 1
        val offHours = parseDigits(text, i + 1, 2) ?: 0
        val offMins = parseDigits(text, i + 3, 2) ?: 0
        return sign * (offHours * SECONDS_PER_HOUR + offMins * SECONDS_PER_MINUTE)
    }

    /** Parses [len] digits of [s] from [start], or null if out of range / non-digit. */
    private fun parseDigits(
        s: String,
        start: Int,
        len: Int,
    ): Int? {
        if (start + len > s.length) return null
        var acc = 0
        for (k in start until start + len) {
            val c = s[k]
            if (c < '0' || c > '9') return null
            acc = acc * 10 + (c - '0')
        }
        return acc
    }

    private companion object {
        const val TAG_PROGRAMME = "programme"
        const val TAG_TITLE = "title"
        const val TAG_DESC = "desc"
        const val TS_DIGITS = 14
        const val SECONDS_PER_HOUR = 3600
        const val SECONDS_PER_MINUTE = 60

        // "yyyyMMddHHmmss" field widths and their indices in the parsed array.
        val FIELD_WIDTHS = intArrayOf(4, 2, 2, 2, 2, 2)
        const val YEAR = 0
        const val MONTH = 1
        const val DAY = 2
        const val HOUR = 3
        const val MINUTE = 4
        const val SECOND = 5
    }
}
