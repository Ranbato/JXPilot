package org.xpilot.client.swing;

import org.xpilot.client.GFX2d;
import org.xpilot.common.ShipShape;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.xpilot.common.Const.RES;

/**
 * Created by z002m6r on 7/7/17.
 */
public class MainForm
{
    private JPanel arenaPanel;
    private JPanel statusPanel;
    private JButton buttonShip;
    private JPanel mainPanel;
    private JButton PPMButton;
    private JButton buttonXPM;
    static List<String> shapeList = new ArrayList<>(400);
    static  {
        File temp = new File(".");
        List<String> shapeStrings = null;
        try {
            File f = new File(".");
            shapeStrings = Files.readAllLines(new File("common/src/main/resources/shipshapes.txt").toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
        for (String shape:shapeStrings
                ) {
            shapeList.add(shape.replace("xpilot.shipShape:",""));
        }
    }
    public MainForm() {
        buttonShip.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ShipShape ship = new ShipShape();
                    String shape = shapeList.get(94);
                    ship.do_parse_shape(shape);

                    for(int i = 0;i<RES*3;i++) {
                        ship.drawShip(arenaPanel, i%RES);
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e1) {
                            e1.printStackTrace();
                        }
                    }

            }
        });
        PPMButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GFX2d gfx2d = new GFX2d();
                GFX2d.XPPicture pic = gfx2d.Picture_init("common/src/main/resources/textures/allitems.ppm",-30);
                ArrayList<BufferedImage> data = pic.getData();
                Graphics2D g2d = (Graphics2D)arenaPanel.getGraphics();
                g2d.scale(2,2);
                for(int i =0;i<data.size();i++) {
                    g2d.drawImage(data.get(i), null, 100, 100+(i*17));
                }
            }
        });
        buttonXPM.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GFX2d gfx2d = new GFX2d();
                GFX2d.XPPicture pic = gfx2d.Picture_init("common/src/main/resources/textures/moon.xpm",1);
                ArrayList<BufferedImage> data = pic.getData();
                Graphics2D g2d = (Graphics2D)arenaPanel.getGraphics();
                g2d.scale(2,2);
                for(int i =0;i<data.size();i++) {
                    g2d.drawImage(data.get(i), null, 100, 100+(i*17));
                }
            }
        });
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame("MainForm");
        frame.setContentPane(new MainForm().mainPanel);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);
    }


}
