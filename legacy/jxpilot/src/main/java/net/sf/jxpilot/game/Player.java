package net.sf.jxpilot.game;

import java.awt.*;
import net.sf.jgamelibrary.geom.MovingPolygon2D;
import net.sf.jxpilot.util.Utilities;

/**
 * Holds all Player information.
 * @see {@link PlayerHolder}
 * @author Vlad Firoiu
 */
public class Player extends PlayerHolder {
	/**
	 * This id indicates that the player does not exist.
	 */
	public static final short NO_ID = -1;
	
	private short score, life;
	
	//ship data
	/**
	 * Whether or not to draw the ship.
	 */
	private boolean active=false;
	
	private ShipHolder ship;
	/**
	 * Bounds of ship.
	 */
	private MovingPolygon2D shipBounds;
	
	public Player(short id, byte my_team, byte my_char, String nick, String real, String host, ShipShape ship_shape) {
		super.setPlayer(id, my_team, my_char, nick, real, host, ship_shape);
		ship = new ShipHolder();
		shipBounds = new MovingPolygon2D(ship_shape.getShape());
	}
	
	public Player(PlayerHolder p) {
		super.setFrom(p);
		ship = new ShipHolder();
		shipBounds = new MovingPolygon2D(ship_shape.getShape());
	}
	
	public void setScore(short score){this.score = score;}
	public void setLife(short life){this.life = life;}
	
	public short getScore(){return score;}
	public short getLife(){return life;}
	
	//ship data
	//public Polygon getShape(){return ship_shape.getShape();}
	public void setActive(boolean b){active = b;}
	public boolean isActive(){return active;}
	public ShipHolder getShip(){return ship;}
	public MovingPolygon2D getShipBounds(){return shipBounds;}
	
	public void setShip(ShipHolder s) {
		active = true;
		ship.setFrom(s);
		//shipBounds.setPolygon(ship.getX(), ship.getY(), Utilities.getAngleFrom128(s.getHeading()));
	}
}