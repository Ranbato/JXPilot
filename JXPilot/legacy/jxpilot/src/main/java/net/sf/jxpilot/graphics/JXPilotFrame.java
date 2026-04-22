package net.sf.jxpilot.graphics;

import static net.sf.jxpilot.map.MapBlock.BLOCK_SIZE;

import java.util.*;
import java.awt.event.*;

import net.sf.jgamelibrary.geom.Vector2D;
import net.sf.jgamelibrary.graphics.AbstractRenderer;
import net.sf.jgamelibrary.graphics.BufferedFrame;
import net.sf.jgamelibrary.graphics.FrameMode;
import net.sf.jgamelibrary.graphics.GfxUtil;
import net.sf.jxpilot.data.Keys;
import java.awt.*;
import java.awt.geom.*;
import java.awt.image.*;
import javax.swing.*;
import net.sf.jxpilot.user.*;
import net.sf.jxpilot.*;
import net.sf.jxpilot.game.*;
import net.sf.jxpilot.game.GameWorld.Ball;
import net.sf.jxpilot.game.GameWorld.Connector;
import net.sf.jxpilot.game.GameWorld.FastShot;
import net.sf.jxpilot.game.GameWorld.Ship;
import net.sf.jxpilot.game.GameWorld.Spark;
import net.sf.jxpilot.map.AbstractBlock;

public class JXPilotFrame extends BufferedFrame {
	/**
	 * Default UID.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Whether or not to try to recenter the mouse after every movement.
	 */
	public final boolean MOUSE_RECENTERING = true;
	
	/**
	 * The display mode to be used if no display mode is given.
	 */
	public static final DisplayMode defaultDisplayMode = DisplayMode.UFS;
	
	/**
	 * DisplayMode used by this JXPilotFrame.
	 */
	private DisplayMode displayMode;
	
	/**
	 * Whether or not the mouse should be used to control user input.
	 */
	private boolean mouseControl = false;
	
	private AffineTransform identity = new AffineTransform();

	private Toolkit toolkit = Toolkit.getDefaultToolkit();
	private Dimension screenSize = toolkit.getScreenSize();
	
	private ClientInputListener clientInputListener;
	
	private GameWorld world;
	
	public static final int defaultViewSize = 27;
	
	/**
	 * number of blocks in the screen
	 */
	private int viewSize;

	/**
	 * Whether or not the player is typing a message.
	 */
	private boolean typing = false;
	
	/**
	 * TextBox used to input messages.
	 */
	private DefaultTextBox textBox = new DefaultTextBox() {
		@Override
		protected void done() {
			String message = super.getText();
			if(!message.isEmpty()) clientInputListener.talk(message);
			typing = false;
		}
		@Override
		protected void cancel() {
			typing = false;
		}
	};
	
	/**
	 * Messages to draw.
	 */
	private MessagePool messagePool = null;
	
	/**
	 * Table of players and scores.
	 */
	private PlayerTable playerTable=null;
	
	/**
	 * Maps local keyboard to abstract XPilot keyboard which is sent to server.
	 * Note that each actual key may represent multiple abstract XPilot keys.
	 */
	private HashMap<Integer, Byte[]> keyPreferences;
	
	/**
	 * Maps mouse buttons to abstract XPilot keyboard which is sent to server.
	 */
	private HashMap<Integer, Byte[]> mousePreferences;
	
	/**
	 * Maps keyboard to various user options.
	 */
	private HashMap<Integer, UserOption> userPreferences;
	
	/**
	 * Maps user options to the actual OptionHandlers.
	 */
	private EnumMap<UserOption, OptionHandler> optionHandlers;
	
