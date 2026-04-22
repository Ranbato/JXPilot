// $Id: XPilotPanelRBC.java,v 1.3 2008/07/14 03:00:44 taraskostiak Exp $

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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Implementation of <code>ResourceBundle.Control</code>, to load bundle from
 * xml prorerties.
 * 
 * @author Taras Kostiak
 * 
 */
public class XPilotPanelRBC extends ResourceBundle.Control {

    /**
     * Path to directory in jar, where bundles are stored.
     */
    public static final String I18N_DEFAULT_DIR = "data/i18n";

    /**
     * Path where look for bundles.
     */
    protected String i18NPath = null;

    /**
     * Creates <code>XPilotPanelRBC</code> with default path to search for
     * i18n files.
     */
    public XPilotPanelRBC() {
    }

    /**
     * Creates <code>XPilotPanelRBC</code> with specified path to search for
     * i18n files.
     * 
     * @param i18NPath
     *            Path to search for i18n files.
     */
    public XPilotPanelRBC(String i18NPath) {
        this.i18NPath = i18NPath;
    }

    /**
     * @see java.util.ResourceBundle.Control#getFormats(java.lang.String)
     */
    public List<String> getFormats(String baseName) {
        if (baseName == null)
            throw new NullPointerException();

        return Arrays.asList("xml");
    }

    /**
     * @see java.util.ResourceBundle.Control#newBundle(java.lang.String,
     *      java.util.Locale, java.lang.String, java.lang.ClassLoader, boolean)
     */
    public ResourceBundle newBundle(String baseName, Locale locale,
            String format, ClassLoader loader, boolean reload)
            throws IllegalAccessException, InstantiationException, IOException {
        if (baseName == null || locale == null || format == null
                || loader == null)
            throw new NullPointerException();
        ResourceBundle bundle = null;
        if (format.equals("xml")) {
            String bundleName = toBundleName(baseName, locale);
            String resourceName = ((i18NPath == null) ? I18N_DEFAULT_DIR
                    : i18NPath)
                    + "/" + toResourceName(bundleName, format);
            InputStream stream = null;
            if (reload) {
                URL url = loader.getResource(resourceName);
                if (url != null) {
                    URLConnection connection = url.openConnection();
                    if (connection != null) {
                        // Disable caches to get fresh data for
                        // reloading.
                        connection.setUseCaches(false);
                        stream = connection.getInputStream();
                    }
                }
            }
            else {
                stream = loader.getResourceAsStream(resourceName);
            }
            if (stream != null) {
                BufferedInputStream bis = new BufferedInputStream(stream);
                bundle = new XPilotPanelResourceBundle(bis);
                bis.close();
            }
        }
        return bundle;
    }
}
