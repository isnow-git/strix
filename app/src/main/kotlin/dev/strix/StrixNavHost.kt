package dev.strix

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.strix.core.ui.theme.StrixTheme
import dev.strix.feature.channels.ChannelsScreen
import dev.strix.feature.player.PlayerScreen
import dev.strix.feature.player.PlayerViewModel

private object Routes {
    const val CHANNELS = "channels"
    const val PLAYER = "player/{${PlayerViewModel.ARG_CHANNEL_ID}}"

    fun player(channelId: String) = "player/${Uri.encode(channelId)}"
}

/** App navigation: channel grid -> fullscreen player. */
@Composable
fun StrixNavHost() {
    StrixTheme {
        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = Routes.CHANNELS) {
            composable(Routes.CHANNELS) {
                ChannelsScreen(
                    onPlay = { channel -> navController.navigate(Routes.player(channel.id.value)) },
                )
            }
            composable(
                route = Routes.PLAYER,
                arguments =
                    listOf(
                        navArgument(PlayerViewModel.ARG_CHANNEL_ID) { type = NavType.StringType },
                    ),
            ) {
                PlayerScreen()
            }
        }
    }
}
