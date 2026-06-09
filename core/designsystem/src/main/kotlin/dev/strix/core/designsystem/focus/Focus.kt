package dev.strix.core.designsystem.focus

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
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.strix.core.designsystem.theme.StrixPalette

/**
 * A [FocusRequester] that grabs focus on first composition. Use it to land D-pad
 * focus on a sensible default (first row, play button, …) when a screen appears, so
 * the remote is immediately useful. `requestFocus` is guarded because the target may
 * not be attached on the very first frame.
 */
@Composable
fun rememberInitialFocusRequester(): FocusRequester {
    val requester = remember { FocusRequester() }
    LaunchedEffect(requester) {
        runCatching { requester.requestFocus() }
    }
    return requester
}

/**
 * Draws a focus ring around the element while it holds D-pad focus. The focus flag is
 * owned locally, so only this element recomposes when focus moves — no per-item
 * recomposition rippling out to siblings.
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
