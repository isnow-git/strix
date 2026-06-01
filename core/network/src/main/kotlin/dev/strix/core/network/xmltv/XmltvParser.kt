package dev.strix.core.network.xmltv

import android.util.Xml
import dev.strix.core.common.epg.normalizeEpgId
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/** One parsed XMLTV programme. Channel id is already normalized. */
data class XmltvProgramme(
    val normChannelId: String,
    val startSec: Long,
    val stopSec: Long,
    val title: String,
)

/**
 * Streaming XMLTV parser (pull-based, O(1) memory). Only programmes whose channel
 * passes [keep] are emitted, so ingestion stays bounded to the user's channels
 * even though a guide holds thousands.
 */
class XmltvParser {
    /**
     * Parses [input] and calls [onProgramme] for each kept programme. The caller
     * is expected to batch writes; this never builds a list.
     */
    fun parse(
        input: InputStream,
        keep: (String) -> Boolean,
        onProgramme: (XmltvProgramme) -> Unit,
    ) {
        val parser = Xml.newPullParser()
        parser.setInput(input, null)

        var channel: String? = null
        var startSec = 0L
        var stopSec = 0L
        var title: String? = null
        var inTitle = false

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
                        }
                        TAG_TITLE -> if (channel != null) inTitle = true
                    }
                XmlPullParser.TEXT ->
                    if (inTitle && title == null) title = parser.text?.trim()
                XmlPullParser.END_TAG ->
                    when (parser.name) {
                        TAG_TITLE -> inTitle = false
                        TAG_PROGRAMME -> {
                            val ch = channel
                            val t = title
                            if (ch != null && !t.isNullOrBlank() && stopSec > startSec) {
                                onProgramme(XmltvProgramme(ch, startSec, stopSec, t))
                            }
                            channel = null
                        }
                    }
            }
            event = parser.next()
        }
    }

    // XMLTV time: "20260601181000 +0200" (or without offset → treated as UTC).
    private fun parseTime(value: String?): Long {
        val text = value?.trim().orEmpty()
        if (text.length < TS_DIGITS) return 0L
        return try {
            if (text.length > TS_DIGITS) {
                SimpleDateFormat("yyyyMMddHHmmss Z", Locale.US).parse(text)?.time?.div(1000) ?: 0L
            } else {
                SimpleDateFormat("yyyyMMddHHmmss", Locale.US)
                    .apply { timeZone = TimeZone.getTimeZone("UTC") }
                    .parse(text)
                    ?.time
                    ?.div(1000) ?: 0L
            }
        } catch (e: java.text.ParseException) {
            0L
        }
    }

    private companion object {
        const val TAG_PROGRAMME = "programme"
        const val TAG_TITLE = "title"
        const val TS_DIGITS = 14
    }
}
