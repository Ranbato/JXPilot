// $Id: BasicServerInfoElement.java,v 1.2 2008/07/14 03:00:40 taraskostiak Exp $

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

/**
 * Stores basic information about XPilot server.
 * 
 * @author Taras Kostiak
 * 
 */
public class BasicServerInfoElement {

    protected String hostname = null;

    protected String version = null;

    protected String serverStatus = null;

    protected String framesPerSecond = null;

    protected String mapName = null;

    protected String mapSize = null;

    protected String mapAuthor = null;

    protected String playersList = null;

    /**
     * Default constructor.
     * 
     */
    public BasicServerInfoElement() {
    }

    /**
     * @return the framesPerSecond
     */
    public String getFramesPerSecond() {
        return framesPerSecond;
    }

    /**
     * @param framesPerSecond
     *            the framesPerSecond to set
     */
    public void setFramesPerSecond(String framesPerSecond) {
        this.framesPerSecond = framesPerSecond;
    }

    /**
     * @return the hostname
     */
    public String getHostname() {
        return hostname;
    }

    /**
     * @param hostname
     *            the hostname to set
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    /**
     * @return the mapAuthor
     */
    public String getMapAuthor() {
        return mapAuthor;
    }

    /**
     * @param mapAuthor
     *            the mapAuthor to set
     */
    public void setMapAuthor(String mapAuthor) {
        this.mapAuthor = mapAuthor;
    }

    /**
     * @return the mapName
     */
    public String getMapName() {
        return mapName;
    }

    /**
     * @param mapName
     *            the mapName to set
     */
    public void setMapName(String mapName) {
        this.mapName = mapName;
    }

    /**
     * @return the mapSize
     */
    public String getMapSize() {
        return mapSize;
    }

    /**
     * @param mapSize
     *            the mapSize to set
     */
    public void setMapSize(String mapSize) {
        this.mapSize = mapSize;
    }

    /**
     * @return the playersList
     */
    public String getPlayersList() {
        return playersList;
    }

    /**
     * @param playersList
     *            the playersList to set
     */
    public void setPlayersList(String playersList) {
        this.playersList = playersList;
    }

    /**
     * @return the serverStatus
     */
    public String getServerStatus() {
        return serverStatus;
    }

    /**
     * @param serverStatus
     *            the serverStatus to set
     */
    public void setServerStatus(String serverStatus) {
        this.serverStatus = serverStatus;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version
     *            the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }

}
