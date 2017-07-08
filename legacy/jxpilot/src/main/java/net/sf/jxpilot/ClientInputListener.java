package net.sf.jxpilot;

/**
 * Interface for listening to the possible user inputs.
 * @author Vlad Firoiu
 */
public interface ClientInputListener
{
	/**
	 * Signals that client wants to quit the game.
	 */
	public void quit();
	
	/**
	 * @param key The constant from the class Keys corresponding to this key.
	 * @param value Whether or not the key is pressed.
	 */
	public void setKey(int key, boolean value);
	
	/**
	 * Signals that the mouse pointer has moved.
	 * @param amount by which pointer has moved.
	 */
	public void movePointer(short amount);
	
	/**
	 * Sends a talk to the server.
	 * @param message Message to send to server.
	 */
	public void talk(String message);
}
