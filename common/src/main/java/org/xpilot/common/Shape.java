package org.xpilot.common;

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
    static final public int MAX_SHIP_PTS2 = (MAX_SHIP_PTS * 2);
    static final public int MAX_GUN_PTS = 3;
    static final public int MAX_LIGHT_PTS = 3;
    static final public int MAX_RACK_PTS = 4;

    /* Defines wire-obj, i.e. ship */
    Click 	pts[][] = new Click[MAX_SHIP_PTS2][RES];	/* the shape rotated many ways */
    int		num_points;		/* total points in object */
    int		num_orig_points;	/* points before SSHACK */
    Click 	cashed_pts[] = new Click[MAX_SHIP_PTS2];
    int		cashed_dir;
    }
