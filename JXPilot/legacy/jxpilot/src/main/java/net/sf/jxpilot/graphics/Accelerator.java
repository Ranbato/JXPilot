package net.sf.jxpilot.graphics;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Class that handles accelerated graphics.
 * @author vlad
 *
 */
public class Accelerator {
	
	// Acquiring the current Graphics Device and Graphics Configuration
	public static final GraphicsEnvironment gfxEnv = GraphicsEnvironment.getLocalGraphicsEnvironment();
	public static final GraphicsDevice gfxDevice = gfxEnv.getDefaultScreenDevice();
	public static final GraphicsConfiguration gfxConfig = gfxDevice.getDefaultConfiguration();
	
	public static BufferedImage createCompatibleImage(int width, int height)
	{
		return gfxConfig.createCompatibleImage(width, height);
	}
}