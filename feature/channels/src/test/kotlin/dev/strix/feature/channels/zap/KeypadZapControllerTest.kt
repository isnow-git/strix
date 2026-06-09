package dev.strix.feature.channels.zap

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test

/**
 * Proves the keypad is deterministic — the property the old timer-and-LaunchedEffect
 * implementation lacked (it opened the channel "one time in two").
 */
@OptIn(ExperimentalCoroutinesApi::class)
class KeypadZapControllerTest {
    @Test
    fun `OK commits the typed number immediately, exactly once`() =
        runTest {
            val committed = mutableListOf<Int>()
            val controller = KeypadZapController(backgroundScope, onCommit = { committed += it })

            controller.append('1')
            controller.append('2')
            controller.append('3')
            assertThat(controller.input.value).isEqualTo("123")

            controller.commitNow()
            advanceUntilIdle() // the pending idle timer must NOT fire a second commit

            assertThat(committed).containsExactly(123)
            assertThat(controller.input.value).isEmpty()
        }

    @Test
    fun `commits after the idle window elapses`() =
        runTest {
            val committed = mutableListOf<Int>()
            val controller =
                KeypadZapController(backgroundScope, idleCommitMs = 2_000, onCommit = { committed += it })

            controller.append('4')
            controller.append('2')

            advanceTimeBy(1_999)
            runCurrent()
            assertThat(committed).isEmpty()

            advanceTimeBy(2)
            runCurrent()
            assertThat(committed).containsExactly(42)
        }

    @Test
    fun `every keypress resets the idle window so long numbers can be typed slowly`() =
        runTest {
            val committed = mutableListOf<Int>()
            val controller =
                KeypadZapController(backgroundScope, idleCommitMs = 1_000, onCommit = { committed += it })

            // Four digits, each typed almost a full window apart: nothing must commit early.
            controller.append('1')
            advanceTimeBy(900)
            controller.append('2')
            advanceTimeBy(900)
            controller.append('3')
            advanceTimeBy(900)
            controller.append('4')
            advanceTimeBy(900)
            runCurrent()
            assertThat(committed).isEmpty()

            advanceTimeBy(101)
            runCurrent()
            assertThat(committed).containsExactly(1234)
        }

    @Test
    fun `a full-length number auto-commits`() =
        runTest {
            val committed = mutableListOf<Int>()
            val controller = KeypadZapController(backgroundScope, onCommit = { committed += it })

            "12345".forEach { controller.append(it) }
            runCurrent()

            assertThat(committed).containsExactly(12345)
            assertThat(controller.input.value).isEmpty()
        }

    @Test
    fun `backspace removes the last digit`() =
        runTest {
            val controller = KeypadZapController(backgroundScope, onCommit = {})

            controller.append('1')
            controller.append('2')
            controller.backspace()

            assertThat(controller.input.value).isEqualTo("1")
        }
}
