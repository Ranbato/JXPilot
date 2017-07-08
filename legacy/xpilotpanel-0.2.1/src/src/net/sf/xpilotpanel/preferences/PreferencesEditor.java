// $Id: PreferencesEditor.java,v 1.11 2008/08/03 12:16:00 taraskostiak Exp $

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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.JComboBox;

import net.sf.xpilotpanel.XPilotPanelDispatcher;
import net.sf.xpilotpanel.gui.AboutWindow;
import net.sf.xpilotpanel.i18n.XPPI18N;
import net.sf.xpilotpanel.preferences.model.Preference;
import net.sf.xpilotpanel.preferences.model.PreferenceSelector;
import net.sf.xpilotpanel.preferences.model.Tab;

/**
 * This class start GUI for editing <code>Preferences</code>.
 * 
 * @author Taras Kostiak
 * 
 */
public class PreferencesEditor extends JDialog {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 4940153132821088306L;

    /**
     * <code>Preferences</code>, that this window edits.
     */
    private Preferences prefs = null;

    /**
     * All <code>JComponent</code>'s that are built from
     * <code>PreferencesModel</code>, are stored here for parse back, when
     * button "Save" is pressed.
     */
    private Map<String, JComponent> dataContainers = null;

    /**
     * Content panel.
     */
    private JPanel cp = null;

    /**
     * If this thread will be interrupted after <code>PreferencesEditor</code>
     * will be closed(with both "Save" and "Cancel").
     */
    private Thread interruptThread = null;

    /**
     * Alternative i18n source to use.
     */
    private XPPI18N alternativeI18N = null;

    /**
     * Alternative namespace to use for i18n #{@link XPPI18N#get(String, String)}.
     */
    private String alternativeI18NNameSpace = null;

    /**
     * Distance between label and input field or previous label and next label.
     */
    private static final int componentDistance = 15;

