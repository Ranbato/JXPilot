package net.sf.jxpilot.game;

import java.awt.*;
import java.util.*;
import net.sf.jxpilot.graphics.*;

import static net.sf.jxpilot.data.Teams.*;

/**
 * Displays a collection of players by team.
 * @author Vlad Firoiu
 */
public class PlayerTable implements Drawable
{	
	private final Color TABLE_COLOR = Color.WHITE;
	
	private final int NAME_OFFSET = 0;
	private final int LIFE_OFFSET = NAME_OFFSET + 100;
	private final int SCORE_OFFSET = LIFE_OFFSET + 40;
	
	/**
	 * Location of display.
	 */
	private int x,y;
	/**
	 * Actual players.
	 */
	private Collection<? extends Player> players;
	
	public PlayerTable(int x, int y,Collection<? extends Player> players) {
		this.players = players;
		this.x=x;
		this.y=y;
	}
	
	public int getX(){return x;}
	public int getY(){return y;}
	
	public void setX(int x){this.x=x;}
	public void setY(int y){this.y=y;}
	
	public Collection<? extends Player> getPlayers(){return players;}
	public void setPlayers(Collection<? extends Player> players) {this.players = players;}
	
	/**
	 * Displays all players by team.
	 * @param g2d The graphics context.
	 */
	@Override
	public void paintDrawable(Graphics2D g2d) {
		int height = g2d.getFontMetrics().getHeight();
		int i =0;
		
		g2d.setColor(TABLE_COLOR);
		
		g2d.drawString("Name", x+NAME_OFFSET, y);
		g2d.drawString("Lives", x+LIFE_OFFSET, y);
		g2d.drawString("Score", x+SCORE_OFFSET, y);
		i++;
		
		//loops through each team
		for(int team = MIN_TEAM;team<MIN_TEAM+NUM_TEAMS;team++) {
			//determines if team is empty or not
			boolean empty = true;
			for(Player p : players) {
				if(p.getTeam()==team) {
					empty = false;
					break;
				}
			}
			
			//draws each player on the team
			if(!empty) {
				g2d.drawString("Team " + team, x, y+i*height);
				i++;
				
				for(Player p : players) {
					if(p.getTeam()==team) {
						renderPlayer(g2d, p, x, y+i*height);
						i++;
					}
				}
			}
		}
		
		//draws players with no teams
		for(Player p : players) {
			int team = p.getTeam();
			if(team<MIN_TEAM || team>=MIN_TEAM+NUM_TEAMS) {
				renderPlayer(g2d, p, x, y+i*height);
				i++;
			}
		}
	}
	
	/**
	 * Draws the specified player's name, life, and score.
	 * @param g2d The graphics context.
	 * @param p The Player.
	 * @param x The x location.
	 * @param y The y location.
	 */
	private void renderPlayer(Graphics2D g2d, Player p, int x, int y) {
		g2d.drawString(p.getName(), x+NAME_OFFSET, y);
		g2d.drawString(String.valueOf(p.getLife()), x+LIFE_OFFSET, y);
		g2d.drawString(String.valueOf(p.getScore()), x+SCORE_OFFSET, y);
	}
	
}
