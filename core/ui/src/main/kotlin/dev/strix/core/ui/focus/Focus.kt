package dev.strix.core.ui.focus

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.strix.core.ui.theme.StrixPalette

/**
 * Returns a [FocusRequester] that grabs focus on first composition. Use it to
 * land D-pad focus on a sensible default (first row, play button, …) when a
 * screen appears, so the remote is immediately useful.
 *
 * `requestFocus` is guarded because the target may not be attached yet on the
 * very first frame.
 */
@Composable
fun rememberInitialFocusRequester(): FocusRequester {
    val requester = remember { FocusRequester() }
    LaunchedEffect(requester) {
        runCatching { requester.requestFocus() }
    }
    return requester
}

/** Attaches [requester] to this element (sugar over [focusRequester]). */
fun Modifier.initialFocus(requester: FocusRequester): Modifier = focusRequester(requester)

/**
 * Draws a focus ring around the element while it holds D-pad focus. A cheap,
 * allocation-light way to make the current selection obvious from across the room
 * without per-item recomposition elsewhere.
 */
fun Modifier.focusRing(
    width: Dp = 3.dp,
    cornerRadius: Dp = 8.dp,
): Modifier =
    composed {
        var focused by remember { mutableStateOf(false) }
        this
            .onFocusChanged { focused = it.isFocused }
            .border(
                width = width,
                color = if (focused) StrixPalette.FocusRing else Color.Transparent,
                shape = RoundedCornerShape(cornerRadius),
            )
    }
