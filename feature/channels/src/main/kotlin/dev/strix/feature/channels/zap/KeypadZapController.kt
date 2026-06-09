package dev.strix.feature.channels.zap

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Deterministic remote-keypad channel entry.
 *
 * This is the fix for the old zap bugs:
 *
 * 1. **"Had to retype the number several times."** The old overlay cleared its input
 *    and then ran the (suspending) commit inside a `LaunchedEffect` keyed on that very
 *    input — clearing it changed the key and cancelled the commit mid-flight, so the
 *    channel opened only sometimes. Here the input is plain state and [commit] simply
 *    calls [onCommit]; nothing the UI does can cancel a commit.
 * 2. **"Can't take your time on long numbers."** Commit is driven by a single generous
 *    idle window ([idleCommitMs]) that every keypress resets, plus an explicit
 *    [commitNow] (OK button) and a [backspace] — never by guessing the number is final
 *    the instant it can't grow. Only a full [maxDigits] entry auto-commits (it cannot
 *    grow further).
 *
 * All timing lives on the injected [scope] (the ViewModel scope in production, a test
 * scope in tests), so behaviour is fully controllable and unit-testable.
 */
class KeypadZapController(
    private val scope: CoroutineScope,
    private val maxDigits: Int = MAX_KEYPAD_DIGITS,
    private val idleCommitMs: Long = IDLE_COMMIT_MS,
    private val onCommit: (Int) -> Unit,
) {
    private val mutableInput = MutableStateFlow("")

    /** The digits typed so far. The overlay renders this; only it reads the value. */
    val input: StateFlow<String> = mutableInput.asStateFlow()

    private var idleJob: Job? = null

    /** Appends a digit. Auto-commits only when the input reaches [maxDigits]. */
    fun append(digit: Char) {
        if (!digit.isDigit() || mutableInput.value.length >= maxDigits) return
        mutableInput.value += digit
        if (mutableInput.value.length >= maxDigits) commit() else scheduleIdle()
    }

    /** Removes the last digit (remote "back"/"del" while typing). */
    fun backspace() {
        val current = mutableInput.value
        if (current.isEmpty()) return
        mutableInput.value = current.dropLast(1)
        if (mutableInput.value.isEmpty()) cancelIdle() else scheduleIdle()
    }

    /** Commits the typed number immediately (remote OK / center). */
    fun commitNow() {
        if (mutableInput.value.isNotEmpty()) commit()
    }

    /** Discards the current input without committing. */
    fun clear() {
        mutableInput.value = ""
        cancelIdle()
    }

    private fun scheduleIdle() {
        cancelIdle()
        idleJob =
            scope.launch {
                delay(idleCommitMs)
                commit()
            }
    }

    private fun cancelIdle() {
        idleJob?.cancel()
        idleJob = null
    }

    private fun commit() {
        val number = mutableInput.value.toIntOrNull()
        mutableInput.value = ""
        cancelIdle()
        if (number != null) onCommit(number)
    }

    companion object {
        /** A catalogue can exceed 9999, so allow five digits. */
        const val MAX_KEYPAD_DIGITS = 5

        /** Generous wait for the next digit before tuning an incomplete number. */
        const val IDLE_COMMIT_MS = 2_500L
    }
}
