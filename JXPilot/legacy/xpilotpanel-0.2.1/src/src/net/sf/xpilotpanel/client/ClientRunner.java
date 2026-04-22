// $Id: ClientRunner.java,v 1.2 2008/10/25 15:13:28 taraskostiak Exp $

// XPilotPanel.
// Copyright (C) 2007-2008 by: Taras Kostiak and others(see PEOPLE file).
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA

package net.sf.xpilotpanel.client;

import net.sf.xpilotpanel.preferences.Preferences;

/**
 * Represents an object that knows how to start the actual xpilot client. This
 * would be passed as an embedded parameter by, for example, jxpilot.
 * 
 * @author vlad
 */
public interface ClientRunner {

    /**
     * Starts up the xpilot client.
     * 
     * @param serverIp
     *            The server's ip address.
     * @param serverPort
     *            The server's port number.
     * @param prefs
     *            The preferences for the client.
     */
    public void runClient(String serverIp, int serverPort, Preferences prefs);

}