    /**
     * This constructor build the <code>Preferences</code> editing window,
     * with given parameters.
     * 
     * @param prefs
     *            The <code>Preferences</code> to edit.
     * @param alternativeI18N
     *            I18N source to use.
     * @param alternativeI18NNameSpace
     *            I18N namespace to use.
     */
    public PreferencesEditor(Preferences prefs, XPPI18N alternativeI18N,
            String alternativeI18NNameSpace) {
        super((JFrame) null, XPPI18N.get().get("gui",
                "mainWindow.menu.preferences"), true);

        this.prefs = prefs;
        this.alternativeI18N = alternativeI18N;
        this.alternativeI18NNameSpace = alternativeI18NNameSpace;

        dataContainers = new HashMap<String, JComponent>();

        setResizable(false);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                closePreferencesEditor(false);
            }
        });

        setIconImage(AboutWindow.getIcon());

        cp = buildContentPane();
        setContentPane(cp);

        pack();
    }

    /**
     * This constructor build the <code>Preferences</code> editing window,
     * with given parameters, using default XPilotPanel I18N(may not be suitable
     * if used as library for other project).
     * 
     * @param prefs
     *            The <code>Preferences</code> to edit.
     */
    public PreferencesEditor(Preferences prefs) {
        this(prefs, null, null);
    }

    /**
     * Builds content panel for this window, as defined by an
     * <code>PreferenceModel</code> given to constructor.
     * 
     * @return Built content panel.
     */
    private JPanel buildContentPane() {
        JPanel cPa = new JPanel();
        cPa.setLayout(new BoxLayout(cPa, BoxLayout.Y_AXIS));

        JTabbedPane tp = new JTabbedPane(JTabbedPane.TOP);
        {
            List<Tab> tabs = prefs.getModel().getTabs();

            for (Tab t : tabs) {
                List<PreferenceSelector> pSelectors = t.getPrefs();

                JPanel tabPanel = new JPanel();
                GridLayout tpl = new GridLayout(0, 2, componentDistance,
                        componentDistance);
                tabPanel.setLayout(tpl);

                for (PreferenceSelector pSelector : pSelectors) {
                    Preference p = prefs.getModel().getPreferenceBySelector(
                            pSelector);

                    if (p == null) // if no such preference in model(only
                        // selector with such name)
                        continue;

                    JLabel name = new JLabel();
                    name.setText((alternativeI18NUsed()) ? (alternativeI18N
                            .get(alternativeI18NNameSpace, p.getI18nKey()))
                            : (XPPI18N.get().get("gui", p.getI18nKey())));
                    tabPanel.add(name);

                    if (p.getType().equals(Preference.TYPE_TEXT_FIELD)) {
                        JTextField input = new JTextField();
                        input.setText(prefs.get(p.getName()));
                        addDataContainer(p.getName(), input);

                        tabPanel.add(input);
                    }
                    else
                        if (p.getType().equals(Preference.TYPE_BOOLEAN)) {
                            JCheckBox input = new JCheckBox();
                            input.setSelected((prefs.get(p.getName())
                                    .equals("true")) ? true : false);

                            addDataContainer(p.getName(), input);

                            tabPanel.add(input);
                        }
                        else
                            if (p.getType().equals(Preference.TYPE_COMBO_BOX)) {
                                JComboBox input = new JComboBox();

                                String[] vals = p.getValue().split(",");
                                for (String s : vals) {
                                    input.addItem(s);

                                    if (s.equals(prefs.get(p.getName())))
                                        input.setSelectedItem(s);
                                }

                                input.setEditable(false);

                                addDataContainer(p.getName(), input);

                                tabPanel.add(input);
                            }
                            else
                                if (p.getType().equals(
                                        Preference.TYPE_FILE_PATH)) {
                                    JTextField pathTextField = new JTextField();
                                    pathTextField.setText(prefs
                                            .get(p.getName()));
                                    addDataContainer(p.getName(), pathTextField);

                                    JButton chooseButton = new JButton(
                                            XPPI18N
                                                    .get()
                                                    .get("gui",
                                                            "preferencesEditor.pathChooseButton"));
                                    chooseButton
                                            .addActionListener(new PathChooseButtonListener(
                                                    pathTextField, this));

                                    JPanel pathAndButton = new JPanel();
                                    BoxLayout pathAndButtonPanelLayout = new BoxLayout(
                                            pathAndButton, BoxLayout.X_AXIS);
                                    pathAndButton
                                            .setLayout(pathAndButtonPanelLayout);
                                    pathAndButton.add(pathTextField);
                                    pathAndButton.add(chooseButton);

                                    tabPanel.add(pathAndButton);
                                }
                }

                Dimension tpD = tpl.preferredLayoutSize(tabPanel);
                tabPanel.setMinimumSize(tpD);
                tabPanel.setMaximumSize(tpD);
                tabPanel.setSize(tpD);

                JPanel pn = new JPanel();
                pn.setLayout(new BoxLayout(pn, BoxLayout.Y_AXIS));
                pn.add(tabPanel);
                pn.add(Box.createVerticalGlue());

                tp.addTab((alternativeI18NUsed()) ? (alternativeI18N.get(
                        alternativeI18NNameSpace, t.getI18nKey())) : (XPPI18N
                        .get().get("gui", t.getI18nKey())), pn);
            }
        }

        JPanel bp = new JPanel();
        {
            bp.setLayout(new BoxLayout(bp, BoxLayout.X_AXIS));

            JButton saveButton = new JButton();
            saveButton.setText(XPPI18N.get().get("gui",
                    "preferencesEditor.saveButton"));
            saveButton.setSelected(true);
            saveButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    closePreferencesEditor(true);
                }
            });

            JButton cancelButton = new JButton();
            cancelButton.setText(XPPI18N.get().get("gui",
                    "preferencesEditor.cancelButton"));
            cancelButton.setSelected(false);
            cancelButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    closePreferencesEditor(false);
                }
            });

            bp.add(Box.createHorizontalGlue());
            bp.add(saveButton);
            bp.add(Box.createRigidArea(new Dimension(3, 0)));
            bp.add(cancelButton);
        }

        cPa.add(tp);
        cPa.add(bp);

        return cPa;
    }

    /**
     * Closes <code>PreferenceEditor</code>.
     * 
     * @param saveOnClose
     *            If true, preferences are saved.
     */
    private void closePreferencesEditor(boolean saveOnClose) {
        if (saveOnClose)
            saveOnClose();
        XPilotPanelDispatcher.getDispatcher().removeWindow(this);
        if (interruptThread != null)
            interruptThread.interrupt();
        dispose();
    }

    /**
     * Adds the "data container".
     * 
     * @see #dataContainers
     * 
     * @param name
     *            Identifier of "data container", should be the same as name of
     *            one of preferences in <code>PreferenceModel</code>.
     * @param c
     *            Reference to "data container".
     */
    private void addDataContainer(String name, JComponent c) {
        dataContainers.put(name, c);
    }

    /**
     * Return "data container", by specified name.
     * 
     * @see #dataContainers
     * 
     * @param name
     *            Name of "data container" to return.
     * @return Stored "data container".
     */
    private JComponent getDataContainer(String name) {
        return dataContainers.get(name);
    }

    /**
     * Saves edited preferences to <code>Preferences</code>.
     */
    private void saveOnClose() {
        Set<String> keys = dataContainers.keySet();

        List<Preference> pr = prefs.getModel().getPrefs();

        for (String key : keys) {
            String type = null;
            for (Preference p : pr) {
                if (p.getName().equals(key)) {
                    type = p.getType();
                    break;
                }
            }

            String value = null;

            if (type.equals(Preference.TYPE_TEXT_FIELD)) {
                value = ((JTextField) getDataContainer(key)).getText();
            }
            else
                if (type.equals(Preference.TYPE_BOOLEAN)) {
                    value = ((JCheckBox) getDataContainer(key)).isSelected() ? "true"
                            : "false";
                }
                else
                    if (type.equals(Preference.TYPE_COMBO_BOX)) {
                        value = (String) ((JComboBox) getDataContainer(key))
                                .getSelectedItem();
                    }
                    else
                        if (type.equals(Preference.TYPE_FILE_PATH)) {
                            value = ((JTextField) getDataContainer(key))
                                    .getText();
                        }

            if (value != null)
                prefs.set(key, value);
        }
    }

    /**
     * @see #interruptThread
     */
    public Thread getInterruptThread() {
        return interruptThread;
    }

    /**
     * @see #interruptThread
     */
    public void setInterruptThread(Thread interruptThread) {
        this.interruptThread = interruptThread;
    }

    /**
     * Checks if alternative I18N is used.
     * 
     * @return True, if is.
     */
    private boolean alternativeI18NUsed() {
        return ((alternativeI18N != null) && (alternativeI18NNameSpace != null));
    }

    /**
     * Is used for path choose button listener, that it remembers
     * <code>JTextField</code> to which store selected path by
     * <code>JFileChooser</code>.
     * 
     * @author Taras Kostiak
     * 
     */
    private class PathChooseButtonListener implements ActionListener {

        /**
         * Field to store selected path.
         */
        private JTextField storeField = null;

        /**
         * Window that calls this file chooser.
         */
        private Component callingWindow = null;

        /**
         * Creates new <code>PathChooseButtonListener</code>.
         * 
         * @param storeField
         *            Field to store selected path.
         */
        public PathChooseButtonListener(JTextField storeField,
                Component callingWindow) {
            this.storeField = storeField;
            this.callingWindow = callingWindow;
        }

        /**
         * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
         */
        @Override
        public void actionPerformed(ActionEvent e) {
            // TODO: Fix lauching JFileChooser, selecting path with it and
            // storing in "storeField".
            JFileChooser fileChooser = new JFileChooser(System.getProperties()
                    .getProperty("user.home"));
            fileChooser.setDialogTitle(XPPI18N.get().get("gui",
                    "preferencesEditor.pathChooserTitle"));

            if (fileChooser.showOpenDialog(callingWindow) == JFileChooser.APPROVE_OPTION) {
                storeField.setText(fileChooser.getSelectedFile()
                        .getAbsolutePath());
            }
        }
    }

}
