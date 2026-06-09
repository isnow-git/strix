package dev.strix

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dev.strix.core.designsystem.theme.StrixTheme
import dev.strix.feature.channels.ChannelsRoute
import dev.strix.feature.epg.EpgTimelineRoute
import dev.strix.feature.onboarding.OnboardingScreen

/** App navigation: onboarding (first run) -> channel home (which hosts its own player). */
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
                            launchSingleTop = true
                        }
                    },
                )
            }
            composable(Routes.CHANNELS) {
                ChannelsRoute(
                    onChangeSource = { navController.navigate(Routes.ONBOARDING) },
                    onOpenGuide = { navController.navigate(Routes.EPG) },
                )
            }
            composable(Routes.EPG) {
                EpgTimelineRoute(onBack = { navController.popBackStack() })
            }
        }
    }
}
