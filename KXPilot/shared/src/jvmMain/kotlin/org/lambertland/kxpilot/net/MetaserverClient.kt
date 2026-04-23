package org.lambertland.kxpilot.net

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.statement.bodyAsText
import kotlinx.coroutines.CancellationException
import org.lambertland.kxpilot.AppInfo
import org.lambertland.kxpilot.model.ServerInfo

/**
 * Intentionally long-lived singleton HTTP client shared across all metaserver
 * fetches.  CIO engine is pure-Kotlin and works on both desktop JVM and Android.
 *
 * R20: the client is a [Closeable]-backed singleton so that test harnesses and
 * instrumented teardown can call [closeHttpClient] to release the underlying
 * CIO thread pool, preventing dangling background threads from keeping the JVM
 * alive after tests complete.
 */
private val httpClient: HttpClient by lazy {
    HttpClient(CIO) {
        install(HttpTimeout) {
            requestTimeoutMillis = 5_000
            connectTimeoutMillis = 3_000
        }
    }
}

/** Release the shared [HttpClient]. Idempotent: safe to call multiple times. */
fun closeHttpClient() {
    try {
        httpClient.close()
    } catch (_: Exception) {
    }
}

/**
 * JVM actual (shared by desktop and Android): performs a real HTTP GET to the
 * XPilot-NG metaserver CGI and parses the colon-delimited response into a list
 * of [ServerInfo].  Returns an empty list on any network or parse failure.
 *
 * R21: [CancellationException] is re-thrown so coroutine cancellation propagates
 * correctly instead of being swallowed by the catch-all.
 */
actual suspend fun fetchMetaserverList(): List<ServerInfo> =
    try {
        val text =
            httpClient
                .get(AppInfo.METASERVER_URL) {
                    header("User-Agent", AppInfo.USER_AGENT)
                }.bodyAsText()
        parseMetaserverResponse(text)
    } catch (e: CancellationException) {
        throw e // R21: never swallow cancellation
    } catch (_: Exception) {
        emptyList()
    }
