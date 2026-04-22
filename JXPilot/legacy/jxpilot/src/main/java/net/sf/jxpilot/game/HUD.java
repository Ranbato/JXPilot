package net.sf.jxpilot.game;

import net.sf.jgamelibrary.graphics.GfxUtil;
import net.sf.jgamelibrary.graphics.Renderable;
import net.sf.jxpilot.map.*;
import net.sf.jxpilot.graphics.Drawable;
import net.sf.jxpilot.util.*;

import java.awt.*;
import java.awt.geom.*;


/**
 * Class that holds/draws the Heads Up Display.
 * @author Vlad Firoiu
 *
 */
public class HUD implements Drawable, Renderable {
	
	private GameWorld world;
	private BlockMap map;
	private BlockMapSetup setup;
	
	private SelfHolder self=new SelfHolder();
	
	private short worldX, worldY;
	
	private Player lockPlayer = null;
	
	private final int RADARS_SIZE = 10;
	private HolderList<Radar> radarHandler;
	
	private TimedQueue<ScoreObject> scoreObjectHandler;
	/**
	 * Score objects should be displayed for 3 seconds.
	 */
	private final long SCORE_OBJECT_DURATION = 3*1000;
	
	public HUD(GameWorld world)
	{
		this.world = world;
		map = world.getMap();
		setup = map.getSetup();
		
		RADAR_WIDTH = 256;
		RADAR_HEIGHT = (RADAR_WIDTH * setup.getY()) / setup.getX();
		
		HUD_RADAR_WIDTH  = HUD_RADAR_SCALE * RADAR_WIDTH;
		HUD_RADAR_HEIGHT = HUD_RADAR_SCALE * RADAR_HEIGHT;
		
		X_FACTOR = (float) HUD_RADAR_WIDTH / map.getWidth();
		Y_FACTOR = (float) HUD_RADAR_HEIGHT / map.getHeight();
		
		//System.out.println("X Factor = " + X_FACTOR + "\nY Factor = " + Y_FACTOR);
		
		initRadars();
		initScoreObjects();
	}
	
	private void initRadars()
	{
		radarHandler = new HolderList<Radar>(radarFactory, RADARS_SIZE);
	}
	
	public Iterable<Radar> getRadarHandler(){return radarHandler;}
	
	private void initScoreObjects()
	{
		scoreObjectHandler = new TimedQueue<ScoreObject>(SCORE_OBJECT_DURATION);
	}
	
	public Iterable<ScoreObject> getScoreObjectHandler(){return scoreObjectHandler;}
	
	public void addRadar(RadarHolder r)
	{
		radarHandler.add(r);
	}
	
	public void addScoreObject(ScoreObjectHolder o)
	{
		scoreObjectHandler.add(new ScoreObject(o));
	}
	
	public void setSelf(SelfHolder s)
	{
		self.setFrom(s);
		this.setLockId(self.getLockId());
	}
	
	public void setLockId(short lockId)
	{
		if(lockId != Player.NO_ID)
		{
			lockPlayer = world.getPlayer(lockId);
		}
		else
		{
			lockPlayer = null;
		}
	}
	
	public Player getLockedPlayer(){return lockPlayer;}
	
	public SelfHolder getSelf()
	{
		return self;
	}
	
	public void update()
	{
		radarHandler.clear();
		scoreObjectHandler.update();
		
		worldX = world.getSelfX();
		worldY = world.getSelfY();
	}
	
	@Override
	public void paintDrawable(Graphics2D g2d)
	{
		paintScoreObjects(g2d);
		
		for(int i = 0;i<radarHandler.size();i++) {
			radarHandler.get(i).paintDrawable(g2d);
		}
		
		paintLockedPlayer(g2d);
	}

	@Override
	public void render(Graphics2D g2d) {
		renderScoreObjects(g2d);
		
		for(int i = 0;i<radarHandler.size();i++) {
			radarHandler.get(i).render(g2d);
		}
		
		renderLockedPlayer(g2d);
	}
	
	private void paintLockedPlayer(Graphics2D g2d)
	{
		if(lockPlayer!=null)
		{
			g2d.setColor(Color.WHITE);
			Utilities.drawAdjustedStringUp(g2d,lockPlayer.getName(),world.getSelfX(), world.getSelfY()+HUD_RADAR_HEIGHT/2);
		}
	}
	
