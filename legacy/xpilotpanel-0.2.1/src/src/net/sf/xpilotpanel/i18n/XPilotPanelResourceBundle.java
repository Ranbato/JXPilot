// $Id: XPilotPanelResourceBundle.java,v 1.2 2008/07/14 03:00:44 taraskostiak Exp $

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

package net.sf.xpilotpanel.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * <code>ResourceBundle</code> that loads it's date from xml properties.
 * 
 * @author Taras Kostiak
 * 
 */
public class XPilotPanelResourceBundle extends ResourceBundle {

    /**
     * Storage of bundle data.
     */
    private Properties pr = null;

    /**
     * Creates new <code>XPilotPanelResourceBundle</code> and loads properties
     * from given <code>InputStream</code>.
     * 
     * @throws IOException
     *             When an IOException occurs.
     * @throws InvalidPropertiesFormatException
     *             If xml properties file, given in InputStream is invalid.
     * 
     */
    public XPilotPanelResourceBundle(InputStream is)
            throws InvalidPropertiesFormatException, IOException {
        pr = new Properties();
        pr.loadFromXML(is);
    }

    /**
     * Required implementation.
     * 
     * @see java.util.ResourceBundle#getKeys()
     */
    @SuppressWarnings("unchecked")
    public Enumeration<String> getKeys() {
        Enumeration e = pr.keys();
        return e;
    }

    /**
     * Required implementation.
     * 
     * @see java.util.ResourceBundle#handleGetObject(java.lang.String)
     */
    protected Object handleGetObject(String key) {
        return pr.getProperty(key);
    }

}
