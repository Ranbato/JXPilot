package net.sf.jxpilot.graphics;

import java.awt.Graphics;
import java.awt.Color;

/**
 * Displays a text box.
 * @author Vlad Firoiu
 */
public class TextBox {
	
	private static final Color BACKGROUND_COLOR = Color.WHITE;
	
	private static final Color TEXT_COLOR = Color.BLACK;
	
	/**
	 * The text.
	 */
	private final char[] text;
	
	/**
	 * Current length of text.
	 */
	private int length=0;
	
	public TextBox(int max_length) {
		if(max_length < 0) throw new IllegalArgumentException("Invalid max length.");
		text = new char[max_length];
	}
	
	public int getMaxLength() {return text.length;}
	public int getLength() {return length;}
	public boolean isEmpty() {return length == 0;}
	public char[] getChars() {return text;}
	public String getText() {return String.valueOf(text, 0, length);}
	
	public void render(Graphics g, int x, int y, int width) {
		g.setColor(BACKGROUND_COLOR);
		int text_width = g.getFontMetrics().charsWidth(text, 0, length);
		int height = g.getFontMetrics().getHeight();
		g.fillRect(x, y, width < text_width ? (text_width + 5) : width, height+5);
		
		g.setColor(TEXT_COLOR);
		g.drawChars(text, 0, length, x, y+height);
	}
	
	public void render(Graphics g, int x, int y) {
		g.setColor(BACKGROUND_COLOR);
		int height = g.getFontMetrics().getHeight();
		g.drawRect(x, y, g.getFontMetrics().charsWidth(text, 0, length)+5, height+5);
		
		g.setColor(TEXT_COLOR);
		g.drawChars(text, 0, length, x, y+height);
	}
	
	public void clear() {length = 0;}
	
	public boolean add(char c) {
		if(length < text.length) {
			text[length++] = c;
			return true;
		} else return false;
	}
	
	public boolean delete() {
		if(length > 0) {
			length--;
			return true;
		} else return false;
	}
}
