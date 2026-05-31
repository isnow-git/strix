package dev.strix.feature.onboarding.server

import dev.strix.core.common.model.StreamSourceConfig
import dev.strix.feature.onboarding.token.OnboardingSession
import fi.iki.elonen.NanoHTTPD

/**
 * Tiny HTTP server, alive only during onboarding (ADR-0005/0008).
 *
 * - GET `/?t=<token>` serves the credential form if the token is currently valid.
 * - POST burns the single-use token and forwards the submitted source.
 *
 * Bound to the site-local interface on an ephemeral port (pass `port = 0`); read
 * [getListeningPort] after [start] for the chosen port.
 */
class OnboardingServer(
    hostname: String,
    private val pairing: OnboardingSession,
    private val formHtml: String,
    private val onCredentials: (StreamSourceConfig) -> Unit,
) : NanoHTTPD(hostname, EPHEMERAL_PORT) {
    @Suppress("TooGenericExceptionCaught") // Any handler failure must become a 500, never crash the server thread.
    override fun serve(httpSession: IHTTPSession): Response =
        try {
            when (httpSession.method) {
                Method.GET -> handleGet(httpSession)
                Method.POST -> handlePost(httpSession)
                else -> text(Response.Status.METHOD_NOT_ALLOWED, "Method not allowed")
            }
        } catch (e: Exception) {
            text(Response.Status.INTERNAL_ERROR, "Error: ${e.message}")
        }

    private fun handleGet(httpSession: IHTTPSession): Response {
        val token = httpSession.parameters["t"]?.firstOrNull()
        return if (token != null && pairing.isValid(token)) {
            newFixedLengthResponse(Response.Status.OK, MIME_HTML, formHtml)
        } else {
            text(Response.Status.FORBIDDEN, "Invalid or expired pairing link.")
        }
    }

    private fun handlePost(httpSession: IHTTPSession): Response {
        val body = HashMap<String, String>()
        httpSession.parseBody(body)
        val params = httpSession.parms

        val token = params["t"]
        if (token == null || !pairing.consume(token)) {
            return text(Response.Status.FORBIDDEN, "Invalid, expired, or already-used pairing link.")
        }

        val source =
            parseSource(params)
                ?: return text(Response.Status.BAD_REQUEST, "Missing or invalid credentials.")

        onCredentials(source)
        return text(Response.Status.OK, "Connected. You can return to your TV.")
    }

    private fun parseSource(params: Map<String, String>): StreamSourceConfig? =
        when (params["type"]) {
            "m3u" -> params["url"]?.takeIf { it.isNotBlank() }?.let(StreamSourceConfig::M3u)
            "xtream" -> {
                val host = params["host"]?.takeIf { it.isNotBlank() }
                val user = params["username"]?.takeIf { it.isNotBlank() }
                val pass = params["password"]?.takeIf { it.isNotBlank() }
                if (host != null && user != null && pass != null) {
                    StreamSourceConfig.Xtream(host, user, pass)
                } else {
                    null
                }
            }
            else -> null
        }

    private fun text(
        status: Response.Status,
        message: String,
    ): Response = newFixedLengthResponse(status, MIME_PLAINTEXT, message)

    private companion object {
        const val EPHEMERAL_PORT = 0
        const val MIME_HTML = "text/html"
    }
}
