package dev.strix.core.common.result

/**
 * A success-or-failure result carrying a typed [StrixError] on failure.
 *
 * Preferred over `kotlin.Result` in the domain because the error side is a closed
 * [StrixError] taxonomy rather than an arbitrary `Throwable`, which keeps `when`
 * branches exhaustive and stops framework exceptions leaking across layers.
 *
 * The combinators are `inline` so mapping/folding on hot paths (channel lists,
 * zapping) adds no extra allocation or lambda dispatch.
 */
sealed interface StrixResult<out T> {
    data class Success<out T>(
        val value: T,
    ) : StrixResult<T>

    data class Failure(
        val error: StrixError,
    ) : StrixResult<Nothing>
}

/** True when this is a [StrixResult.Success]. */
val StrixResult<*>.isSuccess: Boolean
    get() = this is StrixResult.Success

/** The value on success, or `null` on failure. */
fun <T> StrixResult<T>.getOrNull(): T? =
    when (this) {
        is StrixResult.Success -> value
        is StrixResult.Failure -> null
    }

/** The error on failure, or `null` on success. */
fun StrixResult<*>.errorOrNull(): StrixError? =
    when (this) {
        is StrixResult.Success -> null
        is StrixResult.Failure -> error
    }

/** Transforms the success value, propagating failure unchanged. */
inline fun <T, R> StrixResult<T>.map(transform: (T) -> R): StrixResult<R> =
    when (this) {
        is StrixResult.Success -> StrixResult.Success(transform(value))
        is StrixResult.Failure -> this
    }

/** Chains another result-producing step, propagating failure unchanged. */
inline fun <T, R> StrixResult<T>.flatMap(transform: (T) -> StrixResult<R>): StrixResult<R> =
    when (this) {
        is StrixResult.Success -> transform(value)
        is StrixResult.Failure -> this
    }

/** Collapses both branches into a single value. */
inline fun <T, R> StrixResult<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (StrixError) -> R,
): R =
    when (this) {
        is StrixResult.Success -> onSuccess(value)
        is StrixResult.Failure -> onFailure(error)
    }

/** Returns the success value, or [fallback] computed from the error. */
inline fun <T> StrixResult<T>.getOrElse(fallback: (StrixError) -> T): T =
    when (this) {
        is StrixResult.Success -> value
        is StrixResult.Failure -> fallback(error)
    }

/** Wraps a value as a success. */
fun <T> T.asSuccess(): StrixResult<T> = StrixResult.Success(this)

/** Wraps an error as a failure. */
fun StrixError.asFailure(): StrixResult<Nothing> = StrixResult.Failure(this)
