package org.lambertland.kxpilot.net

import org.lambertland.kxpilot.AppInfo
import org.lambertland.kxpilot.model.ServerInfo

/**
 * Fetches the internet server list from the XPilot-NG metaserver via HTTP GET.
 * Returns an empty list on failure.
 *
 * @param url  Metaserver CGI URL.  Defaults to [AppInfo.METASERVER_URL] so
 *             existing call sites and tests continue to work without change.
 *             Pass a config-supplied value to make the URL user-configurable.
 */
expect suspend fun fetchMetaserverList(url: String = AppInfo.METASERVER_URL): List<ServerInfo>
