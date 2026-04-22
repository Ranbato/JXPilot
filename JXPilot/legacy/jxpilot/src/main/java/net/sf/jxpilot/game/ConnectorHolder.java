package net.sf.jxpilot.game;

import net.sf.jxpilot.util.Holder;

/**
 * Defines the data to be held by any Connector object.
 * @author Vlad Firoiu
 */
public class ConnectorHolder implements Holder<ConnectorHolder> {
	protected short x0,y0,x1,y1;
	protected byte tractor;
	
	public ConnectorHolder setConnector(short x0, short y0,short x1, short y1, byte tractor) {	
		this.x0 = x0;
		this.y0 = y0;
		this.x1 = x1;
		this.y1 = y1;
		this.tractor = tractor;
		return this;
	}
	@Override
	public void set(ConnectorHolder other) {
		other.setConnector(x0, y0, x1, y1, tractor);
	}
	@Override
	public void setFrom(Holder<ConnectorHolder> other) {
		other.set(this);
	}
	
	public short getX0(){return x0;}
	public short getX1(){return x1;}
	public short getY0(){return y0;}
	public short getY1(){return y1;}
	public byte getTractor(){return tractor;}
}
