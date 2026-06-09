package dev.strix.core.common.result

/**
 * Domain-level error taxonomy. Keeps the presentation and data layers from leaking
 * framework exceptions (OkHttp, SQLite, …) across the [StrixResult] boundary. Map
 * low-level throwables to one of these at the data edge.
 */
sealed interface StrixError {
    val message: String?

    /** Connectivity failure: no route, refused, TLS, etc. */
    data class Network(
        override val message: String? = null,
        val cause: Throwable? = null,
    ) : StrixError

    /** A request exceeded its deadline. */
    data class Timeout(
        override val message: String? = null,
    ) : StrixError

    /** Malformed playlist / EPG / provider payload. */
    data class Parsing(
        override val message: String? = null,
    ) : StrixError

    /** Requested entity does not exist. */
    data class NotFound(
        override val message: String? = null,
    ) : StrixError

    /** A repeatedly failing stream tripped its circuit breaker. */
    data class CircuitOpen(
        override val message: String? = null,
    ) : StrixError

    /** Anything not yet categorised. */
    data class Unknown(
        override val message: String? = null,
        val cause: Throwable? = null,
    ) : StrixError
}
