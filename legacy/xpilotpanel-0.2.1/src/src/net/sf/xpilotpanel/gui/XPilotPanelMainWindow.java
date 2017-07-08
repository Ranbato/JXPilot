// $Id: XPilotPanelMainWindow.java,v 1.18 2008/08/14 18:18:00 taraskostiak Exp $

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

package net.sf.xpilotpanel.gui;

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.border.Border;
import javax.swing.border.EtchedBorder;

import net.sf.xpilotpanel.preferences.PreferencesEditor;
import net.sf.xpilotpanel.XPilotPanel;
import net.sf.xpilotpanel.XPilotPanelConfDirectory;
import net.sf.xpilotpanel.XPilotPanelDispatcher;
import net.sf.xpilotpanel.i18n.XPPI18N;
import net.sf.xpilotpanel.meta.MetaInfo;
import net.sf.xpilotpanel.meta.MetaInfoElement;
import net.sf.xpilotpanel.meta.MetaInfoUpdater;

/**
 * Main window for XPilotPanel.
 * 
 * @author Taras Kostiak
 * 
 */
public class XPilotPanelMainWindow extends JFrame {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -6320066569143696438L;

    /**
     * Temporary text area where information about is displayed.
     */
    private JTextArea txtar = null;

    /**
     * Construct main window.
     */
    public XPilotPanelMainWindow() {
        super();

        String windowTitle = null;

        // Getting window title for embedded launch, if was invoked.
        Map<String, ?> embeddedParameters = XPilotPanelDispatcher
                .getDispatcher().getEmbeddedParameters();
        if (embeddedParameters != null) {
            try {
                windowTitle = (String) embeddedParameters
                        .get(XPilotPanel.EMBEDDED_PARAMETER_MAIN_WINDOW_TITLE);
            }
            catch (Exception e) {
                // If there are no such parameter in embedded parameters set or
                // ClassCastException occurred(object set to this key is not
                // String).
                windowTitle = null;
            }
        }

        if (windowTitle == null)
            windowTitle = XPPI18N.get().get("gui", "mainWindow.title");

        setTitle(windowTitle);

        setSize(new Dimension(250, 400));
        setResizable(false);
        setJMenuBar(constructMenu());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                activate(false);
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
                MetaInfoUpdater.getUpdater().stopAutoUpdate();
            }

