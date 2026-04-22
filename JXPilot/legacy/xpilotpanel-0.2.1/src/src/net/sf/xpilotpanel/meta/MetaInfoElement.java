// $Id: MetaInfoElement.java,v 1.6 2008/07/15 05:03:32 taraskostiak Exp $

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
 * Information about XPilot server, retrieved from meta server.
 * 
 * @author Taras Kostiak
 * 
 */
public class MetaInfoElement extends BasicServerInfoElement {

    // protected String version = null;

    // protected String hostname = null;

    protected String portNumber = null;

    protected String numberOfUsers = null;

    // protected String mapName = null;

    // protected String mapSize = null;

    // protected String mapAuthor = null;

    // protected String serverStatus = null;

    protected String numberOfHomeBases = null;

    // protected int framesPerSecond = -1;

    // protected String playersList = null;

    protected String sound = null;

    protected String serverUptime = null;

    protected String numberOfTeamBases = null;

    protected String timing = null;

    protected String ipNumber = null;

    protected String numberOfFreeHomeBases = null;

    protected String numberOfQueuedPlayers = null;

    /**
     * Parses given meta information in string representation to object
     * represenatation.
     * 
     * @param s
     *            Meta information in string represenation.
     * @return Meta information in object representaion or null if given string
     *         is incorrect.
     */
    public static MetaInfoElement parse(String s) {
        String[] dataElements = s.split(":");

        if (dataElements.length != 18)
            return null;

        MetaInfoElement nw = new MetaInfoElement();

        nw.version = dataElements[0];
        nw.hostname = dataElements[1];
        nw.portNumber = dataElements[2];
        nw.numberOfUsers = dataElements[3];
        nw.mapName = dataElements[4];
        nw.mapSize = dataElements[5];
        nw.mapAuthor = dataElements[6];
        nw.serverStatus = dataElements[7];
        nw.numberOfHomeBases = dataElements[8];
        nw.framesPerSecond = dataElements[9];
        nw.playersList = dataElements[10];
        nw.sound = dataElements[11];
        nw.serverUptime = dataElements[12];
        nw.numberOfTeamBases = dataElements[13];
        nw.timing = dataElements[14];
        nw.ipNumber = dataElements[15];
        nw.numberOfFreeHomeBases = dataElements[16];
        nw.numberOfQueuedPlayers = dataElements[17];

        return nw;
    }

    /**
     * Construct <code>MetaInfoElement</code> with empty fields.
     * 
     */
    public MetaInfoElement() {
    }

    /**
     * @return the ipNumber
     */
    public String getIpNumber() {
        return ipNumber;
    }

    /**
     * @return the numberOfFreeHomebases
     */
    public String getNumberOfFreeHomeBases() {
        return numberOfFreeHomeBases;
    }

    /**
     * @return the numberOfHomeBases
     */
    public String getNumberOfHomeBases() {
        return numberOfHomeBases;
    }

    /**
     * @return the numberOfQueuedPlayers
     */
    public String getNumberOfQueuedPlayers() {
        return numberOfQueuedPlayers;
    }

    /**
     * @return the numberOfTeamBases
     */
    public String getNumberOfTeamBases() {
        return numberOfTeamBases;
    }

    /**
     * @return the numberOfUsers
     */
    public String getNumberOfUsers() {
        return numberOfUsers;
    }

    /**
     * @return the portNumber
     */
    public String getPortNumber() {
        return portNumber;
    }

    /**
     * @return the serverUptime
     */
    public String getServerUptime() {
        return serverUptime;
    }

    /**
     * @return the sound
     */
    public String getSound() {
        return sound;
    }

    /**
     * @return the timing
     */
    public String getTiming() {
        return timing;
    }

    /**
     * @param ipNumber
     *            the ipNumber to set
     */
    public void setIpNumber(String ipNumber) {
        this.ipNumber = ipNumber;
    }

    /**
     * @param numberOfFreeHomebases
     *            the numberOfFreeHomebases to set
     */
    public void setNumberOfFreeHomeBases(String numberOfFreeHomeBases) {
        this.numberOfFreeHomeBases = numberOfFreeHomeBases;
    }

    /**
     * @param numberOfHomeBases
     *            the numberOfHomeBases to set
     */
    public void setNumberOfHomeBases(String numberOfHomeBases) {
        this.numberOfHomeBases = numberOfHomeBases;
    }

    /**
     * @param numberOfQueuedPlayers
     *            the numberOfQueuedPlayers to set
     */
    public void setNumberOfQueuedPlayers(String numberOfQueuedPlayers) {
        this.numberOfQueuedPlayers = numberOfQueuedPlayers;
    }

    /**
     * @param numberOfTeamBases
     *            the numberOfTeamBases to set
     */
    public void setNumberOfTeamBases(String numberOfTeamBases) {
        this.numberOfTeamBases = numberOfTeamBases;
    }

    /**
     * @param numberOfUsers
     *            the numberOfUsers to set
     */
    public void setNumberOfUsers(String numberOfUsers) {
        this.numberOfUsers = numberOfUsers;
    }

    /**
     * @param portNumber
     *            the portNumber to set
     */
    public void setPortNumber(String portNumber) {
        this.portNumber = portNumber;
    }

    /**
     * @param serverUptime
     *            the serverUptime to set
     */
    public void setServerUptime(String serverUptime) {
        this.serverUptime = serverUptime;
    }

    /**
     * @param sound
     *            the sound to set
     */
    public void setSound(String sound) {
        this.sound = sound;
    }

    /**
     * @param timing
     *            the timing to set
     */
    public void setTiming(String timing) {
        this.timing = timing;
    }

}
