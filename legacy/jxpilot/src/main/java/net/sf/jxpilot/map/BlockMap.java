package net.sf.jxpilot.map;

import static net.sf.jxpilot.map.MapBlock.*;

import java.awt.Graphics2D;
import java.util.*;

import net.sf.jxpilot.game.Base;
import net.sf.jxpilot.game.Cannon;
import net.sf.jxpilot.game.FuelStation;
import net.sf.jxpilot.game.NewBase;
import net.sf.jxpilot.game.NewCannon;
import net.sf.jxpilot.util.Utilities;

/**
 * Represents a block-based XPilot map.
 * @author Vlad Firoiu
 */
public class BlockMap implements java.io.Serializable {
	/**
	 * Default UID.
	 */
	private static final long serialVersionUID = 1L;
	
	private final BlockMapSetup setup;
	private final MapBlock[][] mapBlocks;
	private final AbstractBlock[][] blocks;
	
	/**
	 * Map width and height, measured in blocks*BLOCK_SIZE.
	 */
	private final int width, height;
	
	private ArrayList<Cannon> cannons;
	private ArrayList<Base> bases;
	private ArrayList<FuelStation> fuelStations;
	
	private NewCannon[] cannonBlocks;
	private NewBase[] baseBlocks;
	private FuelBlock[] fuelBlocks;
	
	public BlockMap(BlockMapSetup setup) {
		this.setup = setup;
		
		mapBlocks = new MapBlock[setup.getX()][setup.getY()];
		blocks = new AbstractBlock[setup.getX()][setup.getY()];
		for (int x = 0;x<setup.getX();x++) {
			for (int y = 0 ; y<setup.getY();y++) {
				mapBlocks[x][y] = MapBlock.getMapBlock(setup, setup.getNum(x, y));
				blocks[x][y] = BlockType.createBlock(setup, x, y);
			}
		}
		
		width = setup.getX() * BLOCK_SIZE;
		height = setup.getY() * BLOCK_SIZE;
		
		MapInit();
	}
	
	
	/**
	 * Initializes non-block map objects, such as cannons.
	 */
	private void MapInit()
	{
		byte[] map_data = setup.getMapData();
		
		//counts game objects
		int num_cannons = 0, num_bases = 0, num_fuels=0;
		/*
		for(int i =0;i<map_data.length;i++)
		{
			byte block_type = map_data[i];
			if(CannonType.getCannonType(block_type)!=null) num_cannons++;
			if(BaseType.getBaseType(block_type)!=null) num_bases++;
			if(block_type==BlockMapSetup.SETUP_FUEL) num_fuels++;
		}
		 */
		for(short y = 0;y<setup.getY();y++) {
			for(short x = 0;x<setup.getX(); x++) {
				if(blocks[x][y] != null) {
					if(blocks[x][y] instanceof NewCannon) num_cannons++;
					else if(blocks[x][y] instanceof NewBase) num_bases++;
					else if(blocks[x][y] instanceof FuelBlock) num_fuels++;
				}
			}
		}
		
		//creates game objects
		cannons = new ArrayList<Cannon>(num_cannons);
		cannonBlocks = new NewCannon[num_cannons];
		num_cannons=0;
		
		bases = new ArrayList<Base>(num_bases);
		baseBlocks = new NewBase[num_bases];
		num_bases = 0;
		
		fuelStations = new ArrayList<FuelStation>(num_fuels);
		fuelBlocks = new FuelBlock[num_fuels];
		num_fuels = 0;
		
		
		for(int i =0;i<map_data.length;i++)
		{
			byte block_type = map_data[i];
			int x = setup.getX(i);
			int y = setup.getY(i);
			
			Cannon c = Cannon.createCannon(block_type, num_cannons, x, y);
			if(c!=null) {
				cannons.add(num_cannons++, c);
			}
			
			Base b = Base.createBase(block_type, num_bases, x, y);
			if(b!=null) {
				bases.add(num_bases++, b);
			}
			
			if(block_type==BlockMapSetup.SETUP_FUEL) {
				fuelStations.add(num_fuels++, new FuelStation(num_fuels, x, y));
			}
		}
		
		num_cannons = num_bases = num_fuels = 0;
		for(short x = 0;x<setup.getY();x++) {
			for(short y = 0;y<setup.getY(); y++) {
				if(blocks[x][y] != null) {
					if(blocks[x][y] instanceof NewCannon) cannonBlocks[num_cannons++] = (NewCannon) blocks[x][y];
					else if(blocks[x][y] instanceof NewBase) baseBlocks[num_bases++] = (NewBase) blocks[x][y];
					else if(blocks[x][y] instanceof FuelBlock) fuelBlocks[num_fuels++] = (FuelBlock) blocks[x][y];
				}
			}
		}
	}
	
	public MapBlock[][] getBlocks(){return mapBlocks;}
	public BlockMapSetup getSetup(){return setup;}
	public MapBlock getBlock(short x, short y){return mapBlocks[x][y];}
	public MapBlock getBlock(int num) {
		return mapBlocks[setup.getX(num)][setup.getY(num)];
	}
	
	public int getWidth(){return width;}
	public int getHeight(){return height;}
	
	public ArrayList<Cannon> getCannons(){return cannons;}
	public ArrayList<Base> getBases(){return bases;}
	public ArrayList<FuelStation> getFuelStations(){return fuelStations;}
	
	public NewCannon getCannon(int num) {return cannonBlocks[num];}
	public int numCannons(){return cannonBlocks.length;}
	public NewBase getBase(int num) {return baseBlocks[num];}
	public int numBases(){return baseBlocks.length;}
	public FuelBlock getFuel(int num) {return fuelBlocks[num];}
	public int numFuels(){return fuelBlocks.length;}
	
	@Override
	public String toString() {
		StringBuilder temp = new StringBuilder();
		for (int y=setup.getY()-1;y>=0;y--) {
			for (int x = 0;x<setup.getY();x++) {
				temp.append(mapBlocks[x][y]);
			}
			temp.append('\n');
		}
		return temp.toString();
	}
	
	/**
	 * Paints the map.
	 * @param centerX center of view area, in blocks.
	 * @param centerY center of view area, in blocks.
	 * @param xRadius half of view width, in blocks.
	 * @param yRadius half of view height, in blocks.
	 * @param g2 Graphics on which to paint the blocks
	 */
	public void render(int centerX, int centerY, int xRadius, int yRadius, Graphics2D g2d) {
		for(int x=centerX-xRadius;x<=centerX+xRadius;x++) {
			for(int y=centerY-yRadius;y<=centerY+yRadius;y++) {
				blocks[Utilities.trueMod(x, setup.getX())][Utilities.trueMod(y, setup.getY())].render(x*BLOCK_SIZE, y*BLOCK_SIZE, g2d);
			}
		}
	}
}