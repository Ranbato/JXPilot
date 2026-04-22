// $Id: ConnectWindow.java,v 1.25 2008/10/25 15:42:13 taraskostiak Exp $

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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;

import net.sf.xpilotpanel.XPilotPanel;
import net.sf.xpilotpanel.XPilotPanelConfDirectory;
import net.sf.xpilotpanel.XPilotPanelDispatcher;
import net.sf.xpilotpanel.i18n.XPPI18N;
import net.sf.xpilotpanel.meta.MetaInfo;
import net.sf.xpilotpanel.meta.MetaInfoElement;
import net.sf.xpilotpanel.meta.MetaInfoUpdater;

/**
 * This window shows in different tabs: <br>
 * 
 * <ul>
 * <li>internet servers</li>
 * <li>local servers</li>
 * </ul>
 * 
 * @author Taras Kostiak
 * 
 */
public class ConnectWindow extends JFrame {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 7207063539661562692L;

    private static final Vector<String> internetServersCollumnNames = new Vector<String>();

    JTextField serverAddress = null;

    JTextField serverPort = null;

    JTable internetServersTable = null;

    /**
     * Stores <code>MetaInfo</code>, from which server table was generated.
     */
    MetaInfo currentMetaInfo = null;

    /**
     * Number of server in list currently selected.<br>
     * <code>-1</code> stands for nothing selected.
     */
    int currentSelectedServer = -1;

    /**
     * Indicates if server or port was changed manually.
     */
    boolean serverOrPortChangedManually = false;

