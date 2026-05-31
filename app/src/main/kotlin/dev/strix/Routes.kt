package dev.strix

import android.net.Uri
import dev.strix.feature.player.PlayerViewModel

/** App route definitions. */
internal object Routes {
    const val ONBOARDING = "onboarding"
    const val CHANNELS = "channels"
    const val PLAYER = "player/{${PlayerViewModel.ARG_CHANNEL_ID}}"

    fun player(channelId: String) = "player/${Uri.encode(channelId)}"
}