            @Override
            public void windowActivated(WindowEvent e) {
                activate(true);
            }

        });

        setIconImage(AboutWindow.getIcon());

        setContentPane(constructComponents());
    }

    /**
     * Opens or hides(minimises or if tray present do unvisible) window, doing
     * additional actions(i.e. stopping meta data update).
     * 
     * @param status
     *            <code>True</code> for open, <code>false</code> for close.
     */
    public void activate(boolean status) {
        if (!status) {
            setExtendedState(JFrame.ICONIFIED);
            MetaInfoUpdater.getUpdater().stopAutoUpdate();
        }
        else {
            setVisible(status);
            setExtendedState(JFrame.NORMAL);
            MetaInfoUpdater.getUpdater().startAutoUpdate();
            requestFocus();
        }
    }

    /**
     * Constructs menu for this window.
     * 
     * @return Constructed menu.
     */
    private JMenuBar constructMenu() {
        JMenuBar br = new JMenuBar();

        JMenu mn1 = new JMenu(XPPI18N.get().get("gui", "mainWindow.menu.file"));
        {
            JMenuItem mn1_i1 = new JMenuItem(XPPI18N.get().get("gui",
                    "mainWindow.menu.connect"));

            mn1_i1.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    startConnectWindow();
                }
            });

            JMenuItem mn1_i2 = new JMenuItem(XPPI18N.get().get("gui",
                    "mainWindow.menu.about"));
            mn1_i2.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    startAboutWindow();
                }
            });

            JMenuItem mn1_i3 = new JMenuItem(XPPI18N.get().get("gui",
                    "mainWindow.menu.preferences"));
            mn1_i3.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    Window w = new PreferencesEditor(XPilotPanelConfDirectory
                            .get().getPreferences());
                    XPilotPanelDispatcher.getDispatcher().addWindow(w);
                    w.setVisible(true);
                }
            });

            JMenuItem mn1_i4 = new JMenuItem(XPPI18N.get().get("gui",
                    "mainWindow.menu.exit"));
            mn1_i4.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    XPilotPanelDispatcher.getDispatcher().exit();
                }
            });

            mn1.add(mn1_i1);
            mn1.add(mn1_i2);
            mn1.add(new JSeparator());
            mn1.add(mn1_i3);
            mn1.add(new JSeparator());
            mn1.add(mn1_i4);
        }

        br.add(mn1);
        return br;
    }

    /**
     * Constructs components for this window.
     * 
     * @return Panel with constructed components.
     */
    private JPanel constructComponents() {
        JPanel pn = new JPanel();
        pn.setLayout(new BoxLayout(pn, BoxLayout.Y_AXIS));

        txtar = new JTextArea(); // Should be replaced with JTree!!!
        {
            txtar.setText(XPPI18N.get().get("gui",
                    "mainWindow.textArea.update_in_progress"));
            txtar.setEditable(false);
            txtar.setEnabled(true);
        }

        JPanel buttonPanel = new JPanel();
        {
            buttonPanel.setLayout(new BoxLayout(buttonPanel,
                    BoxLayout.LINE_AXIS));

            Border butBord = BorderFactory
                    .createEtchedBorder(EtchedBorder.LOWERED);

            Border panBord = BorderFactory.createEmptyBorder(3, 3, 3, 3);
            buttonPanel.setBorder(panBord);

            JButton connectButton = new JButton();
            connectButton.setText(XPPI18N.get().get("gui",
                    "mainWindow.buttons.connect"));
            connectButton.setSelected(false);
            connectButton.setEnabled(false);
            connectButton.setBorder(butBord);

            JButton moreButton = new JButton();
            moreButton.setText(XPPI18N.get().get("gui",
                    "mainWindow.buttons.more"));
            moreButton.setSelected(false);
            moreButton.setBorder(butBord);
            moreButton.addActionListener(new ActionListener() {

                public void actionPerformed(ActionEvent e) {
                    startConnectWindow();
                }
            });

            buttonPanel.add(Box.createHorizontalGlue());
            buttonPanel.add(connectButton);
            buttonPanel.add(Box.createRigidArea(new Dimension(3, 0)));
            buttonPanel.add(moreButton);
        }

        pn.add(txtar);
        pn.add(buttonPanel);

        return pn;
    }

    /**
     * Starts <code>ConnectWindow</code>.
     */
    private void startConnectWindow() {
        ConnectWindow w = new ConnectWindow();
        XPilotPanelDispatcher.getDispatcher().addWindow(w);
        w.setVisible(true);
        activate(false);
    }

    /**
     * Starts <code>AboutWindow</code>.
     */
    private void startAboutWindow() {
        AboutWindow w = new AboutWindow();
        XPilotPanelDispatcher.getDispatcher().addWindow(w);
        w.setVisible(true);
    }

    /**
     * Informs <code>XPilotPanelMainWindow</code> that meta information is
     * updated.
     */
    public void informMetaInfoUpdated() {
        MetaInfo nfo = MetaInfoUpdater.getUpdater().getMetaInfo();

        if (nfo != null) {
            List<MetaInfoElement> i = nfo.getMetaInfo();

            List<MetaInfoElement> used = new ArrayList<MetaInfoElement>();

            for (MetaInfoElement e : i)
                if (Integer.parseInt(e.getNumberOfUsers()) > 0)
                    used.add(e);

            MetaInfo.sortList(used);

            StringBuffer bf = new StringBuffer();
            char endl = '\n';
            bf.append(XPPI18N.get().get("gui", "mainWindow.textArea.pl/sr/mp")
                    + endl);
            for (MetaInfoElement e : used) {
                String sep = "/";
                bf.append(e.getNumberOfUsers() + sep + e.getHostname() + sep
                        + e.getMapName() + endl);
            }
            bf.append(endl
                    + XPPI18N.get().get("gui",
                            "mainWindow.textArea.temporaryNote.part1") + endl);
            bf.append(XPPI18N.get().get("gui",
                    "mainWindow.textArea.temporaryNote.part2")
                    + endl);
            bf.append(XPPI18N.get().get("gui",
                    "mainWindow.textArea.temporaryNote.part3")
                    + endl);

            txtar.setText(bf.toString());
        }
    }
}
