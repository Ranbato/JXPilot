package net.sf.jxpilot.game;

public class NewBase extends BaseHolder {
	public final BaseType base_type;
	public final int team, x, y;
	
	protected Player player;
	
	public NewBase(BaseType base_type, int team, int num, int x, int y) {
		this.base_type = base_type;
		this.team = team;
		this.num = num;
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Resets this base so that it has no player.
	 */
	public void leave() {
		super.id = Player.NO_ID;
		player = null;
	}
	
	public BaseType getBaseType() {return base_type;}
	public int getTeam() {return team;}
	public int getX() {return x;}
	public int getY() {return y;}
	
	public Player getPlayer() {return player;}
	public void setPlayer(Player p) {
		this.player = p;
		this.id = player.id;
	}
}
