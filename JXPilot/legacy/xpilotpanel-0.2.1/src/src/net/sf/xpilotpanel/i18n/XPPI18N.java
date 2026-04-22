// $Id: XPPI18N.java,v 1.7 2008/08/03 09:22:57 taraskostiak Exp $

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

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import net.sf.xpilotpanel.XPilotPanelConfDirectory;

/**
 * This class manages i18n for XPilotPanel.
 * 
 * @author Taras Kostiak
 * 
 */
public class XPPI18N {

    /**
     * Is showed as i18n'lised value, when resource couldn't be found in any
     * case.
     */
    private static String MISSING_RESOURCE_IDENT = "I18N_ERROR";

    /**
     * Single <code>XPPI18N</code>.
     */
    private static XPPI18N single = null;

    /**
     * Return single <code>XPPI18N</code>. <br>
     * 
     * It's "single role" is used only for XPilotPanel. But it is library that
     * also can be used by other programs.
     * 
     * @return <code>XPPI18N</code>.
     */
    public static XPPI18N get() {
        if (single == null) {
            single = new XPPI18N();
        }

        return single;
    }

    /**
     * Current locale.
     */
    private Locale l = null;

    /**
     * Determines how to load bundles.
     */
    private ResourceBundle.Control c = null;

    /**
     * Default constructor.
     * 
     */
    public XPPI18N() {
        l = Locale.getDefault();
        c = new XPilotPanelRBC();
    }

    /**
     * Creates <code>XPPI18N</code> with another path to search bundles.
     */
    public XPPI18N(String bundlePath) {
        l = Locale.getDefault();
        c = new XPilotPanelRBC(bundlePath);
    }

    /**
     * Returns i18n'sed value of key in default language.
     * 
     * @param namespace
     *            Namespace, where to seek locale.
     * @param key
     *            Key to i18n'lise.
     * @return I18n'sed value.
     * @see #get(String, String, Locale)
     */
    public String get(String namespace, String key) {
        return get(namespace, key, this.l);
    }

    /**
     * Returns i18n'sed value of key in specified language.
     * 
     * @param namespace
     *            Namespace, where to seek locale.
     * @param key
     *            Key to i18n'lise.
     * @param l
     *            Locale to use.
     * @return I18n'sed value.
     */
    public String get(String namespace, String key, Locale l) {
        String res = null;
        try {
            res = ResourceBundle.getBundle(namespace, l, c).getString(key);
        }
        catch (MissingResourceException e) {
            res = MISSING_RESOURCE_IDENT;
        }
        return res;
    }

    /**
     * Returns default locale, used by <code>XPPI18N</code>.
     * 
     * @return Default <code>Locale</code>.
     */
    public Locale getLocale() {
        return l;
    }

    /**
     * Sets default locale to <code>XPPI18N</code>.
     * 
     * @param l
     *            <code>Locale</code> to be set default.
     */
    public void setLocale(Locale l) {
        this.l = l;
    }

    public static void initXPilotPanelLanguage() {
        if (XPilotPanelConfDirectory.get().getPreferences().get(
                "useAnotherLanguage").compareTo("true") == 0) {
            String myLanguage = XPilotPanelConfDirectory.get().getPreferences()
                    .get("language");
            String[] langs = { "English", "Ukrainian(uk)", "Russian(ru)",
                    "Swedish(sv)", "German(de)", "Portuguese(pt)", "Dutch(nl)",
                    "French(fr)", "Polish(pl)" };
            String[] langCode = { "", "uk", "ru", "sv", "de", "pt", "nl", "fr",
                    "pl" };

            for (int i = 0; i < langs.length; i++)
                if (myLanguage.compareTo(langs[i]) == 0) {
                    XPPI18N.get().setLocale(new Locale(langCode[i]));
                    break;
                }
        }
    }

}
