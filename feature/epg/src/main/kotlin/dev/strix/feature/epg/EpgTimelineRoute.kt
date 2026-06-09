package dev.strix.feature.epg

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel

/** Entry point for the program guide; hoists the ViewModel for the stateless screen. */
@Composable
fun EpgTimelineRoute(
    onBack: () -> Unit,
    viewModel: EpgTimelineViewModel = hiltViewModel(),
) {
    EpgTimelineScreen(viewModel = viewModel, onBack = onBack)
}
