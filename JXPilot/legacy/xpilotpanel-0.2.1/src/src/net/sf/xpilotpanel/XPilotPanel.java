// $Id: XPilotPanel.java,v 1.15 2008/08/14 20:36:28 taraskostiak Exp $

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

package net.sf.xpilotpanel;

import java.io.IOException;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.xml.bind.JAXBException;

import net.sf.xpilotpanel.gui.XPilotPanelMainWindow;
import net.sf.xpilotpanel.i18n.XPPI18N;
import net.sf.xpilotpanel.meta.MetaInfoUpdater;

/**
 * Main class for XPilotPanel.
 * 
 * @author Taras Kostiak
 * 
 */
public class XPilotPanel {

    /**
     * Name of XPilotPanel's main window title for embedded launch parameter.
     */
    public static final String EMBEDDED_PARAMETER_MAIN_WINDOW_TITLE = "MAIN_WINDOW_TITLE";

    /**
     * Name of icon parameter for all XPilotPanel's windows.
     */
    public static final String EMBEDDED_PARAMETER_ICON = "ICON";

    /**
     * Name parameted that stands for <code>java.lang.reflect.Method</code> -
     * method to invoke for embedded launch of XPilot client in java(i.e.
     * JXPilot or possibly something other).
     */
    public static final String EMBEDDED_PARAMETER_CLIENT_LAUNCH_METHOD = "CLIENT_LAUNCH_METHOD";

    /**
     * Main method for XPilotPanel.
     * 
     * @param args
     *            Commandline arguments.
     */
    public static void main(String[] args) {
        try {
            standaloneLaunch();
        }
        catch (Exception e) {
            e.printStackTrace();
            // TODO: Window with warning should be showed.
        }
    }

    /**
     * Launches standalone XPilotPanel.
     * 
     * @throws XPilotPanelException
     *             On errors with reading/writing ".xpilotpanel" directory and
     *             its content.
     * @throws JAXBException
     *             On problems with loading <code>PreferencesModel</code> from
     *             xml.
     * @throws IOException
     *             When misc IOException occurs.
     */
    public static void standaloneLaunch() throws XPilotPanelException,
            IOException, JAXBException {
        launchXPilotPanel(null);
    }

    /**
     * Launches embedded XPilotPanel(from JXPilot or possibly from other
     * application).<br>
     * Needs next parameters:<br>
     * <ul>
     * <li>"mainWindowTitle" - title of XPilotPanel main
     * window(java.lang.String)</li>
     * <li>"icon" - icon for all XPilotPanel's windows(java.awt.Image)</li>
     * <li>reflection's method to launch client(TODO: should be described)</li>
     * </ul>
     * 
     * @param embeddedParameters
     *            Parameters for embedded launch.
     * 
     * @throws XPilotPanelException
     *             On errors with reading/writing ".xpilotpanel" directory and
     *             its content.
     * @throws JAXBException
     *             On problems with loading <code>PreferencesModel</code> from
     *             xml.
     * @throws IOException
     *             When misc IOException occurs.
     */
    public static void embeddedLaunch(Map<String, ?> embeddedParameters)
            throws XPilotPanelException, IOException, JAXBException {
        launchXPilotPanel(embeddedParameters);
    }

    /**
     * Actual launch of XPilotPanel.
     * 
     * @param embeddedParameters
     *            Parameters for embedded launch, or <code>null</code> for
     *            standalone launch.
     * 
     * @throws XPilotPanelException
     *             On errors with reading/writing ".xpilotpanel" directory and
     *             its content.
     * @throws JAXBException
     *             On problems with loading <code>PreferencesModel</code> from
     *             xml.
     * @throws IOException
     *             When misc IOException occurs.
     */
    private static void launchXPilotPanel(Map<String, ?> embeddedParameters)
            throws XPilotPanelException, IOException, JAXBException {
        XPilotPanelConfDirectory.load();
        XPilotPanelConfDirectory.get().loadPreferences();

        XPPI18N.initXPilotPanelLanguage();

        XPilotPanelThread startThread = new XPilotPanelThread() {

            private XPilotPanelMainWindow wn = null;

            public void safeRun() throws XPilotPanelException, Exception {
                wn = new XPilotPanelMainWindow();
                XPilotPanelDispatcher.getDispatcher().setMainWindow(wn);
                wn.activate(true);
            }

            public void finish() {
            }
        };

        if (embeddedParameters != null)
            XPilotPanelDispatcher.getDispatcher().setEmbeddedParameters(
                    embeddedParameters);

        XPilotPanelDispatcher.getDispatcher().addThread(startThread);
        MetaInfoUpdater.getUpdater().setUpdateTimeout(
                MetaInfoUpdater.DEFAULT_UPDATE_TIMEOUT);

        SwingUtilities.invokeLater(startThread);
    }

}
