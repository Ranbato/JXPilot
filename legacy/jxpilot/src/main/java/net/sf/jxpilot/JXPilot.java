package net.sf.jxpilot;

import net.sf.xpilotpanel.XPilotPanel;
import net.sf.xpilotpanel.gui.AboutWindow;
import net.sf.jgamelibrary.preferences.Preferences;
import net.sf.xpilotpanel.client.ClientRunner;

import java.awt.Image;
import java.net.URL;
import java.util.HashMap;

public class JXPilot {

	/**
	 * Whether or not to print various packet information.
	 */
	public static final boolean PRINT_PACKETS = false;
	
	/**
	 * Whether or not to print reliable data.
	 */
	public static final boolean PRINT_RELIABLE = false;
	
	public static final short SERVER_MAIN_PORT = 15345;
	//public static final short CLIENT_PORT = 15345;
	
	public static final String LKRAUSS_ADDRESS = "213.239.204.35";
	public static final String STROWGER_ADDRESS = "149.156.203.245";
	public static final String CHAOS_ADDRESS = "129.13.108.207";
	public static final String BURKEN_ADDRESS = "83.168.206.7";
	public static final String LOCAL_LOOPBACK_ADDRESS = "127.0.0.1";
	public static final String SERVER_IP_ADDRESS = LOCAL_LOOPBACK_ADDRESS;
	
	/**
     * JXPilot's windows icon.
     */
    private static Image icon = null;
		
	public static void main(String[] args) {
		XPilotPanelRun(new DefaultClientRunner());

        //runClient(SERVER_IP_ADDRESS, SERVER_MAIN_PORT, null);
		
		//runClient(BURKEN_ADDRESS, SERVER_MAIN_PORT + 1, null);
	}

	public static void XPilotPanelRun(ClientRunner runner)
	{
        try {
            java.util.Map<String, Object> xpilotpanelEmbeddedParams = new HashMap<String, Object>();
            xpilotpanelEmbeddedParams.put(
                    XPilotPanel.EMBEDDED_PARAMETER_MAIN_WINDOW_TITLE,
                    "JXPilot - XPilotPanel");
            xpilotpanelEmbeddedParams.put(XPilotPanel.EMBEDDED_PARAMETER_ICON,
                    getJXPilotIcon());
            xpilotpanelEmbeddedParams.put(
                    XPilotPanel.EMBEDDED_PARAMETER_CLIENT_LAUNCH_METHOD,
                    runner);

            XPilotPanel.embeddedLaunch(xpilotpanelEmbeddedParams);
        }
        catch (Exception e) {
            e.printStackTrace();
        }	
	}
	
	public static void runClient(String serverIP, int serverPort, Preferences prefs) {
		System.out.println("Running client!");
		new Client(prefs).runClient(serverIP, serverPort);
	}
	
    /**
     * Returns JXPilot's windows icon, retrieved from file and stored as
     * 'single'.
     * 
     * @return JXPilot icon.
     */
    public static Image getJXPilotIcon() {
        if (icon == null) {
            URL u = AboutWindow.class.getClassLoader().getResource(
                    "data/JXPilotIcon.png");
            if(u != null) {
                icon = new javax.swing.ImageIcon(u).getImage();
            } else
        	try {
        		icon = javax.imageio.ImageIO.read(new java.io.File("data/JXPilotIcon.png"));
        	}
        	catch(Exception e){e.printStackTrace();}
        
        }
        
        return icon;
    }
}