	private void renderLockedPlayer(Graphics2D g2d) {
		if(lockPlayer!=null) {
			g2d.setColor(Color.WHITE);
			GfxUtil.drawCenteredStringUp(lockPlayer.getName(), world.getSelfX(), world.getSelfY()+HUD_RADAR_HEIGHT/2, g2d);
		}
	}
	
	private void paintScoreObjects(Graphics2D g2d)
	{
		final int height = g2d.getFontMetrics().getHeight();
		final int baseY = world.getSelfY() - 2*height;
		
		int i =0;
		
		for(ScoreObject o : scoreObjectHandler)
		{
			o.paintDrawable(g2d);
			
			Utilities.drawAdjustedStringDown(g2d, o.getMessage() + " " + o.getScore(), world.getSelfX(), baseY-i*height);
			i++;
		}
		
	}
	
	private void renderScoreObjects(Graphics2D g2d) {
		final int height = g2d.getFontMetrics().getHeight();
		final int baseY = world.getSelfY() - 2*height;
		
		int i =0;
		
		for(ScoreObject o : scoreObjectHandler) {
			o.render(g2d);
			GfxUtil.drawCenteredStringDown(o.getMessage() + " " + o.getScore(), world.getSelfX(), baseY-i*height, g2d);
			i++;
		}
		
	}
	
	//"static" radar data
	private final int RADAR_WIDTH, RADAR_HEIGHT;
    
	private final int HUD_RADAR_SCALE = 1;
	private final int HUD_RADAR_WIDTH, HUD_RADAR_HEIGHT;
	
	private final float X_FACTOR, Y_FACTOR;
	
	public class Radar extends RadarHolder implements Drawable, Renderable {
		
		/*
	    int		i;
	    int		hrscale = 3;
	    int		hrw = hrscale * 256;
	    int		hrh = hrscale * RadarHeight;
	    float	xf = (float)hrw / (float)Setup->width, 
			yf = (float)hrh / (float)Setup->height;

	    for (i = 0; i < num_radar; i++) {

	        int sz = radar_ptr[i].size;

		// skip non-enemy objects 
	        if ((sz & 0x80) == 0) {

	            int x = radar_ptr[i].x * hrscale - 
	                (world.x + ext_view_width / 2) * xf;

	            int y = radar_ptr[i].y * hrscale - 
	                (world.y + ext_view_height / 2) * yf;
            if (BIT(Setup->mode, WRAP_PLAY)) {
                if (x < 0) {
                    if (-x > hrw/2) x += hrw;
                } else {
                    if (x > hrw/2) x -= hrw;
                }

                if (y < 0) {
                    if (-y > hrh/2) y += hrh;
                } else {
                    if (y > hrh/2) y -= hrh;
                }
            }

            sz = (sz > 0) ? sz * hrscale : hrscale;

            Arc_add(hudColor,
                    x + ext_view_width / 2 - sz / 2, 
                    -y + ext_view_height / 2 - sz / 2, 
                    sz, sz, 0, 64*360);
 
      
		 */
		
		public final Color RADAR_COLOR = Color.RED;
		private final Ellipse2D radarShape = new Ellipse2D.Float();
		
		public void paintDrawable(Graphics2D g2d) {
			//skips allies
			if((super.size & 0x80) != 0) return;
			
			int radarX = super.x * HUD_RADAR_SCALE - (int)((world.getSelfX()) * X_FACTOR);
			int radarY = super.y * HUD_RADAR_SCALE - (int)((world.getSelfY()) * Y_FACTOR);
			
			if(setup.wrapPlay())
			{
				if (radarX < 0) {
                    if (-radarX > HUD_RADAR_WIDTH/2) radarX += HUD_RADAR_WIDTH;
                } else {
                    if (radarX > HUD_RADAR_WIDTH/2) radarX -= HUD_RADAR_WIDTH;
                }

                if (radarY < 0) {
                    if (-radarY > HUD_RADAR_HEIGHT/2) radarY += HUD_RADAR_HEIGHT;
                } else {
                    if (radarY > HUD_RADAR_HEIGHT/2) radarY -= HUD_RADAR_HEIGHT;
                }
			}
			
            int size = (super.size > 0) ? super.size * HUD_RADAR_SCALE : HUD_RADAR_SCALE;
			
			//skips if radar would be painted on ship
			if(radarX*radarX + radarY*radarY < Utilities.square(GameWorld.Ship.SHIP_RADIUS+size)){return;}
			
			g2d.setColor(RADAR_COLOR);
			
			radarShape.setFrame((radarX+world.getSelfX()-size),
								(radarY+world.getSelfY()-size),
					2*size, 2*size);
			
			g2d.fill(radarShape);
			
			/*
			System.out.println("Radar drawn:" +
								"\nx = " + super.x + 
								"\ny = " + super.y +
								"\nRadar X = " + radarX +
								"\nRadar Y = " + radarY +
								"\nsize = " + size);
			*/
		}

