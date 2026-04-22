// $Id: PreferencesTester.java,v 1.5 2008/07/14 03:00:45 taraskostiak Exp $

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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;

import net.sf.xpilotpanel.preferences.model.Preference;
import net.sf.xpilotpanel.preferences.model.PreferenceSelector;
import net.sf.xpilotpanel.preferences.model.PreferencesModel;
import net.sf.xpilotpanel.preferences.model.Tab;

/**
 * Tesing class - should be removed.
 * 
 * @author Taras Kostiak
 * 
 */
public class PreferencesTester {

    private static File modelFile = new File("/home/taras/1/model.xml");

    private static File prefsFile = new File("/home/taras/1/1.xml");

    private static JAXBContext c = null;

    private static PreferencesModel mod = null;

    public static void main(String[] args) throws JAXBException,
            InvalidPropertiesFormatException, IOException {
        c = JAXBContext.newInstance(PreferencesModel.class);
        mod = getModelFromFile(modelFile);

        testModels();

        InputStream is = null;
        try {
            is = new FileInputStream(prefsFile);
        }
        catch (FileNotFoundException e) {
        }

        Preferences prefs = Preferences.loadPreferences(mod, is);

        (new PreferencesEditor(prefs)).setVisible(true);

        // prefs.set("pr1", "pr1");
        // prefs.set("pr2", "pr2");
        // prefs.set("pr3", "pr3");
        // prefs.set("other_existing", "yep");
        System.out.println(prefs.get("pr1"));
        System.out.println(prefs.get("pr2"));
        System.out.println(prefs.get("pr3"));
        System.out.println(prefs.get("other_existing"));
        System.out.println(prefs.get("other_unexisting"));
        System.out.println(prefs.get("pr_CB"));

        if (!prefsFile.exists()) {
            System.out.println("HARRR!!!!!!!!!!!!!");
            prefsFile.createNewFile();
        }
        OutputStream os = new FileOutputStream(prefsFile);
        prefs.store(os);
    }

    public static PreferencesModel getModelFromFile(File f)
            throws JAXBException {
        Unmarshaller um = c.createUnmarshaller();

        return (PreferencesModel) um.unmarshal(f);
    }

    public static void testModels() throws JAXBException {
        Marshaller mr = c.createMarshaller();
        mr.setProperty("jaxb.formatted.output", true);
        mr.marshal(mod, System.out);

        printSeparator();

        printModel(mod);

        printSeparator();
    }

    private static void printSeparator() {
        System.out.println("-------------------------------------------------");
    }

    public static void printModel(PreferencesModel m) throws JAXBException {
        char sep = '\t';

        List<Preference> prefs = m.getPrefs();
        for (Preference pr : prefs) {
            System.out.println(pr.getName() + sep + pr.getDefaultValue() + sep
                    + pr.getI18nKey() + sep + pr.getType() + sep
                    + pr.getValue() + " !");
        }

        System.out.println();

        List<Tab> tabs = m.getTabs();
        for (Tab tb : tabs) {
            System.out.println(tb.getName());
            List<PreferenceSelector> psl = tb.getPrefs();
            for (PreferenceSelector s : psl) {
                System.out.println(sep + s.getName());
            }
        }
    }

    /*
     * public static void printTestModel() throws JAXBException {
     * PreferencesModel m = getTestModel();
     * 
     * JAXBContext c = JAXBContext.newInstance(PreferencesModel.class);
     * Marshaller mr = c.createMarshaller();
     * mr.setProperty("jaxb.formatted.output", true); mr.marshal(m, System.out); }
     * 
     * public static PreferencesModel getTestModel() { PreferencesModel m = new
     * PreferencesModel();
     * 
     * Tab t0 = new Tab(); t0.setName("General"); PreferenceSelector ps0 = new
     * PreferenceSelector(); ps0.setName("useTray"); PreferenceSelector ps1 =
     * new PreferenceSelector(); ps1.setName("pathToXPilotExecutable"); List<PreferenceSelector>
     * lps0 = new ArrayList<PreferenceSelector>(); lps0.add(ps0);
     * lps0.add(ps1); t0.setPrefs(lps0);
     * 
     * Tab t1 = new Tab(); t1.setName("I18n"); PreferenceSelector ps2 = new
     * PreferenceSelector(); ps2.setName("language"); List<PreferenceSelector>
     * lps1 = new ArrayList<PreferenceSelector>(); lps1.add(ps2);
     * t1.setPrefs(lps1);
     * 
     * List<Tab> ts = new ArrayList<Tab>(); ts.add(t0); ts.add(t1);
     * m.setTabs(ts);
     * 
     * Preference p0 = new Preference(); p0.setName("useTray");
     * p0.setDefaultValue("false"); p0.setType("boolean");
     * 
     * Preference p1 = new Preference(); p1.setName("pathToXPilotExecutable");
     * p1.setValue("15,25");
     * p1.setI18nKey("connectWindow.label.temporaryPathToExecutable");
     * 
     * Preference p2 = new Preference(); p2.setName("language");
     * p2.setDefaultValue("english"); p2.setType("comboBox");
     * p2.setValue("english,ukrainian,german,russian");
     * 
     * List<Preference> l = new ArrayList<Preference>(); l.add(p0); l.add(p1);
     * l.add(p2); m.setPrefs(l);
     * 
     * return m; }
     */

}
