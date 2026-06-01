@file:Suppress("MatchingDeclarationName") // groups the keypad state + overlay together

package dev.strix.feature.channels

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import kotlinx.coroutines.delay

/**
 * Remote-keypad channel entry, isolated from the rest of the screen.
 *
 * The screen feeds digits into [KeypadZapState] (its single mutable state), and
 * only this overlay reads it — so typing a number recomposes the on-screen digits,
 * not the channel list or header.
 *
 * Commit is deterministic: a number tunes the moment it can't grow into a longer
 * valid one (its last digit, or it already reached [MAX_KEYPAD_DIGITS]); otherwise
 * it waits a generous, fixed window for the next digit. No timing depends on how
 * fast the user types beyond that window.
 */
@Stable
class KeypadZapState {
    var input by mutableStateOf("")
        private set

    fun append(digit: Char) {
        if (input.length < MAX_KEYPAD_DIGITS) input += digit
    }

    fun clear() {
        input = ""
    }
}

@Composable
fun rememberKeypadZapState(): KeypadZapState = remember { KeypadZapState() }

/**
 * Renders the typed digits bottom-right and commits the number via [onCommit].
 * [maxNumber] is the highest channel number, used to know when a number is final.
 */
@Composable
fun BoxScope.KeypadOverlay(
    state: KeypadZapState,
    maxNumber: Int,
    onCommit: suspend (Int) -> Unit,
) {
    LaunchedEffect(state.input, maxNumber) {
        val text = state.input
        if (text.isEmpty()) return@LaunchedEffect
        val number =
            text.toIntOrNull() ?: run {
                state.clear()
                return@LaunchedEffect
            }
        val canGrow = text.length < MAX_KEYPAD_DIGITS && (maxNumber <= 0 || number.toLong() * 10 <= maxNumber)
        if (canGrow) delay(KEYPAD_COMMIT_MS)
        state.clear()
        onCommit(number)
    }

    if (state.input.isNotEmpty()) {
        Box(
            modifier = Modifier.align(Alignment.BottomEnd).padding(36.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .shadow(16.dp, RoundedCornerShape(16.dp), clip = false)
                        .background(NUMBER_OSD_BG, RoundedCornerShape(16.dp))
                        .padding(horizontal = 26.dp, vertical = 14.dp),
            ) {
                Text(text = state.input, color = Color.White, fontSize = 40.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Digits a channel number can have (catalogue can exceed 9999), and the wait for
// the next digit before tuning an as-yet-incomplete number.
const val MAX_KEYPAD_DIGITS = 5
private const val KEYPAD_COMMIT_MS = 1_800L
private val NUMBER_OSD_BG = Color(0xE60B0B0F)
