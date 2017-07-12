package org.xpilot.common;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * Created by z002m6r on 7/11/17.
 */
public class ShipShapeTest
{
    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    static List<String> shapeList = new ArrayList<>(400);
    @BeforeClass
    public static void load_shipshapes() throws IOException {
        File temp = new File(".");
      List<String> shapeStrings =  Files.readAllLines(new File("src/main/resources/shipshapes.txt").toPath());
        for (String shape:shapeStrings
             ) {
            shapeList.add(shape.replace("xpilot.shipShape:",""));
        }
    }
    @Test
    public void rotate_ship() throws Exception
    {
    }

    @Test
    public void default_ship() throws Exception
    {
    }



    @Test
    public void do_parse_shape() throws Exception
    {
    }

    @Test
    public void parse_shape_str() throws Exception
    {
    }

    @Test
    public void convert_shape_str() throws Exception
    {
        ShipShape ship = new ShipShape();
        ship.debugShapeParsing = true;
        ship.verboseShapeParsing = true;
        ship.do_parse_shape(shapeList.get(0));
    }

    @Test
    public void validate_shape_str() throws Exception
    {
    }

    @Test
    public void convert_ship_2_string() throws Exception
    {
        ShipShape ship = new ShipShape();
        ship.debugShapeParsing = true;
        ship.verboseShapeParsing = true;
        for (int i = 0;i<shapeList.size();i++)
        {
            String shape = shapeList.get(i);
            ship.do_parse_shape(shape);
            String[] ss = ship.Convert_ship_2_string(0x3200);

            int pos = shape.indexOf("(SH");
            assertEquals("Shipshape " + i + " did not survive round-trip", shape.substring(pos), ss[0]);
            assertEquals("Ext " + i + " was created", "", ss[1]);
        }
    }

    @Test
    public void calculate_shield_radius() throws Exception
    {
    }

}