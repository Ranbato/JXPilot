// $Id: MetaInfoUpdater.java,v 1.12 2008/08/04 06:05:00 taraskostiak Exp $

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

package net.sf.xpilotpanel.meta;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;

import net.sf.xpilotpanel.XPilotPanelDispatcher;

/**
 * This class is core class for getting information about internet XPilot
 * servers from meta server.
 * 
 * @author Taras Kostiak
 * 
 */
public class MetaInfoUpdater {

    /**
     * The single <code>MetaInfoUpdater</code>.
     */
    private static MetaInfoUpdater singleUpdater = null;

    /**
     * List of possible metaservers.
     */
    public static List<InetSocketAddress> metas = null;

    /**
     * Default update timeout.
     */
    public static final long DEFAULT_UPDATE_TIMEOUT = 3000;

    /**
     * Returns single <code>MetaInfoUpdater</code>.
     * 
     * @return Single <code>MetaInfoUpdater</code>.
     */
    public static MetaInfoUpdater getUpdater() {
        if (metas == null) {
            metas = new ArrayList<InetSocketAddress>();

            metas.add(new InetSocketAddress("meta.xpilot.org", 4401));
            metas.add(new InetSocketAddress("meta2.xpilot.org", 4401));
        }

        if (singleUpdater == null) {
            singleUpdater = new MetaInfoUpdater();
        }
        return singleUpdater;
    }

    /**
     * Current meta info.
     */
    private MetaInfo mtNfo = null;

    /**
     * Lock that shows is already update in progress.
     */
    private boolean updateInProgressLock = false;

    /**
     * Lock that shows is timer started.
     */
    private boolean timerStartedLock = false;

    /**
     * An meta server, which is used currently.
     */
    private InetSocketAddress currentMeta = null;

    /**
     * Indicates timeout after what update is performed.
     * 
     * @see #setUpdateTimeout(long)
     * @see #startAutoUpdate()
     */
    private long updateTimeout = -1;

    /**
     * Timestamp of last update.
     */
    private Date lastUpdate = null;

    /**
     * <code>Timer</code> that periodically performs update of information
     * form meta server.
     * 
     * @see #startAutoUpdate()
     */
    private Timer timer = null;

    /**
     * Creates new <code>MetaInfoUpdater</code>.
     */
    private MetaInfoUpdater() {
    }

    /**
     * Returns meta info that was already retrieved.
     * 
     * @return Meta info.
     * @see #updateMetaInfo()
     */
    public MetaInfo getMetaInfo() {
        return mtNfo;
    }

    /**
     * Retuns update timeout.
     * 
     * @return Update timeout.
     * @see #updateTimeout
     */
    public long getUpdateTimeout() {
        return updateTimeout;
    }

    /**
     * Sets update timeout.
     * 
     * @param updateTimeout
     *            New update timeout.
     */
    public void setUpdateTimeout(long updateTimeout) {
        if (updateTimeout <= 0)
            this.updateTimeout = -1;
        else
            this.updateTimeout = updateTimeout;
    }

    /**
     * Shows is meta info out of date.
     * 
     * @return True, if meta info is out of date.
     */
    public boolean isMetaInfoOutOfDate() {
        if (updateTimeout == -1
                || lastUpdate == null
                || (new Date()).getTime() - lastUpdate.getTime() > updateTimeout)
            return true;

        return false;
    }

    /**
     * Updates info from meta server, selected in turn from queue of availible
     * meta servers. <br>
     * It is thread-safe(i.e. if one thread had started update and another will
     * launch again this method - nothing will happen).
     */
    public synchronized void updateMetaInfo() {
        if (updateInProgressLock)
            return;

        updateInProgressLock = true;

        Socket stm = null;

        for (int tries = 0; tries < metas.size(); tries++) {
            try {
                int i = (currentMeta != null) ? (metas.indexOf(currentMeta))
                        : (-1);
                currentMeta = metas
                        .get((i + 1 < metas.size() && currentMeta != null) ? (i + 1)
                                : (0));
                stm = new Socket(currentMeta.getHostName(), currentMeta
                        .getPort());
                break;
            }
            catch (IOException e) {
            }

        }

        if (stm == null)
            mtNfo = null;
        else {
            List<String> rawMetaInfo = new ArrayList<String>();

            try {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(stm.getInputStream()));

                String s = null;
                while ((s = reader.readLine()) != null) {
                    rawMetaInfo.add(s);
                }

                reader.close();
                stm.close();

                mtNfo = new MetaInfo(currentMeta, rawMetaInfo);
            }
            catch (IOException e) {
                mtNfo = null;
            }
        }

        XPilotPanelDispatcher.getDispatcher().getMainWindow()
                .informMetaInfoUpdated();

        updateInProgressLock = false;
    }

    /**
     * Start thread which periodically refreshes metaserver information.
     * 
     * @see #setUpdateTimeout(long)
     */
    public synchronized void startAutoUpdate() {
        if (updateTimeout == -1 || timerStartedLock)
            return;

        timerStartedLock = true;
        timer = new Timer();
        timer.scheduleAtFixedRate(new MetaInfoUpdateTask(), 0, updateTimeout);
    }

    /**
     * Stops launched thread that periodically refreshes metaserver information.
     */
    public synchronized void stopAutoUpdate() {
        if (timerStartedLock) {
            timer.cancel();
            timer.purge();
            timer = null;
            timerStartedLock = false;
        }
    }

}
