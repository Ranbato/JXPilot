// $Id: PreferencesModel.java,v 1.5 2008/07/14 03:00:44 taraskostiak Exp $

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

package net.sf.xpilotpanel.preferences.model;

import java.util.List;

import java.net.URL;
import java.net.URLConnection;

import java.io.InputStream;
import java.io.IOException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class is core of concept, that determines how to build GUI for editing
 * preferences.
 * 
 * @author Taras Kostiak
 * 
 */
@XmlRootElement(name = "XPilotPanelPreferencesModel")
public class PreferencesModel {

    /**
     * List of <code>Preference</code>'s.
     * 
     * @see Preference
     */
    protected List<Preference> prefs = null;

    /**
     * List of <code>Tab</code>'s.
     * 
     * @see Tab
     */
    protected List<Tab> tabs = null;

    /**
     * Default constructor.
     * 
     */
    public PreferencesModel() {
    }

    /**
     * Standart getter.
     * 
     * @see #prefs
     */
    @XmlElementWrapper(name = "preferences")
    @XmlElementRef
    public List<Preference> getPrefs() {
        return prefs;
    }

    /**
     * Standart setter.
     * 
     * @see #prefs
     */
    public void setPrefs(List<Preference> prefs) {
        this.prefs = prefs;
    }

    /**
     * Standart getter.
     * 
     * @see #tabs
     */
    @XmlElementWrapper(name = "tabs")
    @XmlElementRef
    public List<Tab> getTabs() {
        return tabs;
    }

    /**
     * Standart setter.
     * 
     * @see #prefs
     */
    public void setTabs(List<Tab> tabs) {
        this.tabs = tabs;
    }

    /**
     * Return <code>Preference</code>, by name specified in given
     * <code>PreferenceSelector</code>.
     * 
     * @param s
     *            <code>PreferenceSelector</code> with name of preference.
     * @return Appropriate <code>Preference</code>.
     */
    public Preference getPreferenceBySelector(PreferenceSelector s) {
        for (Preference p : prefs) {
            if (p.getName().equals(s.getName()))
                return p;
        }

        return null;
    }

    /**
     * Loads model from xml file given by URl, using JAXB.
     * 
     * @param u
     *            URL to load from.
     * @return Loaded model.
     * @throws IOException
     *             If an IOException occurs.
     * @throws JAXBException
     *             On problems with unmarshalling.
     */
    public static PreferencesModel loadModelFromURL(URL u) throws IOException,
            JAXBException {
        URLConnection c = u.openConnection();

        InputStream is = c.getInputStream();

        JAXBContext cn = JAXBContext.newInstance(PreferencesModel.class);
        Unmarshaller unm = cn.createUnmarshaller();

        PreferencesModel m = (PreferencesModel) unm.unmarshal(is);

        is.close();

        return m;
    }

}