	private WorldRenderer renderer;
	public JXPilotFrame(DisplayMode mode, GameWorld world, ClientInputListener l) {
		super(mode.FRAME_MODE);
		
		this.world = world;
		clientInputListener = l;
		
		defaultKeyInit();
		defaultMouseInit();
		defaultUserInit();
		optionsInit();
		
		viewSize = defaultViewSize;
		
		this.world = world;
		renderer = new WorldRenderer(world);		

		System.out.println("Using " + super.getFrameMode());
		
		messagePool = new MessagePool();
		playerTable = new PlayerTable(20, super.getHeight()/2, world.getPlayers());
		
		this.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent e) {
				if(typing) {
					textBox.keyPressed(e);
				} else {
					int key = e.getKeyCode();

					if(userPreferences.containsKey(key)) {
						UserOption option = userPreferences.get(key);
						if(option != UserOption.TALK) optionHandlers.get(option).fireOption();
					}
					
					if(keyPreferences.containsKey(key)) {
						for (byte b : keyPreferences.get(key))
							clientInputListener.setKey(b, true);
					}
				}
			}

			@Override
			public void keyReleased(KeyEvent e) {
				if(typing) {
					textBox.keyReleased(e);
				} else {
					int key = e.getKeyCode();

					if (userPreferences.containsKey(key)) {
						UserOption option = userPreferences.get(key);
						if(option == UserOption.TALK) optionHandlers.get(option).fireOption();
					}
					
					if(keyPreferences.containsKey(key)) {
						for (byte b : keyPreferences.get(key))
							clientInputListener.setKey(b, false);
					}
				}
			}
			
