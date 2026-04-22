package net.sf.jxpilot.game;

import net.sf.jxpilot.util.*;

public class SelfHolder implements Holder<SelfHolder>{

	protected short x, y, vx, vy;
	protected byte heading, power, turnspeed, turnresistance;
	protected short lockId, lockDist;
	protected byte lockDir;
	protected byte nextCheckPoint;
	protected byte currentTank;
	protected short fuelSum, fuelMax,
			ext_view_width, ext_view_height;
	protected byte debris_colors, stat, autopilot_light;

	
	public SelfHolder setSelf(short x, short y, short vx, short vy, byte heading,
			byte power, byte turnspeed, byte turnresistance,
			short lockId, short lockDist, byte lockDir, byte nextCheckPoint,
			byte currentTank, short fuelSum, short fuelMax,
			short ext_view_width, short ext_view_height,
			byte debris_colors, byte stat, byte autopilot_light)
	{
		this.x=x;
		this.y=y;
		this.vx=vx;
		this.vy=vy;
		this.heading=heading;
		this.power=power;
		this.turnspeed=turnspeed;
		this.turnresistance=turnresistance;
		this.lockId = lockId;
		this.lockDist=lockDist;
		this.lockDir=lockDir;
		this.nextCheckPoint=nextCheckPoint;
		this.currentTank=currentTank;
		this.fuelSum=fuelSum;
		this.fuelMax=fuelMax;
		this.ext_view_width=ext_view_width;
		this.ext_view_height=ext_view_height;
		this.debris_colors=debris_colors;
		this.stat=stat;
		this.autopilot_light=autopilot_light;
		return this;
	}
	
	public short getX(){return x;}
	public short getY(){return y;}
	public short getVx(){return vx;}
	public short getVy(){return vy;}
	public byte getHeading(){return heading;}
	public byte getPower(){return power;}
	public byte getTurnSpeed(){return turnspeed;}
	public byte getTurnResistance(){return turnresistance;}
	public short getLockId(){return lockId;}
	public short getLockDist(){return lockDist;}
	public byte getLockDir(){return lockDir;}
	public byte getNextCheckPoint(){return nextCheckPoint;}
	public short getExtViewWidth(){return ext_view_width;}
	public short getExtViewHeight(){return ext_view_height;}
	public byte getDebrisColors(){return debris_colors;}
	public byte getStat(){return stat;}
	public byte getAutopilotLight(){return autopilot_light;}
	
	@Override
	public void set(SelfHolder other)
	{
		other.setSelf(x, y, vx, vy, 
				heading, power, turnspeed, turnresistance, 
				lockId, lockDist, lockDir, 
				nextCheckPoint, 
				currentTank, fuelSum, fuelMax, 
				ext_view_width, ext_view_height, 
				debris_colors, stat, autopilot_light);
	}
	
	@Override
	public void setFrom(Holder<SelfHolder> other)
	{
		other.set(this);
	}
}