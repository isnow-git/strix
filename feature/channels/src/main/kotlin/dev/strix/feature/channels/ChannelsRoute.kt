package dev.strix.feature.channels

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/**
 * Entry point for the channels feature: hoists the [ChannelsViewModel] and hands it to
 * the stateless [ChannelsScreen]. Kept thin so the screen stays easy to preview/test.
 */
@Composable
fun ChannelsRoute(
    onChangeSource: () -> Unit,
    onOpenGuide: () -> Unit,
    viewModel: ChannelsViewModel = hiltViewModel(),
) {
    ChannelsScreen(viewModel = viewModel, onChangeSource = onChangeSource, onOpenGuide = onOpenGuide)
}
