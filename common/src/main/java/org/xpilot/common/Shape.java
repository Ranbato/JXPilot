package org.xpilot.common;

import java.awt.*;
import java.awt.geom.Area;
import java.util.ArrayList;

import static org.xpilot.common.Const.RES;

/**
 * Created by mark on 7/3/2017.
 */
public class Shape {
    /*
     * Please don't change any of these maxima.
     * It will create incompatibilities and frustration.
     */
    static final public int MIN_SHIP_PTS = 3;
    static final public int MAX_SHIP_PTS = 24;
    /* SSHACK needs to double the vertices */
//    static final public int MAX_SHIP_PTS2 = (MAX_SHIP_PTS * 2);
    static final public int MAX_GUN_PTS = 3;
    static final public int MAX_LIGHT_PTS = 3;
    static final public int MAX_RACK_PTS = 4;

    /* Defines wire-obj, i.e. ship */
    protected ArrayList<Click> pts = new ArrayList<>(MAX_SHIP_PTS);	/* the shape rotated many ways is now stored in shipshape*/
//    protected int		num_points;		/* total points in object */
//    protected int		num_orig_points;	/* points before SSHACK */

    }
