// $Id: Tab.java,v 1.3 2008/07/14 03:00:44 taraskostiak Exp $

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

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * This class determines how to build an tab for GUI.
 * 
 * @author Taras Kostiak
 * 
 */
@XmlRootElement(name = "tab")
public class Tab {

    /**
     * Name of tab to display.
     */
    protected String name = null;

    /**
     * List of preferences in this tab.
     */
    protected List<PreferenceSelector> prefs = null;

    /**
     * Default constructor.
     * 
     */
    public Tab() {
    }

    /**
     * Standart getter.
     * 
     * @see #name
     */
    @XmlAttribute(required = true)
    public String getName() {
        return name;
    }

    /**
     * Standart setter.
     * 
     * @see #name
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Standart getter.
     * 
     * @see #prefs
     */
    @XmlElementRef
    public List<PreferenceSelector> getPrefs() {
        return prefs;
    }

    /**
     * Standart setter.
     * 
     * @see #prefs
     */
    public void setPrefs(List<PreferenceSelector> prefs) {
        this.prefs = prefs;
    }

    /**
     * Returns key for i18n depending on its name.
     * 
     * @return Key for i18n.
     */
    public String getI18nKey() {
        return "preference.tab." + name;
    }

}
