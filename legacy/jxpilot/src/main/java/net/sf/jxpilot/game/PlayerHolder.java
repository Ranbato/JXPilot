package net.sf.jxpilot.game;

import net.sf.jxpilot.util.Holder;

/**
 * Holds player data.
 * @author Vlad Firoiu
 */
public class PlayerHolder implements Holder<PlayerHolder> {
	protected short id;
	protected byte my_team, my_char;
	protected String name, real, host;
	protected ShipShape ship_shape;
	
	public short getId(){return id;}
	public byte getTeam(){return my_team;}
	public byte getChar(){return my_char;}
	public String getName(){return name;}
	public String getReal(){return real;}
	public String getHost(){return host;}
	public ShipShape getShipShape(){return ship_shape;}
	
	public void setPlayer(short id, byte my_team , byte my_char, String name, String real, String host, ShipShape ship_shape) {
		this.id = id;
		this.my_team = my_team;
		this.my_char = my_char;
		this.name = name;
		this.real = real;
		this.host = host;
		this.ship_shape = ship_shape;
	}
	
	public void setFrom(PlayerHolder other) {
		this.setPlayer(other.id, other.my_team, other.my_char, other.name, other.real, other.host, other.ship_shape);
	}
	
	@Override
	public void set(PlayerHolder other) {
		other.setPlayer(id, my_team, my_char, name, real, host, ship_shape);
	}
	
	@Override
	public void setFrom(Holder<PlayerHolder> other) {
		other.set(this);
	}
	
	@Override
	public String toString() {
		return "Player\nid = " + id +
				"\nmy team = " + my_team +
				"\nmy char = " + my_char +
				"\nname = " + name +
				"\nreal = " + real +
				"\nhost = " + host;
	}
}
