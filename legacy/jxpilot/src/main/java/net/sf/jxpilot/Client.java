package net.sf.jxpilot;

import javax.swing.JOptionPane;

import net.sf.jxpilot.game.*;
import net.sf.jxpilot.graphics.*;
import net.sf.jxpilot.map.BlockMap;
import net.sf.jxpilot.net.NetClient;
import net.sf.jxpilot.net.packet.QuitPacket;
import net.sf.jxpilot.net.packet.ShutdownPacket;
import net.sf.jgamelibrary.preferences.Preferences;

/**
 * Class that manages client data. Communication between user and
 * NetClient go through here.
 * @author Vlad Firoiu
 */
public class Client implements AbstractClient, ClientInputListener
{
	private volatile boolean quit=false;
	
	private NetClient netClient;
	private BlockMap blockMap;
	private JXPilotFrame frame;

	/**
     * "Preferences" sent by XPilotPanel.<br>
     * For now its only copy of XPilotPanel's preferences.
     */
    private Preferences prefs = null;

	private GameWorld world;
	private HUD hud;
	
	/**
	 * Our current position. Perhaps would be more efficient to be protected so as to allow direct access.
	 */
	private short selfX, selfY;
	
	public short getSelfX(){return selfX;}
	public short getSelfY(){return selfY;}
	
	
	/**
	 * The Player object representing us.
	 */
	private Player self;
	
	/**
	 * The id of the player we are watching, or -1 if we aren't dead.
	 */
	private short eyesId = -1;
	
	/**
     * Creates new <code>Client</code>.
     * 
     * @param p
     *            Preferences for this client or null for launch w/o
     *            XPilotPanel.
     */
    public Client(Preferences p) {
        prefs = p;
        netClient = new NetClient(this);
    }

	public void runClient(String serverIP, int serverPort) {
		netClient.runClient(serverIP, serverPort);
	}
	
	public BlockMap getMap(){return blockMap;}
	public GameWorld getWorld(){return world;}
	public JXPilotFrame getFrame(){return frame;}
	public HUD getHUD(){return hud;}
	public Player getSelf(){return self;}
	
	//Abstract Client methods
	@Override
	public void handlePacketEnd() {
		//frame.activeRender();
	}
	
	@Override
	public void handleTimeout() {
		JOptionPane.showMessageDialog(frame, "Server timed out!");
		quit();
	}
	
	@Override
	public void handleShutdown(ShutdownPacket s) {
		JOptionPane.showMessageDialog(frame, "Server shut down!");
		quit();
	}
	
	/**
	 * Initializes world which maintains map and various drawables.
	 * Also initializes JXPilotFrame.
	 */
	@Override
	public void mapInit(BlockMap blockMap) {
		world = new GameWorld(blockMap);
		hud = world.getHud();
		this.blockMap = blockMap;
		
		DisplayMode mode = prefs!=null ?
				DisplayMode.getDisplayMode(prefs.get(DisplayMode.displayModeKey))
				: DisplayMode.AFS;
		
		frame = new JXPilotFrame(mode, world, this);
		//frame.setDrawables(drawables);
		
		frame.setVisible(true);
	}
	
	@Override
	public void handleQuit(QuitPacket q) {
		JOptionPane.showMessageDialog(frame, "Server closed connection:\n" + q.getReason());
		this.quit();
	}
	
	@Override
	public void handleSelf(SelfHolder self) {
		eyesId = -1;
		selfX = self.getX();
		selfY = self.getY();
		
		world.setViewPosition(selfX, selfY);
		world.setExtView(self.getExtViewWidth(), self.getExtViewHeight());

		hud.setSelf(self);
	}
	
	@Override
	public void handleShip(ShipHolder s) {
		world.addShip(s);
	}
	
	@Override
	public void handlePlayer(PlayerHolder p) {
		Player player = new Player(p);
		System.out.println(player);
		//System.out.println("Our nick is: " + netClient.getNick());
		
		if(player.getName().equalsIgnoreCase(netClient.getNick())) {
			self = player;
			world.setSelf(player);
			System.out.println("Found self!");
		}
		
		world.addPlayer(player);
	}
	