			@Override
			public void keyTyped(KeyEvent e) {
				if(typing) {
					textBox.keyTyped(e);
				}
			}
		});

		this.addMouseListener(new MouseAdapter()
		{
			
			public void mousePressed(MouseEvent e)
			{
				if (!mouseControl || typing) return;
				
				int button = e.getButton();
				if (mousePreferences.containsKey(button))
				{
					for(byte b : mousePreferences.get(button))
					{
						clientInputListener.setKey(b, true);
					}
				}
			}
			
			public void mouseReleased(MouseEvent e)
			{
				if (!mouseControl || typing) return;
				
				int button = e.getButton();
				if (mousePreferences.containsKey(button))
				{
					for(byte b : mousePreferences.get(button))
					{
						clientInputListener.setKey(b, false);
					}
				}			
			}
		});
		
		this.addMouseMotionListener(new MouseMotionHandler());
	}
	
	/**
	 * Class that handles MouseMovement. It sends movement to the server so the ship can turn.
	 * It also attempts to move the mouse pointer back to the center of the screen, if allowed by the system.
	 * @author Vlad Firiou
	 */
	private class MouseMotionHandler extends MouseMotionAdapter
	{

		private Robot robot;
		private int mouseX, mouseY;
		private boolean robotMovement;
		private int centerX = screenSize.width/2,
					centerY = screenSize.height/2;
		
		private int margin = 100;
		
		public MouseMotionHandler()
		{
			if (MOUSE_RECENTERING)
			try
			{
				robot = new Robot();
				//this.movePointerBack();
				//mouseX = screenSize.width/2;
			}
			catch(AWTException e)
			{
				System.out.println("Can't control mouse movements :(");
				robot = null;
			}
		}
		
		/**
		 * @param e The mouse location.
		 * @return
		 */
		private boolean inBounds(MouseEvent e)
		{
			return (e.getX()>margin &&e.getX()< JXPilotFrame.this.getWidth()-margin) &&
					(e.getY()>margin &&e.getY()< JXPilotFrame.this.getHeight()-margin);
		}
		
		private Point lastMousePosition = new Point();
		
		@Override
		public void mouseMoved(MouseEvent e) {
			if (!mouseControl || typing) return;
			
			lastMousePosition.setLocation(e.getLocationOnScreen());
			//if (!robotMovement) 
				handleMove(e);
		}
		
		@Override
		public void mouseDragged(MouseEvent e) {
			if (!mouseControl || typing) return;
			
			lastMousePosition.setLocation(e.getLocationOnScreen());
			//if (!robotMovement) 
				handleMove(e);
		}
		
		/**
		 * Amount of time to let pointer move back, in nanoseconds.
		 */
		private final long pointer_delay = 1000*10;//10ms
		
		/**
		 * Time of last move pointer back, in nanoseconds.
		 */
		private long last_move;
		
		private void handleMove(MouseEvent e)
		{
			if (MOUSE_RECENTERING)
			{
			// this event is from re-centering the mouse - ignore it
			    if (robotMovement) {
			    	if(this.inBounds(e))
			    		robotMovement = false;
			    	else if(!inBounds(e)) movePointerBack(e);
			    }
			    else
			    {
					clientInputListener.movePointer((short)((e.getXOnScreen()-mouseX)));
					//mouseX = e.getX();
					if(robot!=null && !inBounds(e))
						movePointerBack(e);
					else
					{
						mouseX = e.getXOnScreen();
					}
			    }
			}
			else
			{
				clientInputListener.movePointer((short)((e.getX()-mouseX)));
				mouseX = e.getX();
			}
		}
		
		private void movePointerBack(MouseEvent e) {
			movePointerBack();
		}
		
		private void movePointerBack() {
			robotMovement = true;
			mouseX = JXPilotFrame.this.getX()+JXPilotFrame.this.getWidth()/2;
			mouseY = JXPilotFrame.this.getY()+JXPilotFrame.this.getHeight()/2;
			robot.mouseMove(mouseX,mouseY);
			last_move = System.nanoTime();
			//robotMovement = false;
		}
	}
	
	private Image blankImage = toolkit.createImage(new byte[]{0});
	private Cursor noCursor = toolkit.createCustomCursor(blankImage, new Point(1,1), "No Cursor");
	
	private void hideCursor()
	{
		setCursor(noCursor);
	}
	
	private void showCursor()
	{
		this.setCursor(Cursor.getDefaultCursor());
	}
	
	/**
	 * sets default mouse actions
	 */
	
	private void defaultMouseInit()
	{
		mousePreferences = new HashMap<Integer, Byte[]>();
		
		mousePreferences.put(MouseEvent.BUTTON1, new Byte[]{Keys.KEY_FIRE_SHOT});
		mousePreferences.put(MouseEvent.BUTTON3, new Byte[]{Keys.KEY_THRUST});
	}
	
	//keyboard stuff
	/**
	 * sets default keys 
	 */
	private void defaultKeyInit()
	{
		keyPreferences = new HashMap<Integer, Byte[]>();
		
		keyPreferences.put(KeyEvent.VK_ENTER,	new Byte[] {Keys.KEY_FIRE_SHOT});
		keyPreferences.put(KeyEvent.VK_A, 		new Byte[] {Keys.KEY_TURN_LEFT});
		keyPreferences.put(KeyEvent.VK_S, 		new Byte[] {Keys.KEY_TURN_RIGHT});
		keyPreferences.put(KeyEvent.VK_SHIFT, 	new Byte[] {Keys.KEY_THRUST});
		keyPreferences.put(KeyEvent.VK_CONTROL, new Byte[] {Keys.KEY_CONNECTOR, Keys.KEY_REFUEL});
		keyPreferences.put(KeyEvent.VK_H, 		new Byte[] {Keys.KEY_CHANGE_HOME});
		keyPreferences.put(KeyEvent.VK_D, 		new Byte[] {Keys.KEY_DROP_BALL});
		keyPreferences.put(KeyEvent.VK_PAUSE, 	new Byte[] {Keys.KEY_PAUSE});
		keyPreferences.put(KeyEvent.VK_M,		new Byte[] {Keys.KEY_TALK} );
		keyPreferences.put(KeyEvent.VK_T,		new Byte[] {Keys.KEY_TALK} );
		keyPreferences.put(KeyEvent.VK_SPACE,	new Byte[] {Keys.KEY_SHIELD});
		keyPreferences.put(KeyEvent.VK_BACK_SPACE, new Byte[]{Keys.KEY_CLOAK, Keys.KEY_EMERGENCY_SHIELD});
		keyPreferences.put(KeyEvent.VK_P, 		new Byte[] {Keys.KEY_PHASING});
		keyPreferences.put(KeyEvent.VK_Z, 		new Byte[]{Keys.KEY_DROP_BALL});
		keyPreferences.put(KeyEvent.VK_I, 		new Byte[]{Keys.KEY_TOGGLE_IMPLOSION});
		keyPreferences.put(KeyEvent.VK_C, 		new Byte[]{Keys.KEY_TOGGLE_CLUSTER});
		keyPreferences.put(KeyEvent.VK_N, 		new Byte[]{Keys.KEY_TOGGLE_NUCLEAR});
		keyPreferences.put(KeyEvent.VK_SLASH, 	new Byte[]{Keys.KEY_FIRE_LASER});
		keyPreferences.put(KeyEvent.VK_BACK_SLASH, new Byte[]{Keys.KEY_FIRE_MISSILE});
		keyPreferences.put(KeyEvent.VK_LEFT, new Byte[]{Keys.KEY_LOCK_PREV});
		keyPreferences.put(KeyEvent.VK_RIGHT, new Byte[]{Keys.KEY_LOCK_NEXT});
		keyPreferences.put(KeyEvent.VK_UP, new Byte[]{Keys.KEY_LOCK_NEXT_CLOSE});
		keyPreferences.put(KeyEvent.VK_UP, new Byte[]{Keys.KEY_LOCK_CLOSE});
		
		
		//keyPreferences.put(KeyEvent.VK_F5, Keys.)
	}
	
	private void optionsInit()
	{
		optionHandlers = new EnumMap<UserOption, OptionHandler>(UserOption.class);
		
		optionHandlers.put(UserOption.QUIT, new OptionHandler() {
			public void fireOption() {
				clientInputListener.quit();
			}
		});
		
		optionHandlers.put(UserOption.TOGGLE_MOUSE_CONTROL, new OptionHandler() {
			public void fireOption() {
				if (mouseControl) {
					showCursor();
					mouseControl = false;
				} else {
					hideCursor();
					mouseControl = true;
				}
			}
		});
		
		optionHandlers.put(UserOption.TALK, new OptionHandler() {
			public void fireOption() {
				/*
				if(displayMode != DisplayMode.FSEM) {
					typing = true;
					String message = JOptionPane.showInputDialog(JXPilotFrame.this, "Enter a message: ");
					if(message != null && !message.isEmpty())
						clientInputListener.talk(message);
					typing = false;
				}
				 */
				typing = true;
				textBox.clear();
			}
		});
		
		optionHandlers.put(UserOption.BALL_GONE, new OptionHandler() {
			public void fireOption() {
				clientInputListener.talk("***BALL IS GONE SAVE IT NOW***");
			}
		});
		
		optionHandlers.put(UserOption.BALL_SAFE, new OptionHandler() {
			public void fireOption() {
				clientInputListener.talk("***BALL IS SAFE, DON'T SHOOT YOURSELF***");
			}
		});
		optionHandlers.put(UserOption.COVER, new OptionHandler() {
			public void fireOption() {
				clientInputListener.talk("***BALL IS APPROACHING BASE COVER NOW***");
			}
		});
		optionHandlers.put(UserOption.BALL_POP, new OptionHandler() {
			public void fireOption() {
				clientInputListener.talk("***BALL HAS POPPED***");
			}
		});
		optionHandlers.put(UserOption.SWITCH_TEAMS, new OptionHandler() {
			public void fireOption() {
				byte team = world.getSelf().getTeam();
				clientInputListener.talk("/team " + (6-team));
			}
		});
	}
	
	private interface OptionHandler{
		public void fireOption();
	}

	private void defaultUserInit()
	{
		userPreferences = new HashMap<Integer, UserOption>();
		
		userPreferences.put(KeyEvent.VK_ESCAPE, UserOption.QUIT);
		userPreferences.put(KeyEvent.VK_K, UserOption.TOGGLE_MOUSE_CONTROL);
		userPreferences.put(KeyEvent.VK_F1, UserOption.BALL_GONE);
		userPreferences.put(KeyEvent.VK_F2, UserOption.BALL_SAFE);
		userPreferences.put(KeyEvent.VK_F3, UserOption.COVER);
		userPreferences.put(KeyEvent.VK_F4, UserOption.BALL_POP);
		userPreferences.put(KeyEvent.VK_F5, UserOption.SWITCH_TEAMS);
		userPreferences.put(KeyEvent.VK_M, UserOption.TALK);
	}

	//graphics stuff
	public static final Color SPACE_COLOR = Color.BLACK;
	
	private final XPilotRenderer xpilotRenderer = new XPilotRenderer();
	private class XPilotRenderer extends AbstractRenderer {

		private final Vector2D centerScale = new Vector2D(0.5, 0.5);
		@Override
		protected Vector2D getCenterScale() {return centerScale;}

		@Override
		protected int getHeight() {return JXPilotFrame.this.getHeight();}

		private final Vector2D scaleFactor = new Vector2D();
		@Override
		protected Vector2D getScaleFactor() {
			double scale = (double)getHeight()/world.getExtViewHeight();
			return scaleFactor.setCartesian(scale, scale);
		}

		private final Vector2D viewCenter = new Vector2D();
		@Override
		protected Point2D getViewCenter() {return viewCenter.setCartesian(world.getSelfX(), world.getSelfY());}

		@Override
		protected int getWidth() {return JXPilotFrame.this.getWidth();}
	}
	
	private final boolean USE_XPILOT_RENDERER = true;
	
	@Override
	protected void render(Graphics2D screenG2D) {
		if(USE_XPILOT_RENDERER) {			
			xpilotRenderer.setDrawTransform(screenG2D);
			
			int viewHeight = world.getExtViewHeight(),
			viewWidth = super.getWidth() * viewHeight / super.getHeight();
			
			int centerX = world.getSelfX()/BLOCK_SIZE,
			centerY = world.getSelfY()/BLOCK_SIZE,
			xRadius = (1+viewWidth/2)/BLOCK_SIZE,
			yRadius = (1+viewHeight/2)/BLOCK_SIZE;
			
			screenG2D.setColor(SPACE_COLOR);
			GfxUtil.fillRect(world.getSelfX()-viewWidth/2, world.getSelfY()-viewHeight/2,
					viewWidth, viewHeight, screenG2D);
			world.getMap().render(centerX, centerY, xRadius, yRadius, screenG2D);
			for(Ship s : world.getShipHandler()) {
				s.render(screenG2D);
			}
			for(FastShot f : world.getShotHandler()) {
				f.render(screenG2D);
			}
			for(Spark s : world.getSparkHandler()) {
				s.render(screenG2D);
			}
			for(Ball b : world.getBallHandler()) {
				b.render(screenG2D);
			}
			for(Connector c : world.getConnectorHandler()) {
				c.render(screenG2D);
			}
			world.getHud().render(screenG2D);

		} else {
			renderer.renderGame(screenG2D, super.getWidth()/2.0, super.getHeight()/2.0,
					(viewSize*super.getWidth())/super.getHeight(), viewSize, super.getHeight()/viewSize);
			//screenG2D.setTransform(currentTransform);
			//screenG2D.drawImage(mapBuffer, 0, 0, this);
		}
		
		screenG2D.setTransform(identity);
		messagePool.render(screenG2D);

		playerTable.setY(super.getHeight()/2);
		playerTable.paintDrawable(screenG2D);
		
		if(typing)
			textBox.render(screenG2D, super.getWidth()/2 - 40, super.getHeight()/2-20, 80);
	}

	/**
     * Shows if Full Screen Exclusive Mode is used by this
     * <code>JXPilotFrame</code>.
     * 
     * @return <code>True</code>, if supported.
     */
    public boolean isFSEMSupported() {
        return super.getFrameMode() == FrameMode.FSEM;
    }

    /**
     * @see #messagePool
     */
    public MessagePool getMessagePool() {
        return messagePool;
    }
}