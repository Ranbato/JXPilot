package net.sf.jxpilot;

import net.sf.xpilotpanel.client.ClientRunner;
import net.sf.jgamelibrary.preferences.Preferences;

public class DefaultClientRunner implements ClientRunner {
	private Client client;
	
	public void runClient(String serverIP, int serverPort, Preferences prefs) {
		System.out.println("Running client!");
		client = new Client(prefs);
		client.runClient(serverIP, serverPort);
	}
	
	public Client getClient() {return client;}
}
