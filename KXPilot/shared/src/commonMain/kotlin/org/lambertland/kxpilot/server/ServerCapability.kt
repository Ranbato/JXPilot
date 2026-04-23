package org.lambertland.kxpilot.server

/**
 * Returns true if the embedded UDP server is supported on this platform.
 *
 * Desktop (JVM): true — full DatagramSocket support.
 * Android: false — only content-URI sockets are available; DatagramSocket
 *          throws SecurityException or is unavailable in sandboxed apps.
 * wasmJs: false — no raw UDP socket access in the browser.
 *
 * [ServerController.start] checks this before attempting to bind and surfaces
 * a clear [ServerState.Error] instead of a cryptic UnsupportedOperationException.
 */
internal expect fun isServerSupported(): Boolean
