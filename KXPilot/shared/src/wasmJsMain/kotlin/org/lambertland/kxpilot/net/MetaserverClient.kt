package org.lambertland.kxpilot.net

import org.lambertland.kxpilot.model.ServerInfo

/** wasmJs actual: returns empty list until ktor-client is wired in. */
actual suspend fun fetchMetaserverList(): List<ServerInfo> = emptyList()
