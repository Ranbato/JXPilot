// $Id: XPilotClientWaitThread.java,v 1.6 2008/10/25 15:13:28 taraskostiak Exp $

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

package net.sf.xpilotpanel.gui;

import net.sf.xpilotpanel.XPilotPanelConfDirectory;
import net.sf.xpilotpanel.XPilotPanelDispatcher;
import net.sf.xpilotpanel.XPilotPanelException;
import net.sf.xpilotpanel.XPilotPanelThread;
import net.sf.xpilotpanel.client.ClientRunner;
import net.sf.xpilotpanel.i18n.XPPI18N;

/**
 * Thread that waits until system XPilot client finishes and activates main
 * window.<br>
 * Or in case of embedded java client - actually launches client in concurrent
 * thread.
 * 
 * @author Taras Kostiak
 * 
 */
public class XPilotClientWaitThread extends XPilotPanelThread {

    /**
     * Type 1 client - system.<br>
     * Client to wait for.
     */
    protected Process client = null;

    /**
     * Type 2 client - java embedded.<br>
     * Method to launch client with.
     */
    protected ClientRunner clientLaunchMethod = null;

    /**
     * Type 2 client - java embedded.<br>
     * Server to connect to.
     */
    protected String server = null;

    /**
     * Type 2 client - java embedded.<br>
     * Port to connect to.
     */
    protected int port = -1;

    /**
     * <code>true</code> means to activate main windows after client finished.
     * 
     * @see #finish()
     */
    protected boolean activateMainWindow = true;

    /**
     * Creates this thread to wait until system client finishes.
     * 
     * @param client
     *            Client, to wait for.
     */
    public XPilotClientWaitThread(Process client) {
        this.client = client;
    }

    /**
     * Creates this thread to actually launch embedded java client.
     * 
     * @param clientLaunchMethod
     *            Method to launch client.
     * @param server
     *            Server to connect to.
     * @param port
     *            Port, to connect to.
     */
    public XPilotClientWaitThread(ClientRunner clientLaunchMethod, String server,
            int port) {
        this.clientLaunchMethod = clientLaunchMethod;
        this.server = server;
        this.port = port;
    }

    /**
     * @see net.sf.xpilotpanel.XPilotPanelThread#safeRun()
     */
    @Override
    public void safeRun() throws XPilotPanelException, Exception {
        if (client != null)
            try {
                client.waitFor();
            }
            catch (InterruptedException e) {
            }
        else {
            boolean showWarning = true;
            String warningMessage = null;

            clientLaunchMethod.runClient(server, port,
            		XPilotPanelConfDirectory.get().getPreferences());

            showWarning = false;

            if (showWarning)
                ConnectWindow.showWarning((XPPI18N.get().get("gui",
                        "connectWindow.warning.failedToStart")
                        + ": " + warningMessage));
        }

        if (activateMainWindow)
            XPilotPanelDispatcher.getDispatcher().getMainWindow()
                    .activate(true);
    }

    /**
     * In case of system client this stops this thread on exit.<br>
     * In case of java embedded client this just will remove this thread from
     * dispatcher and XPilotPanel will exit, with java client running.
     * 
     * @see net.sf.xpilotpanel.XPilotPanelThread#finish()
     */
    @Override
    public void finish() {
        activateMainWindow = false;

        if (client != null)
            interrupt();
    }

}
