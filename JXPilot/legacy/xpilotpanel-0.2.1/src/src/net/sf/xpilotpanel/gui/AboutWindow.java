// $Id: AboutWindow.java,v 1.8 2008/10/26 10:26:44 taraskostiak Exp $

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
import java.awt.Image;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.net.URL;
import java.util.Map;

import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.xpilotpanel.XPilotPanel;
import net.sf.xpilotpanel.XPilotPanelDispatcher;

/**
 * Window that shows "about" information.
 * 
 * @author Taras Kostiak
 * 
 */
public class AboutWindow extends JDialog {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -6822549903212808782L;

    /**
     * XPilotPanel logo.
     */
    private static ImageIcon logo = null;

    /**
     * XPilotPanel icon.
     */
    private static Image icon = null;

    /**
     * Retuns XPilotPanel logo, retrieved from file and stored as 'single'.
     * 
     * @return XPilotPanel logo.
     */
    public static ImageIcon getLogo() {
        if (logo == null) {
            URL u = AboutWindow.class.getClassLoader().getResource(
                    "data/XPilotPanel.jpg");
            logo = new ImageIcon(u);
        }

        return logo;
    }

    /**
     * Retuns XPilotPanel window's icon(or on embedded launch - icon set for all
     * windows), retrieved from file and stored as 'single'.
     * 
     * @return XPilotPanel icon.
     */
    public static Image getIcon() {
        if (icon == null) {
            Map<String, ?> embeddedParameters = XPilotPanelDispatcher
                    .getDispatcher().getEmbeddedParameters();
            if (embeddedParameters != null) {
                try {
                    icon = (Image) embeddedParameters
                            .get(XPilotPanel.EMBEDDED_PARAMETER_ICON);
                }
                catch (Exception e) {
                    // If there are no such parameter in embedded parameters set
                    // or
                    // ClassCastException occurred(object set to this key is not
                    // Image).
                    icon = null;
                }
            }

            // Loading default icon if there are no embedded parameters or
            // failed to load icon from them.
            if (icon == null) {
                URL u = AboutWindow.class.getClassLoader().getResource(
                        "data/icon.png");
                icon = (new ImageIcon(u)).getImage();
            }
        }

        return icon;
    }

    /**
     * Reference to same object. Is used in anonymys classes to access this
     * object.
     */
    private Window ths = this;

    /**
     * Constructs new <code>AboutWindow</code>.
     */
    public AboutWindow() {
        super((JFrame) null, "About", false);
        // setSize(250, 200);
        setResizable(false);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                XPilotPanelDispatcher.getDispatcher().removeWindow(ths);
                dispose();
            }
        });

        setIconImage(getIcon());

        JPanel pn = new JPanel();
        pn.setLayout(new BoxLayout(pn, BoxLayout.PAGE_AXIS));
        JLabel logoLabel = new JLabel(getLogo());
        pn.add(logoLabel);
        pn.add(new JLabel("XPilotPanel v. 0.2.1"));
        pn.add(new JLabel("Copyright (C) Taras Kostiak, 2007-2008."));
        pn.add(new JLabel(
                "Under the terms of GNU General Public License v.2 or later."));

        // JTextArea aboutArea = new JTextArea();
        // aboutArea.setText("Copyriht (C) Taras Kostiak, 2007.\n"
        // + "Under the terms of GNU General Public License.");
        // aboutArea.setEditable(false);
        // pn.add(aboutArea);

        setContentPane(pn);
        pack();

        // This centers logo.
        Dimension newLogoLabelSize = new Dimension(getWidth(), logoLabel
                .getHeight());
        logoLabel.setMinimumSize(newLogoLabelSize);
        logoLabel.setSize(newLogoLabelSize);
        logoLabel.setMaximumSize(newLogoLabelSize);
    }

}
