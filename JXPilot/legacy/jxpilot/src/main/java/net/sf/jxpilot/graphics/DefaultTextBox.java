package net.sf.jxpilot.graphics;

import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;

/**
 * Text box implementation with default key presses.
 * @author Vlad Firoiu
 */
public abstract class DefaultTextBox extends TextBox implements KeyListener {
	public static final int DEFAULT_LENGTH = 80;
	
	private static final byte BACK_SPACE = 8, DELETE = 127;
	
	public DefaultTextBox(int max_length) throws IllegalArgumentException {super(max_length);}
	public DefaultTextBox() {this(DEFAULT_LENGTH);}

	@Override
	public void keyPressed(KeyEvent e) {
		switch(e.getKeyCode()) {
		case KeyEvent.VK_DELETE:
		case KeyEvent.VK_BACK_SPACE:
			super.delete();
			break;
		case KeyEvent.VK_ENTER:
			done();
			break;
		case KeyEvent.VK_ESCAPE:
			cancel();
			break;
		}
	}

	@Override
	public void keyReleased(KeyEvent e) {}

	@Override
	public void keyTyped(KeyEvent e) {
		char c = e.getKeyChar();
		if(c != BACK_SPACE && c != DELETE) super.add(e.getKeyChar());
	}

	/**
	 * Called when entering of text is done.
	 */
	protected abstract void done();

	/**
	 * Called if user wants to cancel.
	 */
	protected abstract void cancel();
}
