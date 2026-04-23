package org.lambertland.kxpilot

// ---------------------------------------------------------------------------
// Application identity & version constants
// ---------------------------------------------------------------------------
//
// There are two distinct version concepts kept here:
//
//   VERSION_STRING   — the KXPilot release label, shown in the UI and sent
//                      as the HTTP User-Agent when querying the metaserver.
//                      Free to change with each KXPilot release.
//
//   PROTOCOL_VERSION — the XPilot-NG wire protocol integer packed into the
//                      contact-packet magic word (VERSION2MAGIC).  Must match
//                      the value in XPilot-NG 4.7.3 pack.h (CLIENT_VERSION =
//                      0x4F15) so that existing servers accept our connections.
//                      Only change this if the wire protocol is intentionally
//                      updated to a new XPilot-NG protocol level.

object AppInfo {
    /** KXPilot release label, e.g. "0.1-beta". */
    const val VERSION_STRING: String = "0.1-beta"

    /** Full version label shown in the UI. */
    const val VERSION_LABEL: String = "v$VERSION_STRING (KMP port of XPilot NG 4.7.3)"

    /**
     * XPilot-NG wire protocol version integer.
     *
     * Packed into the 32-bit contact-packet magic word via VERSION2MAGIC:
     *   magic = (PROTOCOL_VERSION shl 16) or XP_MAGIC_WORD
     *
     * Matches XPilot-NG 4.7.3 pack.h CLIENT_VERSION = 0x4F15.
     * Sent by the client in every contact request; echoed back by the server.
     */
    const val PROTOCOL_VERSION: Int = 0x4F15

    /**
     * HTTP User-Agent string sent when fetching the internet server list
     * from the metaserver.  Format: "KXPilot/<version>".
     */
    const val USER_AGENT: String = "KXPilot/$VERSION_STRING"

    /**
     * XPilot-NG metaserver URL.  Used by [org.lambertland.kxpilot.net.fetchMetaserverList]
     * when a real HTTP client is available.
     */
    const val METASERVER_URL: String = "https://xpilot.sourceforge.io/cgi-bin/metaserver.cgi"
}