	@Override
	public void handleRadar(RadarHolder r) {
		world.handleRadar(r);
	}
	
	@Override
	public void handleLeave(short id) {
		world.removePlayer(id);
	}
	
	@Override
	public void handleStart(int loops) {
		world.update();
	}
	
	@Override
	public void handleEyes(short id) {
		eyesId = id;
		world.setViewPosition(eyesId);
	}
	
	@Override
	public void handleScoreObject(ScoreObjectHolder s) {
		world.addScoreObject(s);
	}
	
	@Override
	public void handleEnd(int loops) {
		//setFrameView();
		if(!quit) frame.activeRender();
	}
	
	@Override
	public void handleFastShot(AbstractDebrisHolder shot) {
		//shotHandler.addDrawable(s);
		
		world.addFastShot(shot);
	}
	
	@Override
	public void handleBall(BallHolder ball) {
		//ballHandler.addDrawable(b);
		
		world.addBall(ball);
	}
	
	@Override
	public void handleConnector(ConnectorHolder connector) {
		world.addConnector(connector);
	}
	
	@Override
	public void handleFuel(FuelHolder f) {
		world.handleFuel(f);
	}
	
	@Override
	public void handleCannon(CannonHolder c) {
		world.handleCannon(c);
	}
	
	@Override
	public void handleBase(BaseHolder b) {
		world.handleBase(b);
	}
	
    /**
     * @see net.sf.jxpilot.AbstractClient#handleMessage(java.lang.String)
     */
    @Override
    public void handleMessage(String message) {
        frame.getMessagePool().publishMessage(message);
    }
	
    @Override
	public void handleMine(MineHolder mine) {
		//mineHandler.addDrawable(m);
		
		world.addMine(mine);
	}
    @Override
	public void handleDebris(AbstractDebrisHolder debris) {
		//debrisHandler.addDrawable(d);
		world.addSpark(debris);
		
		//System.out.println("NetClient handling debris!");
	}
	
    @Override
	public void handleMissile(MissileHolder missile) {
		//missileHandler.addDrawable(m);
		world.addMissile(missile);
	}
	
    @Override
	public void handleScore(short id, short score, short life) {
		Player p = world.getPlayer(id);
		if(p!=null) {
			p.setScore(score);
			p.setLife(life);
		}
	}
	
    @Override
    public void handleSelfItems(byte[] items) {
    	//TODO Implement handling of self items. Perhaps display them in HUD.
    }
    
    @Override
	public void handleModifiers(String modifiers) {
		//TODO Implement handling of modifiers. Perhaps display them in HUD.
	}
    
    @Override
    public void handleLaser(LaserHolder l) {
    	//TODO Implement handling of lasers. These should be added to GameWorld and drawn.
    }
    
    @Override
    public void handleECM(ECMHolder e) {
    	//TODO Implement handling of ECM.
    }
    
    @Override
    public void handleRefuel(RefuelHolder r) {
    	//TODO Implement handling of Refuel.
    }
    
    @Override
    public void handleSeek(SeekHolder s) {
    	//TODO Implement handling of Seek.
    }
    
    @Override
    public void handleAsteroid(AsteroidHolder a) {
    	//TODO Implement handling of Asteroid.
    }
    
	//Client Input Listener methods
	@Override
    public void quit() {
		quit=true;
		netClient.quit();
		frame.finish();
		//System.exit(0);
	}
	
	@Override
    public void setKey(int key, boolean value) {
		netClient.setKey(key, value);
	}
	
	/**
	 * The amount of degrees (in 128 degree mode) that the ship moves is amount*turnspeed/64.
	 */
	@Override
    public void movePointer(short amount) {
		netClient.movePointer(amount);
	}

	@Override
    public void talk(String message) {
		netClient.netTalk(message);
	}
	
	/**
	 * Returns preferences of this client or <code>null</code>, if it was
	 * launched w/o XPilotPanel.
	 * 
	 * @return Client's preferences.
	 */
	@Override
    public Preferences getPreferences() {
		return prefs;
	}
}