    /**
     * Constructs new <code>ConnectWindow</code>.
     */
    public ConnectWindow() {
        super(XPPI18N.get().get("gui", "connectWindow.title"));

        if (internetServersCollumnNames.size() == 0) {
            internetServersCollumnNames.add(XPPI18N.get().get("gui",
                    "connectWindow.internetServers.collumnNames.1"));
            internetServersCollumnNames.add(XPPI18N.get().get("gui",
                    "connectWindow.internetServers.collumnNames.2"));
            internetServersCollumnNames.add(XPPI18N.get().get("gui",
                    "connectWindow.internetServers.collumnNames.3"));
        }

        setSize(new Dimension(900, 400));
        setResizable(false);

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                closeConnectWindow(true);
            }
        });

        setIconImage(AboutWindow.getIcon());

        setContentPane(constructComponents());

        refresh();
    }

    /**
     * Constructs content for <code>ConnectWindow</code>.
     * 
     * @return JPanel, with content for <code>ConnectWindow</code>.
     */
    private JPanel constructComponents() {
        JPanel contPanel = new JPanel();
        contPanel.setLayout(new BoxLayout(contPanel, BoxLayout.Y_AXIS));

        JTabbedPane tbPane = new JTabbedPane(JTabbedPane.RIGHT);
        {
            JPanel internetServersTab = new JPanel();
            {
                internetServersTab.setLayout(new BorderLayout());

                internetServersTable = new JTable(new ConnectWindowTableModel());
                internetServersTable
                        .setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
                internetServersTable.getSelectionModel()
                        .addListSelectionListener(new ListSelectionListener() {

                            public void valueChanged(ListSelectionEvent e) {
                                if (e.getValueIsAdjusting())
                                    return;

                                ListSelectionModel lsm = (ListSelectionModel) e
                                        .getSource();
                                if (lsm.isSelectionEmpty()) {
                                    selectNothing();
                                }
                                else {
                                    selectServer(lsm.getMinSelectionIndex());
                                }
                            }
                        });

                JScrollPane sp = new JScrollPane(internetServersTable);

                internetServersTab.add(sp, BorderLayout.CENTER);
            }

            JPanel localServersTab = new JPanel();
            {
                localServersTab.setLayout(new BorderLayout());

                JLabel lb = new JLabel();
                lb.setText("No local servers availible yet!!!");

                localServersTab.add(lb, BorderLayout.CENTER);
            }

            tbPane.addTab(XPPI18N.get().get("gui",
                    "connectWindow.internetServers.tabName"),
                    internetServersTab);
            tbPane.addTab(XPPI18N.get().get("gui",
                    "connectWindow.localServers.tabName"), localServersTab);
            tbPane.setEnabledAt(1, false);
        }

        JPanel buttonPanel = new JPanel();
        {
            buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.X_AXIS));

            JLabel infoLb = new JLabel(XPPI18N.get().get("gui",
                    "connectWindow.label.server/port"));

            KeyListener serverAndPortKeyListener = new KeyAdapter() {

                @Override
                public void keyTyped(KeyEvent e) {
                    serverOrPortChangedManually = true;
                }
            };

            serverAddress = new JTextField();
            serverAddress.setColumns(20);
            serverAddress.addKeyListener(serverAndPortKeyListener);

            serverPort = new JTextField();
            serverPort.setColumns(5);
            serverPort.addKeyListener(serverAndPortKeyListener);

            JButton connectButton = new JButton();
            connectButton.setText(XPPI18N.get().get("gui",
                    "connectWindow.button.connect"));
            connectButton.setSelected(false);
            connectButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    connect();
                }
            });

            JButton refreshButton = new JButton();
            refreshButton.setText(XPPI18N.get().get("gui",
                    "connectWindow.button.refresh"));
            refreshButton.setSelected(false);
            refreshButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    refresh();
                }
            });

            JButton serverInfoButton = new JButton(XPPI18N.get().get("gui",
                    "connectWindow.button.serverInfo"));
            serverInfoButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    if (currentSelectedServer >= 0
                            && currentSelectedServer < currentMetaInfo
                                    .getSortedMetaInfo().size()) {
                        MetaInfoElementWindow infoWindow = new MetaInfoElementWindow(
                                currentMetaInfo.getSortedMetaInfo().get(
                                        currentSelectedServer));
                        XPilotPanelDispatcher.getDispatcher().addWindow(
                                infoWindow);
                        infoWindow.setVisible(true);
                    }
                }
            });

            JButton closeButton = new JButton(XPPI18N.get().get("gui",
                    "connectWindow.button.close"));
            closeButton.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    closeConnectWindow(true);
                }
            });

            buttonPanel.add(infoLb);
            buttonPanel.add(Box.createRigidArea(new Dimension(25, 25)));
            // buttonPanel.add(Box.createHorizontalGlue());
            buttonPanel.add(serverAddress);
            buttonPanel.add(serverPort);
            buttonPanel.add(Box.createRigidArea(new Dimension(25, 25)));
            buttonPanel.add(serverInfoButton);
            buttonPanel.add(Box.createRigidArea(new Dimension(25, 25)));
            // buttonPanel.add(Box.createHorizontalGlue());
            buttonPanel.add(connectButton);
            buttonPanel.add(refreshButton);
            buttonPanel.add(closeButton);
            buttonPanel.setMaximumSize(new Dimension(900, 35));
        }

        // JPanel temporarySettingsPanel = new JPanel();
        // {
        // temporarySettingsPanel.setLayout(new BoxLayout(
        // temporarySettingsPanel, BoxLayout.X_AXIS));
        //
        // JLabel path = new JLabel(XPPI18N.get().get("gui",
        // "connectWindow.label.temporaryPathToExecutable"));
        //
        // pathField = new JTextField();
        //
        // temporarySettingsPanel.add(path);
        // temporarySettingsPanel.add(Box
        // .createRigidArea(new Dimension(25, 25)));
        // temporarySettingsPanel.add(pathField);
        //
        // temporarySettingsPanel.setMaximumSize(new Dimension(600, 35));
        // }

        contPanel.add(tbPane);
        contPanel.add(buttonPanel);
        // contPanel.add(temporarySettingsPanel);

        return contPanel;
    }

    /**
     * Closes ConnectWindow and if specified activates main window.
     * 
     * @param activateMainWindow
     *            True, to activate main window.
     */
    private void closeConnectWindow(boolean activateMainWindow) {
        XPilotPanelDispatcher.getDispatcher().removeWindow(this);
        if (activateMainWindow) {
            XPilotPanelDispatcher.getDispatcher().getMainWindow()
                    .activate(true);
        }
        dispose();
        XPilotPanelDispatcher.getDispatcher().getMainWindow().requestFocus();
    }

    /**
     * Closes ConnectWindow and shows warning.
     * 
     * @param warning
     *            Warning to show.
     */
    private void closeConnectWindowAndWarn(String warning) {
        showWarning(warning);

        closeConnectWindow(true);
    }

    /**
     * Shows dialog with warning.
     * 
     * @param warning
     *            Warning to show in dialog.
     */
    public static void showWarning(String warning) {
        XPilotPanelWarningDialog warnDialog = new XPilotPanelWarningDialog(
                XPilotPanelDispatcher.getDispatcher().getMainWindow(), warning);

        XPilotPanelDispatcher.getDispatcher().addWindow(warnDialog);

        warnDialog.setVisible(true);
    }

    private void connect() {
        if ((currentSelectedServer < 0 || currentSelectedServer >= currentMetaInfo
                .getSortedMetaInfo().size())
                && !serverOrPortChangedManually) {
            showWarning(XPPI18N.get().get("gui",
                    "connectWindow.warning.serverNotSelected"));
            return;
        }

        String serverToConnect = null;
        String portToConnect = null;

        if (serverOrPortChangedManually) {
            if (serverAddress.getText().isEmpty()
                    || serverPort.getText().isEmpty()) {
                showWarning(XPPI18N.get().get("gui",
                        "connectWindow.warning.serverNotSelected"));
                return;
            }
            else {
                serverToConnect = serverAddress.getText();
                portToConnect = serverPort.getText();
            }
        }
        else {
            serverToConnect = currentMetaInfo.getSortedMetaInfo().get(
                    currentSelectedServer).getIpNumber();
            portToConnect = currentMetaInfo.getSortedMetaInfo().get(
                    currentSelectedServer).getPortNumber();
        }

        Map<String, ?> embeddedParams = XPilotPanelDispatcher.getDispatcher()
                .getEmbeddedParameters();

        XPilotClientWaitThread clientWaitThread = null;

        if (embeddedParams == null
                || embeddedParams
                        .get(XPilotPanel.EMBEDDED_PARAMETER_CLIENT_LAUNCH_METHOD) == null) {
            try {
                final char sep = ' ';

                StringBuffer query = new StringBuffer();

                String pathToExecutable = XPilotPanelConfDirectory.get()
                        .getPreferences().get("pathToXPilotExecutable");

                if (pathToExecutable.compareTo("") == 0) {
                    closeConnectWindowAndWarn(XPPI18N
                            .get()
                            .get("gui",
                                    "connectWindow.warning.pathToExecutableNotSpecified"));
                    return;
                }

                File executableFile = new File(pathToExecutable);

                if (!executableFile.exists() || !executableFile.isFile()) {
                    closeConnectWindowAndWarn(XPPI18N.get().get("gui",
                            "connectWindow.warning.noFile"));
                    return;
                }

                String fileSeparator = System.getProperty("file.separator");
                if (fileSeparator.compareTo("/") == 0) { // For linux.
                    query.append("./" + executableFile.getName());
                }
                else
                    if (fileSeparator.compareTo("\\") == 0) { // For windows.
                        final char quote = '\"';
                        query.append(quote + executableFile.getAbsolutePath()
                                + quote);
                    }
                    else { // On OS where path separator isn't one of '/' or
                        // '\'.
                        closeConnectWindowAndWarn(XPPI18N.get().get("gui",
                                "connectWindow.warning.otherError"));
                        return;
                    }

                query.append(sep + "-join" + sep);
                query.append("-port" + sep + portToConnect);
                query.append(sep);

                String appendStr = ""; // Is used for getting XPilotName,
                // XPilotUser, XPilotHost
                String[] names = { "XPilotName", "XPilotUser", "XPilotHost" };
                String[] commandLine = { "-name", "-user", "-host" };

                for (int i = 0; i < names.length; i++) {
                    appendStr = XPilotPanelConfDirectory.get().getPreferences()
                            .get(names[i]);
                    if (appendStr.compareTo("") != 0)
                        query.append(commandLine[i] + sep + appendStr + sep);
                }

                query.append(XPilotPanelConfDirectory.get().getPreferences()
                        .get("otherOptionsForXPilot"));
                query.append(sep);
                query.append(serverToConnect);

                Process xpilotClient = Runtime.getRuntime().exec(
                        query.toString(), null, executableFile.getParentFile());

                clientWaitThread = new XPilotClientWaitThread(xpilotClient);

            }
            catch (IOException e) {
                closeConnectWindowAndWarn(XPPI18N.get().get("gui",
                        "connectWindow.warning.failedToStart")
                        + ": " + e.getMessage());
                return;
            }
        }
        else {
            try {
                clientWaitThread = new XPilotClientWaitThread(
                        (net.sf.xpilotpanel.client.ClientRunner) embeddedParams
                                .get(XPilotPanel.EMBEDDED_PARAMETER_CLIENT_LAUNCH_METHOD),
                        serverToConnect, Integer.parseInt(portToConnect));
            }
            catch (ClassCastException e) {
                ConnectWindow.showWarning((XPPI18N.get().get("gui",
                        "connectWindow.warning.failedToStart")
                        + ": " + e.getMessage()));
            }
        }

        if (clientWaitThread != null) {
            XPilotPanelDispatcher.getDispatcher().addThread(clientWaitThread);

            clientWaitThread.start();

            closeConnectWindow(false);
        }
    }

    /**
     * Refreshes server list table.
     */
    private void refresh() {
        MetaInfoUpdater.getUpdater().updateMetaInfo();
        currentMetaInfo = MetaInfoUpdater.getUpdater().getMetaInfo();

        if (currentMetaInfo != null) {
            List<MetaInfoElement> l = currentMetaInfo.getSortedMetaInfo();

            Vector<Vector<String>> v = new Vector<Vector<String>>();

            for (MetaInfoElement e : l) {
                String sl = " / ";
                Vector<String> ve = new Vector<String>();
                ve.add(e.getHostname() + ':' + e.getPortNumber());
                ve.add(e.getMapName() + sl + e.getVersion());
                ve.add(e.getNumberOfUsers() + sl + e.getNumberOfHomeBases()
                        + sl + e.getNumberOfFreeHomeBases());

                v.add(ve);
            }

            ConnectWindowTableModel m = (ConnectWindowTableModel) internetServersTable
                    .getModel();
            m.setDataVector(v, internetServersCollumnNames);
            selectNothing();
        }
    }

    /**
     * Represents non-editable(by user) table model.
     * 
     * @author Taras Kostiak
     * 
     * @see TableModel
     */
    private class ConnectWindowTableModel extends DefaultTableModel {

        /**
         * Serial version UID.
         */
        private static final long serialVersionUID = 8014961739308652081L;

        /**
         * Return false(i.e. non editable) for any element.
         * 
         * @see javax.swing.table.DefaultTableModel#isCellEditable(int, int)
         */
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }

    /**
     * Selects specified server by number for server and port text fields(as
     * well as for storing current selected server number for ).
     * 
     * @param serverNumber
     *            Server to select(<code>-1</code> to select nothing).
     */
    private void selectServer(int serverNumber) {
        if (serverNumber == -1) {
            serverAddress.setText("");
            serverPort.setText("");
            currentSelectedServer = -1;
        }
        else {
            List<MetaInfoElement> metaInfoElements = currentMetaInfo
                    .getSortedMetaInfo();

            if (serverNumber >= metaInfoElements.size()) {
                selectNothing();
                return;
            }

            serverAddress.setText(metaInfoElements.get(serverNumber)
                    .getHostname());
            serverPort.setText(metaInfoElements.get(serverNumber)
                    .getPortNumber());
            currentSelectedServer = serverNumber;
        }

        serverOrPortChangedManually = false;
    }

    /**
     * Selects no server for server and port text fields(and for other actions
     * with current server as well).
     */
    private void selectNothing() {
        selectServer(-1);
    }

}