		@Override
		public void render(Graphics2D g2d) {
			if((super.size & 0x80) != 0) return;
			
			int radarX = super.x * HUD_RADAR_SCALE - (int)((world.getSelfX()) * X_FACTOR);
			int radarY = super.y * HUD_RADAR_SCALE - (int)((world.getSelfY()) * Y_FACTOR);
			
			if(setup.wrapPlay()) {
				if (radarX < 0) {
                    if (-radarX > HUD_RADAR_WIDTH/2) radarX += HUD_RADAR_WIDTH;
                } else {
                    if (radarX > HUD_RADAR_WIDTH/2) radarX -= HUD_RADAR_WIDTH;
                }

                if (radarY < 0) {
                    if (-radarY > HUD_RADAR_HEIGHT/2) radarY += HUD_RADAR_HEIGHT;
                } else {
                    if (radarY > HUD_RADAR_HEIGHT/2) radarY -= HUD_RADAR_HEIGHT;
                }
			}
			
            int size = (super.size > 0) ? super.size * HUD_RADAR_SCALE : HUD_RADAR_SCALE;
			
			//skips if radar would be painted on ship
			if(Math.abs(radarX) < GameWorld.Ship.SHIP_RADIUS && Math.abs(radarY) < GameWorld.Ship.SHIP_RADIUS) return;
			
			g2d.setColor(RADAR_COLOR);
			
			/*
			radarShape.setFrame((radarX+world.getSelfX()-size),
								(radarY+world.getSelfY()-size),
					2*size, 2*size);
			
			g2d.fill(radarShape);
			*/
			
			GfxUtil.fillCenteredOval(radarX+world.getSelfX(), radarY+world.getSelfY(), size<<1, size<<1, g2d);
		}
	}
	public final Factory<Radar> radarFactory = new Factory<Radar>() {
		public Radar newInstance(){return new Radar();}
	};
	
	public class ScoreObject extends ScoreObjectHolder implements Drawable, Renderable {
		private final Color SCORE_OBJECT_COLOR = Color.WHITE;
		
		public ScoreObject(){}
		
		public ScoreObject(ScoreObjectHolder holder) {
			holder.set(this);
		}
		
		@Override
		public void paintDrawable(Graphics2D g2d) {
			//AffineTransform saved = g2d.getTransform();
			
			g2d.setColor(SCORE_OBJECT_COLOR);
			//g2d.translate(x, y);
			
			int x = super.x * MapBlock.BLOCK_SIZE;
			int y = super.y * MapBlock.BLOCK_SIZE;
			
			Utilities.drawFlippedString(g2d, String.valueOf(score),
					Utilities.wrap(map.getWidth(), worldX, x),
					Utilities.wrap(map.getHeight(), worldY, y));
			Rectangle2D bounds = g2d.getFontMetrics().getStringBounds(message, g2d);
			
			Utilities.drawFlippedString(g2d, message, (float)(x-bounds.getWidth()/2.0), (float)(y-bounds.getHeight()));
			
			//g2d.setTransform(saved);
		}

		@Override
		public void render(Graphics2D g2d) {
			g2d.setColor(SCORE_OBJECT_COLOR);
			//g2d.translate(x, y);
			
			
			int x = super.x * MapBlock.BLOCK_SIZE;
			int y = super.y * MapBlock.BLOCK_SIZE;
			if(setup.wrapPlay()) {
				x = Utilities.wrap(map.getWidth(), worldX, x);
				y = Utilities.wrap(map.getHeight(), worldY, y);
			}
			
			GfxUtil.drawString(String.valueOf(score), x, y, g2d);
			Rectangle2D bounds = g2d.getFontMetrics().getStringBounds(message, g2d);
			GfxUtil.drawString(message, (int)(x-bounds.getWidth()/2), (int)(y-bounds.getHeight()), g2d);
		}
	}
}