// $Id: XPilotPanelWarningDialog.java,v 1.3 2008/08/03 12:16:00 taraskostiak Exp $

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

import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;

import net.sf.xpilotpanel.XPilotPanelDispatcher;
import net.sf.xpilotpanel.i18n.XPPI18N;

/**
 * This class manages warning dialogs.
 * 
 * @author Taras Kostiak
 * 
 */
public class XPilotPanelWarningDialog extends JDialog {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -1080595814814921392L;

    /**
     * Message to show.
     */
    protected String message = null;

    /**
     * Creates new modal dialog with warning.
     * 
     * @param owner
     * @param message
     */
    public XPilotPanelWarningDialog(Frame owner, String message) {
        super(owner, true);

        this.message = message;

        createContent();
    }

    /**
     * Builds content of this dialog.
     */
    private void createContent() {
        setTitle(XPPI18N.get().get("gui", "warningDialog.title"));

        setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                closeDialog();
            }
        });

        setIconImage(AboutWindow.getIcon());

        JPanel contentPanel = new JPanel();

        BoxLayout layout = new BoxLayout(contentPanel, BoxLayout.Y_AXIS);
        contentPanel.setLayout(layout);

        JLabel warningLabel = new JLabel();
        warningLabel.setText(message);

        JButton closeButton = new JButton();
        closeButton.setText(XPPI18N.get().get("gui",
                "connectWindow.button.close"));
        closeButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                closeDialog();
            }
        });

        contentPanel.add(warningLabel);
        contentPanel.add(closeButton);

        setContentPane(contentPanel);

        setResizable(false);

        pack();
    }

    /**
     * Closes this dialog and removes it from XPilotPanel dispatcher.
     */
    private void closeDialog() {
        XPilotPanelDispatcher.getDispatcher().removeWindow(this);
        dispose();
    }

}
