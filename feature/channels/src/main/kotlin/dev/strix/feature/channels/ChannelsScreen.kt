package dev.strix.feature.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import dev.strix.core.common.model.Channel
import dev.strix.core.ui.focus.focusRing
import dev.strix.core.ui.theme.StrixTheme
import kotlinx.coroutines.flow.collectLatest

/**
 * Channels grid with debounced FTS search. Focused-channel changes are forwarded
 * to the ViewModel; the debounced [ChannelsViewModel.playbackTarget] is collected
 * with `collectLatest` so a pending playback start is cancelled when the user
 * keeps zapping.
 */
@Composable
fun ChannelsScreen(
    onPlay: (Channel) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChannelsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val channels = viewModel.pagedChannels.collectAsLazyPagingItems()

    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.playbackTarget.collectLatest(onPlay)
    }

    StrixTheme {
        Column(modifier = modifier.fillMaxSize().padding(24.dp)) {
            SearchField(
                query = state.query,
                onQueryChange = { viewModel.onIntent(ChannelsIntent.SearchChanged(it)) },
            )
            state.errorMessage?.let { message ->
                Text(text = message, modifier = Modifier.padding(top = 8.dp))
            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(COLUMN_COUNT),
                modifier = Modifier.fillMaxSize().padding(top = 16.dp),
            ) {
                items(
                    count = channels.itemCount,
                    key = channels.itemKey { it.id.value },
                    contentType = channels.itemContentType { "channel" },
                ) { index ->
                    val channel = channels[index] ?: return@items
                    ChannelCard(
                        channel = channel,
                        onFocused = { viewModel.onIntent(ChannelsIntent.ChannelFocused(channel)) },
                        onClick = { onPlay(channel) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 18.sp),
        cursorBrush =
            androidx.compose.ui.graphics
                .SolidColor(Color.White),
        modifier =
            Modifier
                .fillMaxWidth()
                .focusRing()
                .background(Color(0xFF22222C), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (query.isEmpty()) {
                Text(text = "Search channels", color = Color(0xFFB6B6C2))
            }
            inner()
        },
    )
}

@Composable
private fun ChannelCard(
    channel: Channel,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .padding(8.dp)
                .width(CARD_WIDTH.dp)
                .focusRing()
                .onFocusChanged { if (it.isFocused) onFocused() },
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            AsyncImage(
                model = channel.logoUrl,
                contentDescription = channel.name,
                modifier = Modifier.fillMaxWidth().height(LOGO_HEIGHT.dp),
            )
            Text(
                text = channel.name,
                maxLines = 1,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

private const val COLUMN_COUNT = 4
private const val CARD_WIDTH = 180
private const val LOGO_HEIGHT = 100
