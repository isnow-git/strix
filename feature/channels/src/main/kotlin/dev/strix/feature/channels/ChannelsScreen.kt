package dev.strix.feature.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.LoadState
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemContentType
import androidx.paging.compose.itemKey
import androidx.tv.material3.Button
import androidx.tv.material3.ClickableSurfaceDefaults
import androidx.tv.material3.Surface
import androidx.tv.material3.Text
import coil.ImageLoader
import coil.compose.AsyncImage
import dev.strix.core.common.model.Channel
import dev.strix.core.ui.focus.focusRing
import dev.strix.core.ui.image.rememberStrixImageLoader
import dev.strix.core.ui.theme.StrixTheme

/**
 * Channel browser: a vertical list (one channel per row, like commercial TV
 * players) backed by paging, with debounced FTS search and a "change source"
 * action.
 *
 * Scrolling stays smooth on low-RAM TVs because rows are **fixed-height** (the
 * list skips re-measuring) and each logo loads exactly once: Coil caches it in
 * memory and on disk, so a logo that scrolls off and back is a cache hit, never
 * a re-fetch.
 */
@Composable
fun ChannelsScreen(
    onPlay: (Channel) -> Unit,
    onChangeSource: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChannelsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val channels = viewModel.pagedChannels.collectAsLazyPagingItems()
    val imageLoader = rememberStrixImageLoader()

    StrixTheme {
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .background(BACKGROUND)
                    .padding(horizontal = 32.dp, vertical = 24.dp),
        ) {
            Header(
                query = state.query,
                count = channels.itemCount,
                onQueryChange = { viewModel.onIntent(ChannelsIntent.SearchChanged(it)) },
                onChangeSource = onChangeSource,
            )
            if (categories.isNotEmpty()) {
                CategoryRail(
                    categories = categories,
                    selected = state.selectedCategory,
                    onSelect = { viewModel.onIntent(ChannelsIntent.CategorySelected(it)) },
                )
            }
            state.errorMessage?.let { message ->
                Text(text = message, color = ERROR_RED, modifier = Modifier.padding(top = 12.dp))
            }

            val refreshing = channels.loadState.refresh is LoadState.Loading
            when {
                channels.itemCount == 0 && refreshing -> CenterMessage("Chargement…")
                channels.itemCount == 0 -> CenterMessage("Aucune chaîne. Change la source pour en importer.")
                else ->
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize().padding(top = 20.dp),
                    ) {
                        items(
                            count = channels.itemCount,
                            key = channels.itemKey { it.id.value },
                            contentType = channels.itemContentType { "channel" },
                        ) { index ->
                            val channel = channels[index] ?: return@items
                            ChannelRow(
                                channel = channel,
                                imageLoader = imageLoader,
                                onFocused = { viewModel.onIntent(ChannelsIntent.ChannelFocused(channel)) },
                                onClick = { onPlay(channel) },
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun Header(
    query: String,
    count: Int,
    onQueryChange: (String) -> Unit,
    onChangeSource: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column {
            Text(text = "Strix", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            if (count > 0) {
                Text(text = "$count chaînes", color = MUTED, fontSize = 13.sp)
            }
        }
        SearchField(
            query = query,
            onQueryChange = onQueryChange,
            modifier = Modifier.weight(1f),
        )
        Button(onClick = onChangeSource) {
            Text(text = "Changer la source")
        }
    }
}

@Composable
private fun CategoryRail(
    categories: List<String>,
    selected: String?,
    onSelect: (String?) -> Unit,
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
    ) {
        item {
            CategoryChip(label = "Toutes", selected = selected == null, onClick = { onSelect(null) })
        }
        items(categories) { category ->
            CategoryChip(
                label = category,
                selected = selected == category,
                onClick = { onSelect(category) },
            )
        }
    }
}

@Composable
private fun CategoryChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val container = if (selected) PRIMARY else SURFACE
    Surface(
        onClick = onClick,
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        shape = ClickableSurfaceDefaults.shape(RoundedCornerShape(20.dp)),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = container,
                focusedContainerColor = container,
                pressedContainerColor = container,
            ),
        modifier = Modifier.focusRing(cornerRadius = 20.dp),
    ) {
        Text(
            text = label,
            color = Color.White,
            fontSize = 13.sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
}

@Composable
private fun CenterMessage(text: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = text, color = MUTED, fontSize = 16.sp)
    }
}

@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
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
            modifier
                .focusRing()
                .background(SURFACE, RoundedCornerShape(10.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
        decorationBox = { inner ->
            if (query.isEmpty()) {
                Text(text = "Rechercher une chaîne", color = MUTED)
            }
            inner()
        },
    )
}

@Composable
private fun ChannelRow(
    channel: Channel,
    imageLoader: ImageLoader,
    onFocused: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        // Outline-only focus: disable the tv Surface's default focus zoom and
        // keep the container transparent so only the focusRing border reacts.
        scale = ClickableSurfaceDefaults.scale(focusedScale = 1f),
        colors =
            ClickableSurfaceDefaults.colors(
                containerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                pressedContainerColor = Color.Transparent,
            ),
        modifier =
            Modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT.dp)
                .focusRing()
                .onFocusChanged { if (it.isFocused) onFocused() },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
        ) {
            LogoBox(channel = channel, imageLoader = imageLoader)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = channel.displayName.ifBlank { channel.name },
                    color = Color.White,
                    fontSize = 16.sp,
                    maxLines = 1,
                )
                channel.group?.let { group ->
                    Text(text = group, color = MUTED, fontSize = 12.sp, maxLines = 1)
                }
            }
            channel.number?.let { number ->
                Text(text = "$number", color = MUTED, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun LogoBox(
    channel: Channel,
    imageLoader: ImageLoader,
) {
    Box(
        modifier =
            Modifier
                .size(LOGO_SIZE.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(LOGO_BG),
        contentAlignment = Alignment.Center,
    ) {
        if (channel.logoUrl != null) {
            AsyncImage(
                model = channel.logoUrl,
                imageLoader = imageLoader,
                contentDescription = channel.name,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize().padding(6.dp),
            )
        } else {
            Text(
                text = channel.displayName.ifBlank { channel.name }.take(1).uppercase(),
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

private const val ROW_HEIGHT = 64
private const val LOGO_SIZE = 52

private val BACKGROUND = Color(0xFF0E0E14)
private val SURFACE = Color(0xFF22222C)
private val PRIMARY = Color(0xFF6C8CFF)
private val LOGO_BG = Color(0xFF1A1A22)
private val MUTED = Color(0xFFB6B6C2)
private val ERROR_RED = Color(0xFFFF6B6B)
