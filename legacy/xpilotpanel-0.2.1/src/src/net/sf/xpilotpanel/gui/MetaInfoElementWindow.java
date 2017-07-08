// $Id: MetaInfoElementWindow.java,v 1.3 2008/08/03 12:16:00 taraskostiak Exp $

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
import java.awt.Window;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JFrame;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.JLabel;
import javax.swing.JTextField;

import net.sf.xpilotpanel.XPilotPanelDispatcher;
import net.sf.xpilotpanel.i18n.XPPI18N;
import net.sf.xpilotpanel.meta.MetaInfoElement;

/**
 * @author Taras Kostiak
 * 
 */
public class MetaInfoElementWindow extends JFrame {

    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = -7465286271661874361L;

    /**
     * Reference to "this".
     */
    private Window thisWindow = null;

    private JPanel jContentPane = null;

    private MetaInfoElement serverInfo = null;

    /**
     * This is the default constructor
     */
    public MetaInfoElementWindow(MetaInfoElement serverInfo) {
        super();
        thisWindow = this;
        this.serverInfo = serverInfo;
        initialize();
        pack();
    }

    /**
     * This method initializes this
     * 
     * @return void
     */
    private void initialize() {
        this.setLayout(new BorderLayout());
        this.setContentPane(getJContentPane());
        this.setTitle(XPPI18N.get().get("gui", "metaInfoElementWindow.title")
                + ' ' + serverInfo.getHostname() + ':'
                + serverInfo.getPortNumber());
        this.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        this.addWindowListener(new WindowAdapter() {

            public void windowClosing(WindowEvent e) {
                closeMetaInfoElementWindow();
            }
        });
        this.setIconImage(AboutWindow.getIcon());
        this.setResizable(false);
    }

    /**
     * This method initializes jContentPane
     * 
     * @return javax.swing.JPanel
     */
    private JPanel getJContentPane() {
        if (jContentPane == null) {
            GridLayout gridLayout1 = new GridLayout(0, 2);
            gridLayout1.setVgap(4);
            jContentPane = new JPanel();
            jContentPane.setLayout(gridLayout1);

            int textAlignment = JLabel.LEFT;

            { // "Server" / hostname
                JLabel hostnameLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.info.hostname"), textAlignment);

                JTextField hostnameField = new JTextField();
                hostnameField.setText(serverInfo.getHostname());
                hostnameField.setEditable(false);

                jContentPane.add(hostnameLabel, null);
                jContentPane.add(hostnameField, null);
            }

            { // "IP:port" / ipNumber and portNumber
                JLabel hostnameLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.info.ipPort"), textAlignment);

                JTextField hostnameField = new JTextField();
                hostnameField.setText(serverInfo.getIpNumber() + ":"
                        + serverInfo.getPortNumber());
                hostnameField.setEditable(false);

                jContentPane.add(hostnameLabel, null);
                jContentPane.add(hostnameField, null);
            }

            { // "Version" / version
                JLabel hostnameLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.info.version"), textAlignment);

                JTextField hostnameField = new JTextField();
                hostnameField.setText(serverInfo.getVersion());
                hostnameField.setEditable(false);

                jContentPane.add(hostnameLabel, null);
                jContentPane.add(hostnameField, null);
            }

            { // "Players" / numberOfUsers
                JLabel hostnameLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.info.numberOfUsers"),
                        textAlignment);

                JTextField hostnameField = new JTextField();
                hostnameField.setText(serverInfo.getNumberOfUsers());
                hostnameField.setEditable(false);

                jContentPane.add(hostnameLabel, null);
                jContentPane.add(hostnameField, null);
            }

            { // "Map name" / mapName
                JLabel hostnameLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.info.mapName"), textAlignment);

                JTextField hostnameField = new JTextField();
                hostnameField.setText(serverInfo.getMapName());
                hostnameField.setEditable(false);

                jContentPane.add(hostnameLabel, null);
                jContentPane.add(hostnameField, null);
            }

            { // "Map size" / mapName
                JLabel hostnameLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.info.mapSize"), textAlignment);

                JTextField hostnameField = new JTextField();
                hostnameField.setText(serverInfo.getMapSize());
                hostnameField.setEditable(false);

                jContentPane.add(hostnameLabel, null);
                jContentPane.add(hostnameField, null);
            }

            { // "Map author" / mapName
                JLabel hostnameLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.info.mapAuthor"), textAlignment);

                JTextField hostnameField = new JTextField();
                hostnameField.setText(serverInfo.getMapAuthor());
                hostnameField.setEditable(false);

                jContentPane.add(hostnameLabel, null);
                jContentPane.add(hostnameField, null);
            }

            { // "Status" / serverStatus
                JLabel hostnameLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.info.serverStatus"),
                        textAlignment);

                JTextField hostnameField = new JTextField();
                hostnameField.setText(serverInfo.getServerStatus());
                hostnameField.setEditable(false);

                jContentPane.add(hostnameLabel, null);
                jContentPane.add(hostnameField, null);
            }

            { // "Bases" / numberOfHomeBases
                JLabel hostnameLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.info.numberOfHomeBases"),
                        textAlignment);

                JTextField hostnameField = new JTextField();
                hostnameField.setText(serverInfo.getNumberOfHomeBases());
                hostnameField.setEditable(false);

