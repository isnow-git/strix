package dev.strix.core.model.epg

/** One programme in the guide. Times are Unix epoch seconds. */
data class EpgProgramme(
    val title: String,
    val startEpochSec: Long,
    val endEpochSec: Long,
    val description: String? = null,
)

/** The current and upcoming programme for a channel. */
data class NowNext(
    val current: EpgProgramme?,
    val next: EpgProgramme?,
)
