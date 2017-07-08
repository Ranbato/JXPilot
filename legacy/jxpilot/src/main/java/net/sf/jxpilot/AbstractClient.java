package net.sf.jxpilot;

import net.sf.jxpilot.game.*;
import net.sf.jxpilot.map.BlockMap;
import net.sf.jgamelibrary.preferences.Preferences;
import net.sf.jxpilot.net.packet.QuitPacket;
import net.sf.jxpilot.net.packet.ShutdownPacket;

public interface AbstractClient
{
	public void handlePacketEnd();
	
	public void handleTimeout();
	public void handleShutdown(ShutdownPacket s);
	public void mapInit(BlockMap blockMap);
	public void handleSelf(SelfHolder self);
	public void handleQuit(QuitPacket q);
	public void handleShip(ShipHolder s);
	public void handlePlayer(PlayerHolder p);
	public void handleRadar(RadarHolder r);
	public void handleLeave(short id);
	public void handleStart(int loops);
	public void handleEnd(int loops);
	public void handleFastShot(AbstractDebrisHolder shot);
	public void handleBall(BallHolder b);
	public void handleConnector(ConnectorHolder c);
	public void handleMine(MineHolder m);
	public void handleDebris(AbstractDebrisHolder d);
	public void handleMissile(MissileHolder m);
	public void handleScoreObject(ScoreObjectHolder s);
	public void handleCannon(CannonHolder c);
	public void handleBase(BaseHolder b);
	public void handleFuel(FuelHolder f);
	public void handleScore(short id, short score, short life);
	public void handleEyes(short id);
	public void handleSelfItems(byte[] items);
	public void handleModifiers(String modifiers);
	public void handleLaser(LaserHolder l);
	public void handleECM(ECMHolder e);
	public void handleRefuel(RefuelHolder r);
	public void handleSeek(SeekHolder s);
	public void handleAsteroid(AsteroidHolder a);
	
	/**
     * Prints message in client window.
     * 
     * @param message
     *            Message to print.
     */
    public void handleMessage(String message);
    
    /**
     * Returns current view position x value.
     */
    public short getSelfX();
    /**
     * Returns current view position y value.
     */
    public short getSelfY();

    /**
     * Preferences of this client.
     * 
     * @see Client#getPreferences()
     */
    public Preferences getPreferences();
}