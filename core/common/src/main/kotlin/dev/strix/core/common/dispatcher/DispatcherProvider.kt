package dev.strix.core.common.dispatcher

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

/**
 * Abstraction over the coroutine dispatchers so use cases and repositories never
 * hard-code [Dispatchers]. Lets tests swap in a single test dispatcher and keeps
 * threading decisions injectable (DIP).
 */
interface DispatcherProvider {
    val main: CoroutineDispatcher
    val default: CoroutineDispatcher
    val io: CoroutineDispatcher
}

/** Production dispatchers. Bound via DI; replaced by a test provider in tests. */
class DefaultDispatcherProvider : DispatcherProvider {
    override val main: CoroutineDispatcher get() = Dispatchers.Main
    override val default: CoroutineDispatcher get() = Dispatchers.Default
    override val io: CoroutineDispatcher get() = Dispatchers.IO
}
