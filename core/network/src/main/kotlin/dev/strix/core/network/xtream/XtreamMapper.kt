package dev.strix.core.network.xtream

import dev.strix.core.common.model.Channel
import dev.strix.core.common.model.ChannelId
import dev.strix.core.common.model.StreamSourceConfig

/** Normalizes a user-entered host into a scheme-qualified base URL with no trailing slash. */
internal fun normalizeXtreamBase(host: String): String {
    val trimmed = host.trim().trimEnd('/')
    return if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
        trimmed
    } else {
        "http://$trimmed"
    }
}

/** Builds the live `.ts` URL ExoPlayer plays for a given stream. */
internal fun xtreamLiveUrl(
    base: String,
    config: StreamSourceConfig.Xtream,
    streamId: Long,
): String = "$base/live/${config.username}/${config.password}/$streamId.ts"

/** Maps an API stream to a [Channel], or null when it has no usable name. */
internal fun XtreamStream.toChannel(
    base: String,
    config: StreamSourceConfig.Xtream,
    categoryNames: Map<String, String>,
): Channel? {
    val displayName = name?.takeIf { it.isNotBlank() } ?: return null
    return Channel(
        // Stable across refreshes; host-scoped so two providers can't collide.
        id = ChannelId("xtream:${config.host}:$streamId"),
        name = displayName,
        streamUrl = xtreamLiveUrl(base, config, streamId),
        logoUrl = streamIcon?.takeIf { it.isNotBlank() },
        group = categoryId?.let { categoryNames[it] }?.takeIf { it.isNotBlank() },
        number = number,
    )
}
