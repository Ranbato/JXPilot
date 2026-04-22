// $Id: MetaInfo.java,v 1.7 2008/07/22 11:53:15 taraskostiak Exp $

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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Object representation of information from XPilot meta server.
 * 
 * @author Taras Kostiak
 * 
 */
public class MetaInfo {

    /**
     * Sorts list by decreasement of number of players.
     * 
     * @param l
     *            List of <code>MetaInfoElement</code>'s to sort.
     */
    public static void sortList(List<MetaInfoElement> l) {
        for (int i = 0; i < l.size() - 1; i++)
            for (int j = i + 1; j < l.size(); j++) {
                MetaInfoElement eAtI = l.get(i);
                MetaInfoElement eAtJ = l.get(j);

                if (Integer.parseInt(eAtJ.getNumberOfUsers()) > Integer
                        .parseInt(eAtI.getNumberOfUsers())) {
                    l.set(i, eAtJ);
                    l.set(j, eAtI);
                }
            }
    }

    /**
     * List of information about servers.
     */
    private InetSocketAddress retrievedFrom = null;

    /**
     * List of information about servers.
     * 
     * @see MetaInfoElement
     */
    private List<MetaInfoElement> metaInfo = null;

    /**
     * Sorted list of information about servers(contains references to same
     * objects).
     * 
     * @see MetaInfoElement
     */
    private List<MetaInfoElement> sortedMetaInfo = null;

    /**
     * Time, when this <code>MetaInfo</code> was created.
     */
    private Date timeStamp = null;

    /**
     * Constructs new <code>MetaInfo</code>, with empty content.
     * 
     * @param adr
     *            Server, from which this meta inforamtion is retrieved from.
     * 
     * @see #add(MetaInfoElement)
     */
    public MetaInfo(InetSocketAddress adr) {
        this(adr, null);
    }

    /**
     * Constructs new <code>MetaInfo</code>.
     * 
     * @param adr
     *            Server, from which this meta inforamtion is retrieved from.
     * @param rawMetaInfo
     *            List with information about servers(in <code>String</code>
     *            representation).
     */
    public MetaInfo(InetSocketAddress adr, List<String> rawMetaInfo) {
        timeStamp = new Date();
        retrievedFrom = adr;
        metaInfo = new ArrayList<MetaInfoElement>();

        if (rawMetaInfo != null)
            for (String s : rawMetaInfo)
                metaInfo.add(MetaInfoElement.parse(s));

    }

    /**
     * @return List of information about servers.
     */
    public InetSocketAddress getRetrievedFrom() {
        return retrievedFrom;
    }

    /**
     * Returns list of information about servers.
     * 
     * @return List of information about servers.
     */
    public List<MetaInfoElement> getMetaInfo() {
        return metaInfo;
    }

    /**
     * Returns sorted list of information about servers.
     * 
     * @return Sorted list of information about servers.
     * 
     * @see #sortedMetaInfo
     */
    public List<MetaInfoElement> getSortedMetaInfo() {
        if (sortedMetaInfo == null) {
            sortedMetaInfo = new ArrayList<MetaInfoElement>();

            for (MetaInfoElement e : metaInfo)
                sortedMetaInfo.add(e);

            sortList(sortedMetaInfo);
        }
        return sortedMetaInfo;
    }

    /**
     * @return Time, when this <code>MetaInfo</code> was created.
     */
    public Date getTimeStamp() {
        return timeStamp;
    }

    /**
     * Adds info about server.
     * 
     * @param el
     *            Info about server.
     */
    public void add(MetaInfoElement el) {
        metaInfo.add(el);
    }

}
