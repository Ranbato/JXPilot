package net.sf.jxpilot.graphics;

import net.sf.jgamelibrary.graphics.FrameMode;

/**
 * Represents the various possible display modes of a JXPilotFrame.
 * This should be eliminated in favor of the equivalent {@link FrameMode}.
 * @author Vlad Firoiu
 */
public enum DisplayMode {
	
	/**
	 * Full Screen Exclusive Mode
	 */
	FSEM(FrameMode.FSEM),
	/**
	 * Undecorated Full Screen
	 */
	UFS(FrameMode.UFS), 
	 /**
	  * Almost Full Screen
	  */
	 AFS(FrameMode.AFS);
	
	/**
	 * The corresponding {@link FrameMode} constant.
	 */
	public final FrameMode FRAME_MODE;
	
	private DisplayMode(FrameMode mode) {FRAME_MODE = mode;}
	
	/**
	 * Used for getting DisplayMode from Preferences.
	 */
	public static final String displayModeKey = "displayMode";	
	
	public static DisplayMode getDisplayMode(String value) {
		for(DisplayMode d : values()) {
			if (d.toString().compareTo(value)==0) return d;
		}
		return null;
	}
}
