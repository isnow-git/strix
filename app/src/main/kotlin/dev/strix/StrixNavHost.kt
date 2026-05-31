package dev.strix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import dev.strix.core.ui.theme.StrixTheme
import dev.strix.feature.channels.ChannelsScreen
import dev.strix.feature.onboarding.OnboardingScreen
import dev.strix.feature.player.PlayerScreen
import dev.strix.feature.player.PlayerViewModel

/** App navigation: onboarding (first run) -> channel grid -> fullscreen player. */
@Composable
fun StrixNavHost() {
    StrixTheme {
        val rootViewModel: RootViewModel = hiltViewModel()
        val startRoute by rootViewModel.startRoute.collectAsStateWithLifecycle()

        // Wait until we know whether the user has onboarded before building the graph.
        val start = startRoute ?: return@StrixTheme Box(Modifier.fillMaxSize())

        val navController = rememberNavController()
        NavHost(navController = navController, startDestination = start) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onDone = {
                        navController.navigate(Routes.CHANNELS) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
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
