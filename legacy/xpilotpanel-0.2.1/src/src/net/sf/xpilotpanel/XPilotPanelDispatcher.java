// $Id: XPilotPanelDispatcher.java,v 1.7 2008/08/14 18:17:59 taraskostiak Exp $

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

import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import java.io.IOException;

import net.sf.xpilotpanel.gui.XPilotPanelMainWindow;
import net.sf.xpilotpanel.meta.MetaInfoUpdater;

/**
 * Stores all resources used by XPilotPanel and finishes them on exit.
 * 
 * @author Taras Kostiak
 * 
 */
public class XPilotPanelDispatcher {

    /**
     * Single <code>XPilotPanelDispatcher</code>.
     */
    private static XPilotPanelDispatcher singleDispatcher = null;

    /**
     * Returns single <code>XPilotPanelDispatcher</code>.
     * 
     * @return <code>XPilotPanelDispatcher</code>.
     */
    public static XPilotPanelDispatcher getDispatcher() {
        if (singleDispatcher == null)
            singleDispatcher = new XPilotPanelDispatcher();

        return singleDispatcher;
    }

    /**
     * List of dispatched <code>XPilotPanelThread</code>'s.
     */
    private List<XPilotPanelThread> threads = null;

    /**
     * List of windows to be closed.
     */
    private List<Window> windows = null;

    /**
     * Main window of XPilotPanel.
     */
    private XPilotPanelMainWindow mainWindow = null;

    /**
     * Parameters for embedded launch(from JXPilot or possibly from other
     * application).
     */
    private Map<String, ?> embeddedParameters = null;

    /**
     * Initialises single <code>XPilotPanelDispatcher</code>.
     * 
     */
    private XPilotPanelDispatcher() {
        threads = new ArrayList<XPilotPanelThread>();
        windows = new ArrayList<Window>();
    }

    /**
     * Exits XPilotPanel freeing all resources.
     */
    public void exit() {
        for (Window w : windows) {
            w.dispose();
        }
        for (XPilotPanelThread t : threads) {
            t.finish();
        }
        MetaInfoUpdater.getUpdater().stopAutoUpdate();
        mainWindow.dispose();

        try {
            XPilotPanelConfDirectory.get().storePreferences();
        }
        catch (IOException e) {
        }
        catch (XPilotPanelException e) {
        }
    }

    /**
     * Adds thread to finish on exit
     * 
     * @param t
     *            <code>XPilotPanelThread</code> to finish on exit.
     */
    public void addThread(XPilotPanelThread t) {
        threads.add(t);
    }

    /**
     * Removes, added previously, thread from list of threads that are to finish
     * on exit.
     * 
     * @param t
     *            <code>XPilotPanelThread</code> to remove.
     */
    public void removeThread(XPilotPanelThread t) {
        for (int i = 0; i < threads.size(); i++) {
            if (t == threads.get(i)) {
                threads.remove(i);
                return;
            }
        }
    }

    /**
     * Adds window to dispose on exit
     * 
     * @param w
     *            <code>Window</code> to finish on exit.
     */
    public void addWindow(Window w) {
        windows.add(w);
    }

    /**
     * Removes, added previously, window from list of windows that are to
     * dispose on exit.
     * 
     * @param w
     *            <code>Window</code> to remove.
     */
    public void removeWindow(Window w) {
        for (int i = 0; i < windows.size(); i++) {
            if (w == windows.get(i)) {
                windows.remove(i);
                return;
            }
        }
    }

    /**
     * Returns stored reference to XPilotPanel's main window.
     * 
     * @return Main window.
     */
    public XPilotPanelMainWindow getMainWindow() {
        return mainWindow;
    }

    /**
     * Sets XPilotPanel's main window to dispatcher.
     * 
     * @param newMW
     *            Main window.
     */
    public void setMainWindow(XPilotPanelMainWindow newMW) {
        mainWindow = newMW;
    }

    /**
     * @see #embeddedParameters
     */
    public Map<String, ?> getEmbeddedParameters() {
        return embeddedParameters;
    }

    /**
     * @see #embeddedParameters
     */
    public void setEmbeddedParameters(Map<String, ?> embeddedParameters) {
        this.embeddedParameters = embeddedParameters;
    }

}
