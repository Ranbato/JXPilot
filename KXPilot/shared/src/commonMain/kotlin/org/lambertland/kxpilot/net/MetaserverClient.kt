package org.lambertland.kxpilot.net

import org.lambertland.kxpilot.model.ServerInfo

/**
 * Fetches the internet server list from the XPilot-NG metaserver.
 * Returns an empty list on failure.
 *
 * The default implementation returns a stub.  Replace with a real HTTP GET
 * to AppInfo.METASERVER_URL when ktor-client is added as a dependency.
 */
expect suspend fun fetchMetaserverList(): List<ServerInfo>
