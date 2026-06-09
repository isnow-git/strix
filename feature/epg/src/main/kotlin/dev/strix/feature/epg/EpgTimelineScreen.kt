package dev.strix.feature.epg

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.itemKey
import androidx.tv.material3.Text
import dev.strix.core.designsystem.glass.glass
import dev.strix.core.designsystem.theme.StrixPalette
import dev.strix.core.model.Channel
import dev.strix.core.model.epg.EpgProgramme
import java.time.Instant
import java.time.ZoneId
import kotlin.math.roundToInt

/**
 * Program guide: channels down, clock across. Channels page in via the keyset source;
 * each row loads its programmes for the shared window and positions them proportionally
 * on the time axis. A primary-coloured line marks "now". D-pad scrolls the rows; Back exits.
 */
@Composable
fun EpgTimelineScreen(
    viewModel: EpgTimelineViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val channels = viewModel.channels.collectAsLazyPagingItems()
    val listState = rememberLazyListState()
    val firstRowFocus = remember { FocusRequester() }
    val startSec = viewModel.windowStartSec
    val durationSec = viewModel.windowDurationSec

    LaunchedEffect(channels.itemCount) {
        if (channels.itemCount > 0) runCatching { firstRowFocus.requestFocus() }
    }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(StrixPalette.BackgroundElevated, StrixPalette.Background)))
                .padding(horizontal = 40.dp, vertical = 28.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Back) {
                        onBack()
                        true
                    } else {
                        false
                    }
                },
    ) {
        Text(
            text = stringResource(R.string.epg_title),
            color = StrixPalette.OnBackground,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
        )

        Row(modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
            Spacer(Modifier.width(LABEL_WIDTH.dp))
            TimeAxis(startSec = startSec, durationSec = durationSec, modifier = Modifier.weight(1f))
        }

        LazyColumn(
            state = listState,
            verticalArrangement = Arrangement.spacedBy(ROW_SPACING.dp),
            contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(
                count = channels.itemCount,
                key = channels.itemKey { it.id.value },
                contentType = { "epg" },
            ) { index ->
                val channel = channels[index]
                if (channel == null) {
                    EpgRowSkeleton()
                } else {
                    val programmes by produceState(
                        initialValue = emptyList<EpgProgramme>(),
                        key1 = channel.id.value,
                    ) {
                        value = viewModel.timeline(channel)
                    }
                    EpgRow(
                        channel = channel,
                        programmes = programmes,
                        startSec = startSec,
                        durationSec = durationSec,
                        nowSec = viewModel.nowSec(),
                        focusRequester = if (index == 0) firstRowFocus else null,
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeAxis(
    startSec: Long,
    durationSec: Long,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier.fillMaxWidth().height(24.dp)) {
        val widthPx = constraints.maxWidth.toFloat()
        // One label per hour boundary across the window (skip the far edge).
        var tick = startSec
        while (tick < startSec + durationSec) {
            val fraction = (tick - startSec).toFloat() / durationSec
            val xPx = fraction * widthPx
            Text(
                text = formatHour(tick),
                color = StrixPalette.Muted,
                fontSize = 12.sp,
                modifier = Modifier.offset { IntOffset(xPx.roundToInt(), 0) },
            )
            tick += SECONDS_PER_HOUR
        }
    }
}

@Composable
private fun EpgRow(
    channel: Channel,
    programmes: List<EpgProgramme>,
    startSec: Long,
    durationSec: Long,
    nowSec: Long,
    focusRequester: FocusRequester?,
) {
    var focused by remember { mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier
                .fillMaxWidth()
                .height(ROW_HEIGHT.dp)
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .onFocusChanged { focused = it.isFocused }
                .focusable()
                .then(if (focused) Modifier.glass(RoundedCornerShape(10.dp)) else Modifier),
    ) {
        Column(modifier = Modifier.width(LABEL_WIDTH.dp).padding(horizontal = 12.dp)) {
            Text(channel.channelNumber.toString(), color = StrixPalette.Muted, fontSize = 12.sp, maxLines = 1)
            Text(
                channel.label,
                color = StrixPalette.OnBackground,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
            )
        }

        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxHeight().padding(vertical = 8.dp)) {
            val widthPx = constraints.maxWidth.toFloat()
            val density = LocalDensity.current
            if (programmes.isEmpty()) {
                Text(
                    text = stringResource(R.string.epg_no_guide),
                    color = StrixPalette.Muted,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.CenterStart).padding(start = 8.dp),
                )
            } else {
                programmes.forEach { programme ->
                    val startFraction = ((programme.startEpochSec - startSec).toFloat() / durationSec).coerceIn(0f, 1f)
                    val endFraction = ((programme.endEpochSec - startSec).toFloat() / durationSec).coerceIn(0f, 1f)
                    val blockWidthPx = (endFraction - startFraction) * widthPx
                    if (blockWidthPx > MIN_BLOCK_PX) {
                        val leftPx = startFraction * widthPx
                        Box(
                            modifier =
                                Modifier
                                    .offset { IntOffset(leftPx.roundToInt(), 0) }
                                    .width(with(density) { blockWidthPx.toDp() })
                                    .fillMaxHeight()
                                    .padding(end = 3.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(StrixPalette.SurfaceVariant)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                            contentAlignment = Alignment.CenterStart,
                        ) {
                            Text(programme.title, color = StrixPalette.OnBackground, fontSize = 12.sp, maxLines = 2)
                        }
                    }
                }
                val nowFraction = (nowSec - startSec).toFloat() / durationSec
                if (nowFraction in 0f..1f) {
                    Box(
                        modifier =
                            Modifier
                                .offset { IntOffset((nowFraction * widthPx).roundToInt(), 0) }
                                .width(2.dp)
                                .fillMaxHeight()
                                .background(StrixPalette.Primary),
                    )
                }
            }
        }
    }
}

@Composable
private fun EpgRowSkeleton() {
    Row(modifier = Modifier.fillMaxWidth().height(ROW_HEIGHT.dp), verticalAlignment = Alignment.CenterVertically) {
        Spacer(Modifier.width(LABEL_WIDTH.dp))
        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .height(44.dp)
                    .padding(end = 6.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(StrixPalette.LogoScrim),
        )
    }
}

private fun formatHour(epochSec: Long): String {
    val time = Instant.ofEpochSecond(epochSec).atZone(ZoneId.systemDefault()).toLocalTime()
    return "%02d:%02d".format(time.hour, time.minute)
}

private const val ROW_HEIGHT = 72
private const val ROW_SPACING = 6
private const val LABEL_WIDTH = 160
private const val MIN_BLOCK_PX = 6f
private const val SECONDS_PER_HOUR = 3600L
