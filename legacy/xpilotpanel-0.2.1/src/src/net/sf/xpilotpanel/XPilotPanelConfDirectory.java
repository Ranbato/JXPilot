// $Id: XPilotPanelConfDirectory.java,v 1.3 2008/07/14 03:00:08 taraskostiak Exp $

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

package net.sf.xpilotpanel;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import javax.xml.bind.JAXBException;

import net.sf.xpilotpanel.preferences.Preferences;

import net.sf.xpilotpanel.preferences.model.PreferencesModel;

/**
 * This class manages ".xpilotpanel" directory in "user.home". <br>
 * When more files will be added, should be rewritten from scratch.
 * 
 * @author Taras Kostiak
 * 
 */
public class XPilotPanelConfDirectory {

    /**
     * Single <code>XPilotPanelConfDirectory</code>.
     */
    private static XPilotPanelConfDirectory single = null;

    /**
     * Initialise this the {@link #single}.
     * 
     * @throws XPilotPanelException
     *             When it can't be initialised - reading/writing errors.
     */
    public static void load() throws XPilotPanelException {
        single = new XPilotPanelConfDirectory();

        if (!check(single.getXPilotPanelDirectory(), true))
            throw new XPilotPanelException(
                    XPilotPanelException.XPILOTPANEL_DIRECTORY_ACCESS_ERROR);
    }

    /**
     * Is used to get the <code>XPilotPanelConfDirectory</code>.
     * 
     * @return Single <code>XPilotPanelConfDirectory</code>.
     */
    public static XPilotPanelConfDirectory get() {
        return single;
    }

    /**
     * Name if directory.
     */
    public static final String name = ".xpilotpanel";

    /**
     * Name of file with preferences.
     */
    public static final String preferencesFileName = "preferences.xml";

    /**
     * Reference to directory.
     */
    private File dir = null;

    /**
     * Reference to file with preferences.
     */
    private File preferencesFile = null;

    /**
     * XPilotPanel's <code>Preferences</code>.
     */
    private Preferences prefs = null;

    /**
     * Private(to make it single) constructor, which create ".xpilotpanel"
     * directory, if such isn't present in user's home directory.
     */
    private XPilotPanelConfDirectory() {
        dir = new File(System.getProperty("user.home") + File.separatorChar
                + name);

        if (dir.exists()) {
            if (dir.isDirectory())
                return;
            else
                dir.delete();
        }

        dir.mkdir();
    }

    /**
     * Checks if directory or file(look <code>directoryOrFile</code>
     * parameter) exists, is readable and writeable.
     * 
     * @param f
     *            Directory or File to check.
     * @param directoryOrFile
     *            Test if it is directory or file.
     * @return True, if directory or file is useable for XPilotPanel.
     */
    public static boolean check(File f, boolean directoryOrFile) {
        return f.exists()
                && ((directoryOrFile && f.isDirectory()) || (!directoryOrFile && f
                        .isFile())) && f.canRead() && f.canWrite();
    }

    /**
     * Returns loaded previously preferences.
     * 
     * @return XPilotPanel's preferences. Can be <code>null</code> if they
     *         weren't loaded.
     */
    public Preferences getPreferences() {
        return prefs;
    }

    /**
     * Loads preferences(and model for it).
     * 
     * @throws IOException
     *             When IOException occurs.
     * @throws JAXBException
     *             When model can't be loaded.
     * @throws XPilotPanelException
     *             When error with reading {@link #preferencesFileName} file.
     */
    public void loadPreferences() throws IOException, JAXBException,
            XPilotPanelException {
        PreferencesModel m = PreferencesModel
                .loadModelFromURL(XPilotPanelConfDirectory.class
                        .getClassLoader().getResource(
                                "data/preferencesModel.xml"));

        InputStream is = new FileInputStream(getPreferencesFile());

        prefs = Preferences.loadPreferences(m, is);
    }

    /**
     * Stores preferences to disk.
     * 
     * @throws IOException
     *             If an IOException occurs.
     * @throws XPilotPanelException
     *             When error with reading {@link #preferencesFileName} file.
     */
    public void storePreferences() throws IOException, XPilotPanelException {
        if (prefs == null)
            return;

        OutputStream os = new FileOutputStream(getPreferencesFile());

        prefs.store(os);
    }

    /**
     * Creates(if not created) and checks if it is useable,
     * {@link #preferencesFileName} file.
     * 
     * @return Reference to {@link #preferencesFileName}.
     * @throws XPilotPanelException
     *             When this file have errors(reading/writing etc.).
     */
    private File getPreferencesFile() throws XPilotPanelException {
        if (preferencesFile == null) {
            preferencesFile = new File(dir.getAbsolutePath()
                    + File.separatorChar + preferencesFileName);

            if (preferencesFile.exists() && !preferencesFile.isFile())
                preferencesFile.delete();

            if (!preferencesFile.exists()) {
                try {
                    preferencesFile.createNewFile();
                }
                catch (IOException e) {
                }
            }
        }

        if (!check(preferencesFile, false))
            throw new XPilotPanelException(
                    XPilotPanelException.XPILOTPANEL_DIRECTORY_ACCESS_ERROR);

        return preferencesFile;
    }

    /**
     * Returns ".xpilotpanel" directory reference.
     * 
     * @return Reference to ".xpilotpanel" directory.
     */
    public File getXPilotPanelDirectory() {
        return dir;
    }

}
