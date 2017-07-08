// $Id: Preferences.java,v 1.6 2008/07/14 03:00:45 taraskostiak Exp $

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

package net.sf.xpilotpanel.preferences;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.List;
import java.util.Properties;

import net.sf.xpilotpanel.preferences.model.Preference;
import net.sf.xpilotpanel.preferences.model.PreferencesModel;

/**
 * This class manages preferences for XPilotPanel.
 * 
 * @author Taras Kostiak
 * 
 */
public class Preferences {

    /**
     * Creatres new <code>Preferences</code> with default values.
     * 
     * @return Preferences with default values.
     */
    public static Preferences createNewPreferences(PreferencesModel m) {
        return new Preferences(m, null);
    }

    /**
     * Loads preferences from given stream. <br>
     * If xml file is broken(wrong format) or IO exception occurs, it ignores it
     * and loads nothing.
     * 
     * @param is
     *            <code>InputStream</code>, to load preferences from.
     * @return Loaded preferences.
     */
    public static Preferences loadPreferences(PreferencesModel m, InputStream is) {
        return new Preferences(m, is);
    }

    /**
     * <code>Properties</code> that contain values for XPilotPanel's
     * preferences.
     */
    private Properties prPr = null;

    /**
     * <code>PreferencesModel</code>, used by this <code>Preferences</code>.
     */
    private PreferencesModel model = null;

    /**
     * @param m
     * @param is
     * @throws IOException
     *             When an IO exception occurs.
     * @throws InvalidPropertiesFormatException
     *             When format of xml file with properties is invalid.
     */
    private Preferences(PreferencesModel m, InputStream is) {
        model = m;

        prPr = new Properties();
        if (is != null) {
            try {
                prPr.loadFromXML(is);
            }
            catch (InvalidPropertiesFormatException e) {
            }
            catch (IOException e) {
            }
        }
    }

    /**
     * Returns <code>PreferencesModel</code>, used by this
     * <code>Preferences</code>.
     * 
     * @return <code>PreferencesModel</code>.
     */
    public PreferencesModel getModel() {
        return model;
    }

    /**
     * Stores current values(if default value was not changed it is not stored,
     * `cos it is defined in <code>PreferencesModel</code>, used by this
     * <code>Preferences</code>) to output stream.
     * 
     * @param os
     *            <code>OutputStream</code>, to store current values too.
     * @throws IOException
     */
    public void store(OutputStream os) throws IOException {
        prPr.storeToXML(os, null, "UTF-8");
    }

    /**
     * Returns value of preference.
     * 
     * @param key
     *            Preference's name.
     * @return Value of preference(default value - if preference not set), empty
     *         string if preference with such name is defined, but have no
     *         default value or <code>null</code>, if preference with such
     *         name is not found.
     */
    public String get(String key) {
        String res = prPr.getProperty(key);

        if (res == null) {
            List<Preference> l = model.getPrefs();
            for (Preference p : l)
                if (p.getName().equals(key)) {
                    res = p.getDefaultValue();
                    if (res == null)
                        res = "";
                    break;
                }
        }

        return res;
    }

    /**
     * Sets value for preference.
     * <p>
     * Could be used to store preferences that are not defined in
     * <code>PreferencesModel</code>, used by this preferences.
     * 
     * @param key
     *            Preference's name.
     * @param value
     *            Value to set.
     */
    public void set(String key, String value) {
        prPr.setProperty(key, value);
    }

}