                jContentPane.add(hostnameLabel, null);
                jContentPane.add(hostnameField, null);
            }

            { // "Team bases" / numberOfTeamBases
                JLabel hostnameLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.info.numberOfTeamBases"),
                        textAlignment);

                JTextField hostnameField = new JTextField();
                hostnameField.setText(serverInfo.getNumberOfTeamBases());
                hostnameField.setEditable(false);

                jContentPane.add(hostnameLabel, null);
                jContentPane.add(hostnameField, null);
            }

            { // "Free bases" / numberOfFreeHomeBases
                JLabel hostnameLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.info.numberOfFreeHomeBases"),
                        textAlignment);

                JTextField hostnameField = new JTextField();
                hostnameField.setText(serverInfo.getNumberOfFreeHomeBases());
                hostnameField.setEditable(false);

                jContentPane.add(hostnameLabel, null);
                jContentPane.add(hostnameField, null);
            }

            { // "Queued players" / numberOfQueuedPlayers
                JLabel hostnameLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.info.numberOfQueuedPlayers"),
                        textAlignment);

                JTextField hostnameField = new JTextField();
                hostnameField.setText(serverInfo.getNumberOfQueuedPlayers());
                hostnameField.setEditable(false);

                jContentPane.add(hostnameLabel, null);
                jContentPane.add(hostnameField, null);
            }

            { // "FPS" / framesPerSecond
                JLabel hostnameLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.info.framesPerSecond"),
                        textAlignment);

                JTextField hostnameField = new JTextField();
                hostnameField.setText(serverInfo.getFramesPerSecond());
                hostnameField.setEditable(false);

                jContentPane.add(hostnameLabel, null);
                jContentPane.add(hostnameField, null);
            }

            { // "Sound" / sound
                JLabel hostnameLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.info.sound"), textAlignment);

                JTextField hostnameField = new JTextField();
                hostnameField.setText(serverInfo.getSound());
                hostnameField.setEditable(false);

                jContentPane.add(hostnameLabel, null);
                jContentPane.add(hostnameField, null);
            }

            { // "Timing" / timing
                JLabel hostnameLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.info.timing"), textAlignment);

                JTextField hostnameField = new JTextField();
                String timing = serverInfo.getTiming();
                hostnameField
                        .setText(timing.compareTo("1") == 0 ? "Game is race"
                                : "Not a race");
                hostnameField.setEditable(false);

                jContentPane.add(hostnameLabel, null);
                jContentPane.add(hostnameField, null);
            }

            { // "Server uptime" / serverUptime
                JLabel hostnameLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.info.serverUptime"),
                        textAlignment);

                JTextField hostnameField = new JTextField();
                hostnameField.setText(serverInfo.getServerUptime());
                hostnameField.setEditable(false);

                jContentPane.add(hostnameLabel, null);
                jContentPane.add(hostnameField, null);
            }

            {
                JLabel playerListLabel = new JLabel(XPPI18N.get().get("gui",
                        "metaInfoElementWindow.playerList"));
                JLabel[] emptyLabels = { new JLabel(), new JLabel(),
                        new JLabel() };

                jContentPane.add(emptyLabels[0], null);
                jContentPane.add(emptyLabels[1], null);
                jContentPane.add(playerListLabel, null);
                jContentPane.add(emptyLabels[2], null);

                if (serverInfo.getPlayersList().compareTo("") != 0) {
                    JLabel playerNameLabel = new JLabel("<html><i>"
                            + XPPI18N.get().get("gui",
                                    "metaInfoElementWindow.playerName")
                            + "</i></html>");
                    JLabel playerHostLabel = new JLabel("<html><i>"
                            + XPPI18N.get().get("gui",
                                    "metaInfoElementWindow.playerHost")
                            + "</i></html>");
                    jContentPane.add(playerNameLabel, null);
                    jContentPane.add(playerHostLabel, null);

                    String[] players = serverInfo.getPlayersList().split(",");

                    for (String player : players) {
                        String[] nameHost = player.split("=", 2);

                        JTextField playerNameField = new JTextField();
                        playerNameField.setText(nameHost[0]);
                        playerNameField.setEditable(false);

                        JTextField playerHostField = new JTextField();
                        playerHostField.setText(nameHost[1]);
                        playerHostField.setEditable(false);

                        jContentPane.add(playerNameField, null);
                        jContentPane.add(playerHostField, null);
                    }
                }
                else {
                    JLabel noPlayersLabel1 = new JLabel("<html><i>"
                            + XPPI18N.get().get("gui",
                                    "metaInfoElementWindow.noPlayers.part1")
                            + " </i></html>", JLabel.RIGHT);
                    JLabel noPlayersLabel2 = new JLabel("<html><i>"
                            + XPPI18N.get().get("gui",
                                    "metaInfoElementWindow.noPlayers.part2")
                            + "</i></html>", JLabel.LEFT);
                    jContentPane.add(noPlayersLabel1, null);
                    jContentPane.add(noPlayersLabel2, null);
                }

                {
                    JLabel emptyLabel = new JLabel();
                    JButton closeButton = new JButton(XPPI18N.get().get("gui",
                            "metaInfoElementWindow.button.close"));
                    closeButton.addActionListener(new ActionListener() {

                        @Override
                        public void actionPerformed(ActionEvent e) {
                            closeMetaInfoElementWindow();
                        }
                    });

                    jContentPane.add(emptyLabel, null);
                    jContentPane.add(closeButton, null);
                }
            }
        }
        return jContentPane;
    }

    /**
     * Closes this window and removes from XPilotPanelDispatcher.
     */
    private void closeMetaInfoElementWindow() {
        XPilotPanelDispatcher.getDispatcher().removeWindow(thisWindow);
        dispose();
    }

}
