package org.xpilot.client.swing;

import org.xpilot.common.ShipShape;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by z002m6r on 7/7/17.
 */
public class MainForm
{
    private JPanel arenaPanel;
    private JPanel statusPanel;
    private JButton button1;
    private JPanel mainPanel;
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
        button1.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ShipShape ship = new ShipShape();
              for (int i = 0;i<shapeList.size();i++) {
                    String shape = shapeList.get(i);
                    ship.do_parse_shape(shape);

                    ship.drawShip(arenaPanel,0);
                       break;
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

//    static void main(String[] args){
//        JFrame frame = new JFrame("Test Panel");
//        frame.setContentPane(new MainForm());
//        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
//        frame.pack();
//        frame.setVisible(true);
//
//    }
}
