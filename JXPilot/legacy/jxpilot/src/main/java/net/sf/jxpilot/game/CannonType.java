package net.sf.jxpilot.game;

import net.sf.jgamelibrary.geom.Polygon2D;
import net.sf.jgamelibrary.geom.Polygon2DAdaptor;
import net.sf.jgamelibrary.geom.Vector2D;

import static net.sf.jxpilot.map.AbstractBlock.BLOCK_SIZE;

public enum CannonType {
	UP {
		Polygon2D CANNON_UP_POLYGON2D = new Polygon2D(new Vector2D[]{
				new Vector2D(0,0),
				new Vector2D(BLOCK_SIZE, 0),
				new Vector2D(BLOCK_SIZE/2.0, BLOCK_SIZE/4.0)
			});
		@Override
		public Polygon2DAdaptor getPolygon2D() {return CANNON_UP_POLYGON2D;}
	}, RIGHT {
		Polygon2D CANNON_RIGHT_POLYGON2D = new Polygon2D(new Vector2D[]{
				new Vector2D(0,0),
				new Vector2D(BLOCK_SIZE/4.0, BLOCK_SIZE/2.0),
				new Vector2D(0, BLOCK_SIZE)
			});
		@Override
		public Polygon2DAdaptor getPolygon2D() {return CANNON_RIGHT_POLYGON2D;}
	}, DOWN {
		Polygon2D CANNON_DOWN_POLYGON2D = new Polygon2D(new Vector2D[]{
				new Vector2D(0,BLOCK_SIZE),
				new Vector2D(BLOCK_SIZE/2.0, 3.0*BLOCK_SIZE/4.0),
				new Vector2D(BLOCK_SIZE, BLOCK_SIZE)
			});
		@Override
		public Polygon2DAdaptor getPolygon2D() {return CANNON_DOWN_POLYGON2D;}
	}, LEFT {
		Polygon2D CANNON_LEFT_POLYGON2D = new Polygon2D(new Vector2D[]{
				new Vector2D(BLOCK_SIZE, 0),
				new Vector2D(BLOCK_SIZE, BLOCK_SIZE),
				new Vector2D(3.0*BLOCK_SIZE/4.0, BLOCK_SIZE/2.0)
			});
		@Override
		public Polygon2DAdaptor getPolygon2D() {return CANNON_LEFT_POLYGON2D;}
	};
	
	/**
	 * @return The shape of this cannon type.
	 */
	public abstract Polygon2DAdaptor getPolygon2D();
}
