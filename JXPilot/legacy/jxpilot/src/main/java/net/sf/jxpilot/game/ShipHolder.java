package net.sf.jxpilot.game;

import net.sf.jxpilot.util.Holder;

/**
 * Holds ship data.
 * @author Vlad Firoiu
 */
public class ShipHolder implements Holder<ShipHolder> {
	protected short x, y, id, heading;
	protected boolean shield, cloak, emergency_shield, phased, deflector;
	
	public ShipHolder setShip(short x, short y, short id, short heading, 
			boolean shield, boolean cloak, boolean emergency_shield, boolean phased, boolean deflector)
	{
		this.x = x;
		this.y =y;
		this.id = id;
		this.heading = heading;
		this.shield=shield;
		this.cloak=cloak;
		this.emergency_shield=emergency_shield;
		this.phased=phased;
		this.deflector=deflector;
		return this;
	}
	
	@Override
	public void set(ShipHolder other) {
		other.setShip(x, y, id, heading, shield, cloak, emergency_shield, phased, deflector);
	}
	
	@Override
	public void setFrom(Holder<ShipHolder> other) {
		other.set(this);
	}
	
	//ship data
	public short getX(){return x;}
	public short getY(){return y;}
	public short getId(){return id;}
	public short getHeading(){return heading;}
	
	public boolean isShielded(){return shield;}
	public boolean isCloaked(){return cloak;}
	public boolean isEmergencyShielded(){return emergency_shield;}
	public boolean isPhased(){return phased;}
	public boolean isDeflectored(){return deflector;}
}
