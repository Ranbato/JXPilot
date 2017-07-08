package net.sf.jxpilot.game;

public class NewCannon extends CannonHolder {
	/**
	 * Measured in blocks.
	 */
	public final int x, y;
	public final CannonType type;
	
	public NewCannon(CannonType type, int num, int x, int y) {
		this.type = type;
		super.num = num;
		this.x = x;
		this.y = y;
	}
	
	public int getX(){return x;}
	public int getY(){return y;}
	public CannonType getCannonType(){return type;}
	
}