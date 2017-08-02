package org.xpilot.client.net;

import net.sf.jgamelibrary.net.UDPBuffer;
import net.sf.jgamelibrary.preferences.Preferences;
import java.nio.ByteBuffer;
import net.sf.jxpilot.AbstractClient;
import net.sf.jxpilot.data.Keys;
import net.sf.jxpilot.map.BlockMap;
import net.sf.jxpilot.map.BlockMapSetup;
import net.sf.jxpilot.util.BitVector;
import net.sf.jxpilot.util.HolderList;
import net.sf.jxpilot.util.Utilities;

import javax.swing.*;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.SocketTimeoutException;
import java.nio.channels.DatagramChannel;

import static net.sf.jxpilot.JXPilot.PRINT_PACKETS;
import static net.sf.jxpilot.net.Ack.putAck;
import static net.sf.jxpilot.net.packet.Packet.*;
import static net.sf.jxpilot.net.packet.ReplyData.readReplyData;

public class NetClient {
	//private final Random rnd = new Random();

	/**
	 * This client's version. Currently 0x4401 is used by fxi.
	 */
	public static final int CLIENT_VERSION = 0x4401;
	/**
	 * The magic password shared by all xpilot clients and servers.
	 */
	public static final int MAGIC_PASSWORD = 0xF4ED;
	
	/**
	 * The first 2 bytes are the client version.
	 * The last 2 bytes are the magic password.
	 */
	public static final int MAGIC = CLIENT_VERSION<<2*8 | MAGIC_PASSWORD;
	
	//these are used for testing, should be changed later to support preferences
	public static final int TEAM = 0x0000FFFF;
	public static final String DISPLAY = "";
	public static final short POWER = 55;
	public static final short TURN_SPEED = 8;
	public static final short TURN_RESISTANCE = 0;
	public static final byte MAX_FPS = (byte)50;
	public static final byte[] MOTD_BYTES = {0,0,0x47,2,0,0x43,3};

	//used for sending display
	public static final short WIDTH_WANTED = 0x400;
	public static final short HEIGHT_WANTED = 0x400;
	public static final byte NUM_SPARK_COLORS = 0x08;
	public static final byte SPARK_RAND = 0x33;

	private final ShipShape SHIP = ShipShape.DEFAULT_SHIP;

	// Nick, user and host to send to server. This default values are for if
    // JXPilot was lauched w/o XPilotPanel.
	private Preferences preferences;
    private String nick = null, real_name = null, host = null;

    /**
     * Amount of time to wait for server before giving up, in milliseconds.
     */
    public static final int SOCKET_TIMEOUT = 5*1000;
    
    /**
	 * 2^12=4096
	 */
	public static final int MAX_PACKET_SIZE = 4096;
	
	/**
	 * Largest possible map size = 2^16.
	 */
	public static final int MAX_MAP_SIZE = 65536;
	
	private int local_port;
	private InetSocketAddress server_address;
	private DatagramSocket socket;
	private DatagramChannel channel;
	private UDPBuffer out = new UDPBuffer(MAX_PACKET_SIZE);
	private UDPBuffer in = new UDPBuffer(MAX_PACKET_SIZE);
	private UDPBuffer map = new UDPBuffer(MAX_MAP_SIZE);
	private UDPBuffer reliableBuf = new UDPBuffer(MAX_PACKET_SIZE);
	private BlockMapSetup setup = new BlockMapSetup();
	private final PacketProcessor[] processors = new PacketProcessor[256];
	private ReplyData reply = new ReplyData();
	private ReliableData reliable = new ReliableData();
	private ReplyMessage message = new ReplyMessage();
	private BitVector keyboard = new BitVector(Keys.NUM_KEYS);
	private int last_keyboard_change=0;
	
	private short turn_speed, new_speed;
	
	/**
	 * Keeps track last frame update number.
	 * This is also used for sending acknowledgments.
	 */
	private int last_loops;
	private volatile boolean quit = false;
	
	/**
	 * Keeps track of how far the mouse pointer has moved since the last frame update.
	 */
	private volatile short pointer_move_amount = 0;
	
	public static final int TALK_RETRY = 2;
	/**
	 * Used for sending talks.
	 */
	private volatile int talk_pending=0,talk_sequence_num=0, talk_last_send;
	
	//private Queue<String> talkQueue = new ArrayDeque<String>();
	private String talkString;
	
	//for measurement
	private long numPackets = 0, numPacketsReceived = 0, numFrames = 0, startTime;
	
	private int server_version;
	
	private AbstractClient client;	
	
	public NetClient(AbstractClient client)
	{
		this.client = client;
		
		//sets functions to handle packets
		
		//unreliable types
		processors[PKT_EYES]		= getEyesProcessor();
		processors[PKT_TIME_LEFT]	= getTimeLeftProcessor();
		processors[PKT_AUDIO]		= getAudioProcessor();
		processors[PKT_START]		= getStartProcessor();
		processors[PKT_END]			= getEndProcessor();
		processors[PKT_SELF]		= getSelfProcessor();
		processors[PKT_DAMAGED]		= getDamagedProcessor();
		processors[PKT_CONNECTOR]	= getConnectorProcessor();
		processors[PKT_LASER]		= getLaserProcessor();
		processors[PKT_REFUEL]		= getRefuelProcessor();
		processors[PKT_SHIP]		= getShipProcessor();
		processors[PKT_ECM]			= getECMProcessor();
		processors[PKT_TRANS]		= getTransProcessor();
		processors[PKT_PAUSED]		= getPausedProcessor();
		processors[PKT_ITEM]		= getItemProcessor();
		processors[PKT_MINE]		= getMineProcessor();
		processors[PKT_BALL]		= getBallProcessor();
		processors[PKT_MISSILE]		= getMissileProcessor();
		processors[PKT_SHUTDOWN]	= getShutdownProcessor();
		processors[PKT_DESTRUCT]	= getDestructProcessor();
		processors[PKT_SELF_ITEMS]	= getSelfItemsProcessor();
		processors[PKT_FUEL]		= getFuelProcessor();
		processors[PKT_CANNON]		= getCannonProcessor();
		processors[PKT_TARGET]		= getTargetProcessor();
		processors[PKT_RADAR]		= getRadarProcessor();
		processors[PKT_FASTRADAR]	= getFastRadarProcessor();
		processors[PKT_RELIABLE]	= getReliableProcessor();
		processors[PKT_QUIT]		= getQuitProcessor();
		processors[PKT_MODIFIERS]  	= getModifiersProcessor();
		processors[PKT_FASTSHOT]	= getFastShotProcessor();
		processors[PKT_THRUSTTIME] 	= getThrustTimeProcessor();
		processors[PKT_SHIELDTIME] 	= getShieldTimeProcessor();
		processors[PKT_PHASINGTIME]	= getPhasingTimeProcessor();
		processors[PKT_ROUNDDELAY] 	= getRoundDelayProcessor();
		processors[PKT_LOSEITEM]	= getLoseItemProcessor();
		processors[PKT_WRECKAGE]	= getWreckageProcessor();
		processors[PKT_ASTEROID]	= getAsteroidProcessor();
		processors[PKT_WORMHOLE]	= getWormholeProcessor();
		
		PacketProcessor debrisProcessor = getDebrisProcessor();	
		int pkt_debris = Utilities.getUnsignedByte(PKT_DEBRIS);
		for (int i = 0;i<DEBRIS_TYPES;i++) {
			processors[i+pkt_debris] = debrisProcessor;
		}
		
		//reliable types
		processors[PKT_MOTD]		= getMOTDProcessor();
		processors[PKT_MESSAGE]		= getMessageProcessor();
		processors[PKT_TEAM_SCORE]	= getTeamScoreProcessor();
		processors[PKT_PLAYER]		= getPlayerProcessor();
		processors[PKT_SCORE]		= getScoreProcessor();
		processors[PKT_TIMING]		= getTimingProcessor();
		processors[PKT_LEAVE]		= getLeaveProcessor();
		processors[PKT_WAR]			= getWarProcessor();
		processors[PKT_SEEK]		= getSeekProcessor();
		processors[PKT_BASE]		= getBaseProcessor();
		processors[PKT_QUIT]		= getQuitProcessor();
		processors[PKT_STRING]		= getStringProcessor();
		processors[PKT_SCORE_OBJECT]= getScoreObjectProcessor();
		processors[PKT_TALK_ACK]	= getTalkAckProcessor();
		processors[PKT_REPLY]		= getReplyProcessor();
		
		// Processing preferences for this client.
		preferences = client.getPreferences();
		if (preferences != null) {
			nick = preferences.get("XPilotName");
			real_name = preferences.get("XPilotUser");
			host = preferences.get("XPilotHost");
		}

		if (nick == null || nick.isEmpty())
			nick = System.getProperty("user.name");
		if (real_name == null || real_name.isEmpty())
			real_name = nick;
		if (host == null || host.isEmpty())
			host = "java.client";

	}

	public String getNick(){return nick;}
	public String getRealName(){return real_name;}
	public String getHost(){return host;}
	
	public void runClient(String serverIP, int server_port)
	{
		try {
			server_address = new InetSocketAddress(serverIP, server_port);
			socket = new DatagramSocket();
			
			//channel = DatagramChannel.open();
			//socket = channel.socket();
			socket.connect(server_address);
			socket.setSoTimeout(SOCKET_TIMEOUT);
			//channel.connect(server_address);

			local_port = socket.getLocalPort();
			//System.out.println("Socket local port: " + local_port);
		} catch(IOException e) {
			e.printStackTrace();
			System.exit(1);
		}

		try {
			System.out.println("Beginning communications with " + server_address);
			
			System.out.println("Sending join request.");
			sendJoinRequest(out, real_name, local_port, nick, host, TEAM);
			sendJoinRequest(out, real_name, local_port, nick, host, TEAM);
			
			System.out.println("Wating for reply message.");
			getReplyMessage(in, message);
			System.out.println('\n' + message.toString());

			if(message.getStatus() != Status.SUCCESS) {
				JOptionPane.showMessageDialog(null, "Can't connect to server.");
				System.out.println("Can't connect to server.");
				return;
			}
			
			server_version = message.getMagic() >> 2*8;
			System.out.println(String.format("\nserver version = %x", server_version));
			
			while(message.getPack()!=ENTER_GAME_pack) {
				getReplyMessage(in, message);
				System.out.println('\n' + message.toString());
			}

			server_port = Utilities.getUnsignedShort(message.getValue());
			System.out.println("New server port: "+server_port);

			try {
				socket.disconnect();
				//socket.close();
				server_address = new InetSocketAddress(serverIP, server_port);
				//socket = new DatagramSocket(local_port);
				socket.connect(server_address);
			} catch(IOException e) {
				e.printStackTrace();
				System.exit(1);
			}
			
			System.out.println("Sending Verify");
			sendVerify(out, real_name, nick);
			sendVerify(out, real_name, nick);
			
			System.out.println("Waiting for reliable data.");
			
			ReliableDataError result=null;
			while (result!=ReliableDataError.NO_ERROR) {
				result = getReliableData(reliable, in);
				//System.out.println(result);

				if (result != ReliableDataError.BAD_PACKET && result != ReliableDataError.NOT_RELIABLE_DATA) {
					sendPacket(out);//sends ack to reliable data
				}
			}

			System.out.println("Starting net setup.");
			netSetup(in, map, setup, reliable);

			//map.flip();
			System.out.println(map.length()+"\n\nMap:\n");

			if(PRINT_PACKETS) setup.printMapData();

			client.mapInit(new BlockMap(setup));

			System.out.println("\nSending Net Start");
			netStart(out);

			//keyboard.clearBits();
			//keyboard.setBit(Keys.KEY_TURN_RIGHT, true);

			System.out.println("\nStarting input loop.");
			startTime = System.currentTimeMillis();

			inputLoop();

			System.out.println("\nEnd of input loop");
		} catch(SocketTimeoutException e) {
			System.out.println("\nServer timed out.");
			client.handleTimeout();
		} catch(PacketReadException e) {
			System.out.println(e);
		}
	}
	
	/**
	 * Main game method. Interprets server input and sends messages to client.
	 * The pointer, keyboard, and talk info are then sent to the server.
	 * @throws InterruptedIOException If the server takes too long.
	 */
	private void inputLoop() throws SocketTimeoutException
	{
		/*
		try {
			channel.configureBlocking(true);
		} catch(IOException e) {
			e.printStackTrace();
		}
		*/
		
		//long lastFrameTime = 0;
		//long min_interval = 1000000000/MAX_FPS;
		//long currentFrameTime;
		
		while(!quit) {
			
			//receivePacket(in);
			//readLatestPacket(in);
			netRead(in);
			
			netPacket(in, reliableBuf);
			
			client.handlePacketEnd();
			
			putPointerMove(out);
			putKeyboard(out, keyboard);
			sendTalk(out);
			sendPacket(out);
			out.clear();
		}
		
		sendQuit();
	}
	
	
	/*
	 //Read a packet into one of the input buffers.
	 //If it is a frame update then we check to see
	 //if it is an old or duplicate one.  If it isn't
	 //a new frame then the packet is discarded and
	 //we retry to read a packet once more.
	 //It's a non-blocking read.
	 //
	static int Net_read(frame_buf_t *frame)
	{
	    int		n;
	    long	loop;
	    u_byte	ch;

	    frame->loops = 0;
	    for (;;) {
		Sockbuf_clear(&frame->sbuf);
		if (Sockbuf_read(&frame->sbuf) == -1) {
		    error("Net input error");
		    return -1;
		}
		if (frame->sbuf.len <= 0) {
		    Sockbuf_clear(&frame->sbuf);
		    return 0;
		}
		//IFWINDOWS( Trace("Net_read: read %d bytes type=%d\n",
		//frame->sbuf.len, frame->sbuf.ptr[0]) );
		if (frame->sbuf.ptr[0] != PKT_START)
		     //Don't know which type of packet this is
		     //and if it contains a frame at all (not likely).
		     //It could be a quit packet.
		    return 1;

		// Peek at the frame loop number.
		n = Packet_scanf(&frame->sbuf, "%c%ld", &ch, &loop);
		//IFWINDOWS( Trace("Net_read: frame # %d\n", loop) );
		frame->sbuf.ptr = frame->sbuf.buf;
		if (n <= 0) {
		    if (n == -1) {
			Sockbuf_clear(&frame->sbuf);
			return -1;
		    }
		    continue;
		}
		else if (loop > last_loops) {
		    frame->loops = loop;
		    return 2;
		} else {
		    // Packet out of order.  Drop it.
		    // We may have already drawn it if it is duplicate.
		    // Perhaps we should try to extract any reliable data
		    // from it before dropping it.
		}
	    }
	    //IFWINDOWS( Trace("Net_read: wbuf->len=%d\n", wbuf.len) );
	}
	*/
	
	/**
	 * Reads a packet into an input buffer.
	 */
	private void netRead(UDPBuffer in) throws SocketTimeoutException {
		while(true) {
			in.clear();
			receivePacket(in);
			
			byte pkt_type = in.peekByte();
			if(pkt_type != Packet.PKT_START) {
				//System.out.println("Quit packets?");
				//System.out.println("packet type = " + pkt_type);
				break;
			}
			
			int loop = in.peekInt(1);
			if(loop > last_loops) break;//packet is ok
			else {
				//Packet out of order. Drop it.
				System.out.println("Packet out of order.");
			}
		}
	}
	
	/**
	 * Temporary buffer used to store packets read from network.
	 */
	private ByteBuffer temp = new ByteBuffer(MAX_PACKET_SIZE);
	/**
	 * Reads the last packet sent into in.
	 * @param in The UDPBuffer in which the packet is read.
	 */
	private void readLatestPacket(UDPBuffer in) throws SocketTimeoutException {
		try {
			channel.configureBlocking(true);
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		receivePacket(in);
		
		try {
			channel.configureBlocking(false);
		} catch(IOException e) {
			e.printStackTrace();
		}
		
		do {
			temp.clear();
			temp.putBytes(in);
		} while(receivePacket(in)>0);
		
		in.clear();
		in.putBytes(temp);
	}
	
	public void putJoinRequest(ByteBuffer buf, String real_name, int port, String nick, String host, int team) {
		buf.putInt(MAGIC);
		buf.putString(real_name);
		//buf.put(PKT_MESSAGE);
		buf.putShort((short)port);
		
		buf.put(ENTER_QUEUE_pack);
		buf.putString(nick);
		
		buf.putString(DISPLAY);
		buf.putString(host);
		buf.putInt(team);
	}
	
	public void sendJoinRequest(UDPBuffer buf, String real_name, int port, String nick, String host, int team) {
		buf.clear();	
		putJoinRequest(buf, real_name, port, nick, host, team);
		/*
		System.out.println(buf.getInt());
		System.out.println(buf.getString());
		System.out.println(buf.getShort());
		System.out.println(buf.getByte());
		System.out.println(buf.getString());
		System.out.println(buf.getString());
		System.out.println(buf.getString());
		System.out.println(buf.getInt());
		buf.clear();
		putJoinRequest(buf, real_name, port, nick, host, team);
		*/
		
		try {
			//buf.flip();
			buf.send(socket);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Note that this method clears buf.
	 * @param buf The buffer in which the input should be received.
	 * @return The number of bytes read.
	 * 
	 */
	private int receivePacket(UDPBuffer buf) throws SocketTimeoutException {
		buf.clear();
		try {
			int read = buf.receive(socket);
			numPackets++;
			numPacketsReceived++;
			if(PRINT_PACKETS) System.out.println("\nGot Packet-number: " + numPackets + ", " + read + " bytes.");
			return read;
		} catch(SocketTimeoutException e) {
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
			return 0;
		}
	}
	
	/**
	 * Sends a packet to the server.
	 * Note that this method clears the buffer.
	 * @param buf The buffer from which the output should be sent.
	 * @see {@link ByteBuffer#sendPacket(DatagramChannel, SocketAddress)}
	 */
	private void sendPacket(UDPBuffer buf) {
		if (buf.length() == 0) return;
		
		try {
			buf.send(socket);
			numPackets++;
			buf.clear();
		} catch(IOException e) {
			e.printStackTrace();
		}
	}

	private static void putVerify(ByteBuffer buf, String real_name, String nick) {
		//System.out.println("Putting verify.");
		buf.put(PKT_VERIFY);
		buf.putString(real_name);
		buf.putString(nick);
		buf.putString(DISPLAY);
	}
	
	private void sendVerify(UDPBuffer buf, String real_name, String nick) {
		buf.clear();
		putVerify(buf, real_name, nick);
		//System.out.println("Verify put.");
		try {
			buf.send(socket);
		} catch(IOException e) {
			e.printStackTrace();
		}
		//System.out.println("Verify sent.");
	}

	private void sendAck(ByteBuffer buf, Ack ack) {
		putAck(buf, ack);
		if(PRINT_PACKETS) System.out.println('\n' + ack.toString());
	}
	
	public void sendAck(Ack ack) {
		sendAck(out, ack);
	}
	
	private static void putPower(ByteBuffer buf, short power) {
		buf.put(PKT_POWER);
		buf.putShort((short)(power*256));
	}
	private static void putPowerS(ByteBuffer buf, short power) {
		buf.put(PKT_POWER_S);
		buf.putShort((short)(power*256));
	}
	
	private static void putTurnSpeed(ByteBuffer buf, short turn_speed) {
		buf.put(PKT_TURNSPEED);
		buf.putShort((short)(turn_speed*256));
	}
	private static void putTurnSpeedS(ByteBuffer buf, short turn_speed) {
		buf.put(PKT_TURNSPEED_S);
		buf.putShort((short)(turn_speed*256));
	}
	private static void putTurnResistance(ByteBuffer buf, short turn_resistance) {
		buf.put(PKT_TURNRESISTANCE);
		buf.putShort((short)(turn_resistance*256));
	}
	private static void putTurnResistanceS(ByteBuffer buf, short turn_resistance) {
		buf.put(PKT_TURNRESISTANCE_S);
		buf.putShort((short)(turn_resistance*256));
	}
	private static void putDisplay(ByteBuffer buf) {
		buf.put(PKT_DISPLAY);
		buf.putShort(WIDTH_WANTED);
		buf.putShort(HEIGHT_WANTED);
		buf.put(NUM_SPARK_COLORS);
		buf.put(SPARK_RAND);
	}
	/**
	 * Puts a request for the MOTD.
	 * TODO: Make this method work correctly. Currently, it does not put in
	 * a correct MOTD request.
	 * @param buf The buffer to put the request into.
	 */
	private static void putMOTDRequest(ByteBuffer buf) {
		buf.put(PKT_MOTD);
		buf.putBytes(MOTD_BYTES);
	}
	
	/**
	 * 
	 * @param buf
	 * @param max_fps
	 */
	private static void putFPSRequest(ByteBuffer buf, byte max_fps) {
		buf.put(PKT_ASYNC_FPS);
		buf.put(max_fps);
	}
	
	/**
	 * Try to send a `start play' packet to the server and get an
	 * acknowledgement from the server.  This is called after
	 * we have initialized all our other stuff like the user interface
	 * and we also have the map already.
	 * 
	 * This method sends our PKT_SHAPE, ShipShape, PKT_PLAY, our power, turnspeed,
	 * turn resistance, display, and max fps request.
	 * 
	 */
	private void netStart(UDPBuffer out) {
		out.clear();
		
		out.put(PKT_SHAPE);
		out.putString(SHIP.toString());
		
		out.put(PKT_PLAY);
		putPower(out, POWER);
		putPowerS(out, POWER);
		putTurnSpeed(out, TURN_SPEED);
		putTurnSpeedS(out, TURN_SPEED);
		putTurnResistance(out, TURN_RESISTANCE);
		putTurnResistanceS(out, TURN_RESISTANCE);
		putDisplay(out);
		//putMOTDRequest(out);
		putFPSRequest(out, MAX_FPS);
		
		//out.flip();
		
		sendPacket(out);
	}
	
	
	private ReliableDataError getReliableData(ReliableData data, UDPBuffer in) throws SocketTimeoutException, PacketReadException {
		in.clear();
		receivePacket(in);
		//in.flip();
		
		return data.readReliableData(in, this);
	}
	
	private ReplyMessage getReplyMessage(UDPBuffer in, ReplyMessage message) throws SocketTimeoutException {
		receivePacket(in);
		//buf.flip();
		return ReplyMessage.readReplyMessage(in, message);
	}
	
	private static int readMapPacket(ByteBuffer in, ByteBuffer map, ReliableData reliable) {
		//readReliableData(reliable, in);
		int remaining = in.length();
		System.out.println("Reliable len = "+reliable.getLen()+"\nMapPacket remaining = " + remaining);
		map.putBytes(in);
		return remaining;
	}
	
	private int getMapPacket(UDPBuffer in, ByteBuffer map, ReliableData reliable) throws SocketTimeoutException, PacketReadException {
		ReliableDataError error = getReliableData(reliable, in);
		
		if (error == ReliableDataError.NO_ERROR) {
			sendPacket(out);//sends the appropriate ack
			return readMapPacket(in, map, reliable);
		}
		
		return -1;
		
		//sendAck(out, Ack.ack.setAck(reliable));
		//System.out.println(reliable);
	}
	
	private static int readFirstMapPacket(ByteBuffer in, ByteBuffer map, BlockMapSetup setup, ReliableData reliable) {
		setup.readMapSetup(in);
		int remaining = in.length();
		System.out.println("Reliable len = "+reliable.getLen()+"\nFirstMapPacket remaining = " + remaining);
		map.putBytes(in);
		return remaining;
	}
	
	private int getFirstMapPacket(UDPBuffer in, ByteBuffer map, BlockMapSetup setup, ReliableData reliable)
			throws SocketTimeoutException, PacketReadException {
		ReliableDataError error = getReliableData(reliable, in);
		
		while(error!=ReliableDataError.NO_ERROR) {
			System.out.println("Didn't get reliable data when waiting for map\n" + error + "\n" + reliable);
			if(error == ReliableDataError.DUPLICATE_DATA)
				sendPacket(out);//send acks to server
			error = getReliableData(reliable, in);
		}
		
		sendPacket(out);
		
		return readFirstMapPacket(in, map, setup, reliable);		
	}
	
	private void putKeyboard(ByteBuffer buf, BitVector keyboard) {
		buf.put(PKT_KEYBOARD);
		buf.putInt(last_keyboard_change);
		
		synchronized(keyboard)
		{	
			buf.putBytes(keyboard.getBytes());
		}
		
		last_keyboard_change++;
	}
	
	public void movePointer(short amount) {
		pointer_move_amount += amount;
	}
	
	private void putPointerMove(ByteBuffer buf) {
		if(pointer_move_amount!=0) {
			buf.put(PKT_POINTER_MOVE);
			buf.putShort(pointer_move_amount);
			pointer_move_amount = 0;
		}
	}
	
	/**
	 * Receive the map data and some game parameters from
	 * the server.  The map data may be in compressed form.
	 * 
	 * Receives the first map packet, which has the setup, which
	 * tells us how much data the map has.
	 * 
	 * Then keeps receiving packets until done w/ data.
	 * 
	 * Uncompresses the map if necessary.
	 * 
	 */
	private void netSetup(UDPBuffer in, ByteBuffer map, BlockMapSetup setup, ReliableData reliable)
			throws SocketTimeoutException, PacketReadException {
		int i = getFirstMapPacket(in, map, setup, reliable);
		System.out.println(setup);
		
		//map.flip();
		
		int todo = setup.getMapDataLen()-i;
		
		//map.flip();
		
		while(todo>0) {
			i = getMapPacket(in, map, reliable);
			if (i>=0) todo -= i;
		}
		
		if (setup.getMapOrder() != BlockMapSetup.SETUP_MAP_UNCOMPRESSED) {
			//map.flip();
			setup.uncompressMap(map);
		}
	}
	
	/**
	 * Process a packet which most likely is a frame update,
	 * perhaps with some reliable data in it.
	 * 
	 * loops through in processing packets
	 * then loops through reliableBuf and processes as many packets as possible 
	 */
	private void netPacket(ByteBuffer in, ByteBuffer reliableBuf)
	{
		while(in.length()>0)
		{
			short type = in.peekUnsignedByte();
			
			if (processors[type]!=null)
			{
				try
				{
					processors[type].processPacket(in);
				}
				catch(PacketReadException e)
				{
					in.clear();
					e.printStackTrace();
				}
			}
			else
			{
				System.out.println("**********Unsupported type: " + type + "************");
				in.clear();
				break;
			}
		}

		//if(reliableBuf.length() > 0) System.out.println("reliable buffer length: " + reliableBuf.length());
		
		while (reliableBuf.length()>0)
		{
			if(PRINT_PACKETS)System.out.println("\nAttempting to read from reliableBuf");

			short type = reliableBuf.peekUnsignedByte();

			if(processors[type]!=null)
			{
				int pos = reliableBuf.getReader();//stores reliable buffer's position
				try
				{
					processors[type].processPacket(reliableBuf);
				}
				catch (ReliableReadException e) //happens if reliable data is broken up
				{
					if(PRINT_PACKETS) System.out.println("Fragmented reliable packet of type: " + type);
					reliableBuf.setReader(pos);
					break;
				}
				catch(PacketReadException e)//this should not happen
				{
					e.printStackTrace();
					break;
				}
			}
			else
			{
				System.out.println("**********Unsupported Reliable type: " + type + "************");
				break;
			}	
		}
		
		if (reliableBuf.length()==0) {
			reliableBuf.clear();
		}
	}

	//public send methods for Client
	
	/**
	 * Number of quit packets to send to server.
	 */
	public static final int NUM_QUITS = 5;

	/**
	 * NetClient attempts to send a talk to the server.
	 * @param message Message to send to server.
	 */
	public void netTalk(String message)
	{
		talkString = message;
		talk_pending = ++talk_sequence_num;
		talk_last_send = last_loops - TALK_RETRY;
	}
	
	private void sendTalk(ByteBuffer out)
	{
		if (talk_pending == 0 || last_loops - talk_last_send < TALK_RETRY) {
			return;
		}
		
		/*
		if (Packet_printf(&wbuf, "%c%ld%s", PKT_TALK,
				talk_pending, talk_str) == -1) {
			return -1;
		}
		*/
		
		out.put(PKT_TALK);
		out.putInt(talk_pending);
		//for(String s : talkQueue)
		out.putString(talkString);
		
		talk_last_send = last_loops;
	}

	/**
	 * Changes the turnspeed.
	 */
	public void netTurnSpeed(short new_speed) {
		this.new_speed = turn_speed;
	}
	
	private void sendTurnSpeed() {
		if(new_speed != turn_speed) {
			putTurnSpeed(out, new_speed);
			//this.putTurnSpeedS(out, turn_speed);
			new_speed = turn_speed;
		}
	}
	
	/**
	 * Stops input loop and sends quit packets.
	 */
	public void quit() {
		quit = true;
		//sendQuit();
	}
	
	private void sendQuit()
	{
		System.out.println("\nSending quit packets");
		synchronized(out)
		{
			for (int i = 0;i<NUM_QUITS;i++)
			{
				out.clear();
				out.put(PKT_QUIT);
				sendPacket(out);
			}
		}
		
		System.out.println('\n' + getStatistics());
		
	}
	
	public String getStatistics() {
		long runTime = (System.currentTimeMillis()-startTime)/1000;
		return "Client ran for " + runTime + " seconds.\n" +
		numPacketsReceived + " packets were received.\n" +
		numFrames + " frames were received.\n" +
		"Average fps = " + (double)numFrames/runTime;
	}
	
	//other methods client might need
	/*
	public BitVector getKeyboard() {
		synchronized(keyboard) {
			return keyboard;
		}
	}
	 */
	
	public void setKey(int key, boolean value) {
		synchronized(keyboard) {
			keyboard.setBit(key, value);
		}
	}
	
	
	//various classes to handle different packet types
	
	/**
	 * Processes XPilot packets.
	 * @author Vlad Firoiu
	 */
	protected interface PacketProcessor {
		public void processPacket(ByteBuffer in) throws PacketReadException;	
		//static final ReliableReadException reliableReadException = new ReliableReadException();
	}
	
	private int reliable_count;
	/**
	 * Processes reliable packets.
	 * @author Vlad Firoiu
	 */
	protected class ReliableProcessor implements PacketProcessor {
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			ReliableDataError error = reliable.readReliableData(in, NetClient.this, reliableBuf);
			if(PRINT_PACKETS) System.out.println(error);
			reliable_count++;
			//System.out.println("count: " + reliable_count + ", size: " + reliable.getLen());
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate reliable
	 * processor is to be used.
	 * @return A new {@code ReliableProcessor} object.
	 */
	protected PacketProcessor getReliableProcessor() {return new ReliableProcessor();}
	
	/**
	 * Processes reply packets.
	 * @author Vlad Firoiu
	 */
	protected class ReplyProcessor implements PacketProcessor {
		@Override
		public void processPacket(ByteBuffer in) throws ReliableReadException {
			ReplyData data = readReplyData(in, reply);
			if(PRINT_PACKETS) System.out.println(data);
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate reply
	 * processor is to be used.
	 * @return A new {@code ReplyProcessor} object.
	 */
	protected PacketProcessor getReplyProcessor() {return new ReplyProcessor();}
	
	/**
	 * Processes quit packets.
	 * @author Vlad Firoiu
	 */
	protected class QuitProcessor implements PacketProcessor {
		protected QuitPacket quitPacket = new QuitPacket();
		@Override
		public void processPacket(ByteBuffer in) throws ReliableReadException {
			quitPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n'+quitPacket.toString());
			client.handleQuit(quitPacket);
			System.out.println("Server closed connection: " + quitPacket.getReason());
		}
	}

	/**
	 * Note that subclasses should override this method if a separate quit
	 * processor is to be used.
	 * @return A new {@code QuitProcessor} object.
	 */
	protected PacketProcessor getQuitProcessor() {return new QuitProcessor();}
	
	/**
	 * Processes start packets.
	 * @author Vlad Firoiu
	 */
	protected class StartProcessor implements PacketProcessor {
		protected StartPacket startPacket = new StartPacket();
		
		@Override
		public void processPacket(ByteBuffer in) throws ReliableReadException {
			startPacket.readPacket(in);

			//packet is duplicate or out of order
			if (last_loops >= startPacket.getLoops()) {
				in.clear();
				System.out.println("Packet duplicate or out of order.");
				return;
			}

			numFrames++;
			last_loops = startPacket.getLoops();
			
			if(PRINT_PACKETS) System.out.println('\n' + startPacket.toString());
			
			client.handleStart(startPacket.getLoops());
			message_count = 0;
			reliable_count = 0;
		}
		
		/*
			int		n;
			long	loops;
			u_byte	ch;
			long	key_ack;

			if ((n = Packet_scanf(&rbuf,
					"%c%ld%ld",
					&ch, &loops, &key_ack)) <= 0) {
				return n;
			}
			if (last_loops >= loops) {

				// Packet is duplicate or out of order.

				Net_measurement(loops, PACKET_DROP);
				printf("ignoring frame (%ld)\n", last_loops - loops);
				return 0;
			}
			last_loops = loops;
			if (key_ack > last_keyboard_ack) {
				if (key_ack > last_keyboard_change) {
					printf("Premature keyboard ack by server (%ld,%ld,%ld)\n",
							last_keyboard_change, last_keyboard_ack, key_ack);

					 // Packet could be corrupt???
					 // Some blokes turn off checksumming.

					return 0;
				}
				else {
					last_keyboard_ack = key_ack;
				}
			}
			Net_lag_measurement(key_ack);
			if ((n = Handle_start(loops)) == -1) {
				return -1;
			}
			return 1;
		}
		 */
	}

	/**
	 * Note that subclasses should override this method if a separate start
	 * start is to be used.
	 * @return A new {@code ReliableProcessor} object.
	 */
	protected PacketProcessor getStartProcessor(){return new StartProcessor();}
	
	/**
	 * Processes Player packets.
	 * @author Vlad Firoiu
	 */
	protected class PlayerProcessor implements PacketProcessor {
		protected PlayerPacket playerPacket = new PlayerPacket();
		@Override
		public void processPacket(ByteBuffer in) throws ReliableReadException {
			playerPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + playerPacket.toString());
			client.handlePlayer(playerPacket);
		}
	}

	/**
	 * Note that subclasses should override this method if a separate player
	 * processor is to be used.
	 * @return A new {@code PlayerProcessor} object.
	 */
	protected PacketProcessor getPlayerProcessor(){return new PlayerProcessor();}
	
	/**
	 * Processes score packets.
	 * @author Vlad Firoiu
	 */
	protected class ScoreProcessor implements PacketProcessor {
		protected ScorePacket scorePacket = new ScorePacket();

		@Override
		public void processPacket(ByteBuffer in) throws ReliableReadException {
			scorePacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + scorePacket.toString());
			client.handleScore(scorePacket.getId(), scorePacket.getScore(), scorePacket.getLife());
		}

		/*
			int Receive_score(void)
			{
				int			n;
				short		id, life;
				DFLOAT		score = 0;
				u_byte		ch, mychar, alliance = ' ';

				if (version < 0x4500) {
					short	rcv_score;
					n = Packet_scanf(&cbuf, "%c%hd%hd%hd%c", &ch,
							&id, &rcv_score, &life, &mychar);
					score = rcv_score;
					alliance = ' ';
				} else {
					// newer servers send scores with two decimals 
					int	rcv_score;
					n = Packet_scanf(&cbuf, "%c%hd%d%hd%c%c", &ch,
							&id, &rcv_score, &life, &mychar, &alliance);
					score = (DFLOAT)rcv_score / 100;
				}
				if (n <= 0) {
					return n;
				}
				if ((n = Handle_score(id, score, life, mychar, alliance)) == -1) {
					return -1;
				}
				return 1;
			}

		 */
	}

	/**
	 * Note that subclasses should override this method if a separate score
	 * processor is to be used.
	 * @return A new {@code ScoreProcessor} object.
	 */
	protected PacketProcessor getScoreProcessor() {return new ScoreProcessor();}
	
	/**
	 * Processes base packets.
	 * @author Vlad Firoiu
	 */
	protected class BaseProcessor implements PacketProcessor {
		protected final BasePacket basePacket = new BasePacket();
		@Override
		public void processPacket(ByteBuffer in) throws ReliableReadException {
			basePacket.readPacket(in);
			if(PRINT_PACKETS)System.out.println('\n' + basePacket.toString());
			client.handleBase(basePacket);
		}
	}

	/**
	 * Note that subclasses should override this method if a separate base
	 * processor is to be used.
	 * @return A new {@code BaseProcessor} object.
	 */
	protected PacketProcessor getBaseProcessor(){return new BaseProcessor();}
	
	private int message_count;
	/**
	 * Processes message packets.
	 * @author Vlad Firoiu
	 */
	protected class MessageProcessor implements PacketProcessor {
		protected final MessagePacket messagePacket = new MessagePacket();
		@Override
		public void processPacket(ByteBuffer in) throws ReliableReadException {
			messagePacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + messagePacket.toString());
			client.handleMessage(messagePacket.getMessage());
			message_count++;
			
			//System.out.println(message_count + "\t" + messagePacket.getMessage());
		}
	}

	/**
	 * Note that subclasses should override this method if a separate message
	 * processor is to be used.
	 * @return A new {@code MessageProcessor} object.
	 */
	protected PacketProcessor getMessageProcessor(){return new MessageProcessor();}
	
	/**
	 * Processes debris packets.
	 * @author Vlad Firoiu
	 */
	protected class DebrisProcessor implements PacketProcessor {
		protected final DebrisPacket debrisPacket = new DebrisPacket();
		protected final AbstractDebrisHolder debrisHolder = new AbstractDebrisHolder();
		
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			debrisPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + debrisPacket.toString());
			for (int i = 0;i<debrisPacket.getNum();i++) {
				client.handleDebris(debrisHolder.setAbstractDebris(debrisPacket.getType()-DEBRIS_TYPES, in.getUnsignedByte(), in.getUnsignedByte()));
			}
		}
	}

	/**
	 * Note that subclasses should override this method if a separate debris
	 * processor is to be used.
	 * @return A new {@code DebrisProcessor} object.
	 */
	protected PacketProcessor getDebrisProcessor(){return new DebrisProcessor();}

	/**
	 * Processes self items packets.
	 * @author Vlad Firoiu
	 */
	protected class SelfItemsProcessor implements PacketProcessor {
		protected final SelfItemsPacket selfItemsPacket = new SelfItemsPacket();
		
		public void processPacket(ByteBuffer in) throws PacketReadException {
			selfItemsPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + selfItemsPacket.toString());
			client.handleSelfItems(selfItemsPacket.getItems());
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate self items
	 * processor is to be used.
	 * @return A new {@code SelfItemsProcessor} object.
	 */
	protected PacketProcessor getSelfItemsProcessor(){return new SelfItemsProcessor();}
	
	/**
	 * Processes self packets.
	 * @author Vlad Firoiu
	 */
	protected class SelfProcessor implements PacketProcessor {
		protected final SelfPacket selfPacket = new SelfPacket();
		
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			selfPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + selfPacket.toString());
			client.handleSelf(selfPacket);
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate self
	 * processor is to be used.
	 * @return A new {@code SelfProcessor} object.
	 */
	protected PacketProcessor getSelfProcessor(){return new SelfProcessor();}
	
	/**
	 * Processes modifiers packets.
	 * @author Vlad Firoiu
	 */
	protected class ModifiersProcessor implements PacketProcessor {
		protected final ModifiersPacket modifiersPacket = new ModifiersPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			modifiersPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + modifiersPacket.toString());
			client.handleModifiers(modifiersPacket.getModifiers());
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate modifiers
	 * processor is to be used.
	 * @return A new {@code ModifiersProcessor} object.
	 */
	protected PacketProcessor getModifiersProcessor(){return new ModifiersProcessor();}	
	
	/**
	 * Processes end packets.
	 * @author Vlad Firoiu
	 */
	protected class EndProcessor implements PacketProcessor {
		protected final EndPacket endPacket = new EndPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			endPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + endPacket.toString());
			client.handleEnd(endPacket.getLoops());
			if(message_count > 0) System.out.println("Message count = " + message_count);
		}
	}

	/**
	 * Note that subclasses should override this method if a separate end
	 * processor is to be used.
	 * @return A new {@code EndProcessor} object.
	 */
	protected PacketProcessor getEndProcessor(){return new EndProcessor();}	
	
	/**
	 * Processes ball packets.
	 * @author Vlad Firoiu
	 */
	protected class BallProcessor implements PacketProcessor {
		protected final BallPacket ballPacket = new BallPacket();
		
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			ballPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + ballPacket.toString());
			client.handleBall(ballPacket);
		}
	}

	/**
	 * Note that subclasses should override this method if a separate ball
	 * processor is to be used.
	 * @return A new {@code BallProcessor} object.
	 */
	protected PacketProcessor getBallProcessor(){return new BallProcessor();}
	
	/**
	 * Processes Ship packets.
	 * @author Vlad Firoiu
	 */
	protected class ShipProcessor implements PacketProcessor {
		protected final ShipPacket shipPacket = new ShipPacket();
		
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			shipPacket.readPacket(in);
			if(PRINT_PACKETS)System.out.println('\n' + shipPacket.toString());
			client.handleShip(shipPacket);
		}
	}

	/**
	 * Note that subclasses should override this method if a separate ship
	 * processor is to be used.
	 * @return A new {@code ShipProcessor} object.
	 */
	protected PacketProcessor getShipProcessor(){return new ShipProcessor();}
	
	/**
	 * Processes Fast Shot packets.
	 * @author Vlad Firoiu
	 */
	protected class FastShotProcessor implements PacketProcessor {
		protected final FastShotPacket fastShotPacket = new FastShotPacket();
		protected final AbstractDebrisHolder fastShotHolder = new AbstractDebrisHolder();
		
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			fastShotPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + fastShotPacket.toString());
			for(int i = 0;i<fastShotPacket.getNum();i++) {
				client.handleFastShot(fastShotHolder.setAbstractDebris(fastShotPacket.getType(), in.getUnsignedByte(), in.getUnsignedByte()));
			}
		}
	}

	/**
	 * Note that subclasses should override this method if a separate fast shot
	 * processor is to be used.
	 * @return A new {@code FastShotProcessor} object.
	 */
	protected PacketProcessor getFastShotProcessor(){return new FastShotProcessor();}
	
	/**
	 * Processes Item packets.
	 * @author Vlad Firoiu
	 */
	protected class ItemProcessor implements PacketProcessor {
		protected final ItemPacket itemPacket = new ItemPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			itemPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + itemPacket.toString());
			//TODO: Implement handling of item packets.
		}
	}

	/**
	 * Note that subclasses should override this method if a separate item
	 * processor is to be used.
	 * @return A new {@code ItemProcessor} object.
	 */
	protected PacketProcessor getItemProcessor(){return new ItemProcessor();}
	
	/**
	 * Processes Fast Radar packets.
	 * @author Vlad Firoiu
	 */
	protected class FastRadarProcessor implements PacketProcessor {
		protected final FastRadarPacket fastRadarPacket = new FastRadarPacket();
		
		public void processPacket(ByteBuffer in) throws PacketReadException {
			fastRadarPacket.clear();
			fastRadarPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + fastRadarPacket.toString());
			HolderList<RadarHolder> radarHolders = fastRadarPacket.getRadarHolders();
			for(int i = 0;i<radarHolders.size();i++) {
				client.handleRadar(radarHolders.get(i));
			}
		}
	}

	/**
	 * Note that subclasses should override this method if a separate fast radar
	 * processor is to be used.
	 * @return A new {@code FastRadarProcessor} object.
	 */
	protected PacketProcessor getFastRadarProcessor(){return new FastRadarProcessor();}
	
	/**
	 * Processes data from a Paused packet.
	 * @author Vlad Firoiu
	 */
	protected class PausedProcessor implements PacketProcessor {
		protected final PausedPacket pausedPacket = new PausedPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			pausedPacket.readPacket(in);
			if(PRINT_PACKETS)System.out.println('\n' + pausedPacket.toString());
			//TODO: Implement handling of Paused packets.
		}
	}

	/**
	 * Note that subclasses should override this method if a separate paused
	 * processor is to be used.
	 * @return A new {@code PausedProcessor} object.
	 */
	protected PacketProcessor getPausedProcessor(){return new WreckageProcessor();}
	
	/**
	 * Processes Wreckage packets.
	 * @author Vlad Firoiu
	 */
	protected class WreckageProcessor implements PacketProcessor {
		protected final WreckagePacket wreckagePacket = new WreckagePacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			wreckagePacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + wreckagePacket.toString());
			//TODO: Implement handling of Wreckage packets.
		}
	}

	/**
	 * Note that subclasses should override this method if a separate wreckage
	 * processor is to be used.
	 * @return A new {@code WreckageProcessor} object.
	 */
	protected PacketProcessor getWreckageProcessor(){return new WreckageProcessor();}
	
	/**
	 * Processes War packets.
	 * @author Vlad Firoiu
	 */
	protected class WarProcessor implements PacketProcessor {
		protected final WarPacket warPacket = new WarPacket();
		@Override
		public void processPacket(ByteBuffer in) throws ReliableReadException {
			warPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + warPacket.toString());
			//TODO: Implement handling of War packets.
		}
	};

	/**
	 * Note that subclasses should override this method if a separate war
	 * processor is to be used.
	 * @return A new {@code WarProcessor} object.
	 */
	protected PacketProcessor getWarProcessor(){return new WarProcessor();}
	
	/**
	 * Processes Connector packets.
	 * @author Vlad Firoiu
	 */
	protected class ConnectorProcessor implements PacketProcessor {
		protected final ConnectorPacket connectorPacket = new ConnectorPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			connectorPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + connectorPacket.toString());
			client.handleConnector(connectorPacket);
		}
	}

	/**
	 * Note that subclasses should override this method if a separate connector
	 * processor is to be used.
	 * @return A new {@code ConnectorProcessor} object.
	 */
	protected PacketProcessor getConnectorProcessor(){return new ConnectorProcessor();}
	
	/**
	 * Processes Leave packets.
	 * @author Vlad Firoiu
	 */
	protected class LeaveProcessor implements PacketProcessor {
		protected final LeavePacket leavePacket = new LeavePacket();
		@Override
		public void processPacket(ByteBuffer in) throws ReliableReadException {
			leavePacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + leavePacket.toString());
			client.handleLeave(leavePacket.getId());
		}
	}

	/**
	 * Note that subclasses should override this method if a separate leave
	 * processor is to be used.
	 * @return A new {@code LeaveProcessor} object.
	 */
	protected PacketProcessor getLeaveProcessor(){return new LeaveProcessor();}
	
	/**
	 * Processes data from a Score Object packet.
	 * @author Vlad Firoiu
	 */
	protected class ScoreObjectProcessor implements PacketProcessor {
		protected final ScoreObjectPacket scoreObjectPacket = new ScoreObjectPacket();
		@Override
		public void processPacket(ByteBuffer in) throws ReliableReadException {
			scoreObjectPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + scoreObjectPacket.toString());
			client.handleScoreObject(scoreObjectPacket);
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate score object
	 * processor is to be used.
	 * @return A new {@code ScoreObjectProcessor} object.
	 */
	protected PacketProcessor getScoreObjectProcessor(){return new ScoreObjectProcessor();}
	
	/**
	 * Processes Mine packets.
	 * @author Vlad Firoiu
	 */
	protected class MineProcessor implements PacketProcessor {
		protected final MinePacket minePacket = new MinePacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			minePacket.readPacket(in);
			if(PRINT_PACKETS)System.out.println('\n' + minePacket.toString());
			client.handleMine(minePacket);
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate mine
	 * processor is to be used.
	 * @return A new {@code MineProcessor} object.
	 */
	protected PacketProcessor getMineProcessor(){return new MineProcessor();}
	
	/**
	 * Processes Cannon packets.
	 * @author Vlad Firoiu
	 */
	protected class CannonProcessor implements PacketProcessor {
		protected final CannonPacket cannonPacket = new CannonPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			cannonPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + cannonPacket.toString());
			
			out.put(PKT_ACK_CANNON);
			out.putInt(last_loops);
			out.putShort((short)cannonPacket.getNum());
			
			client.handleCannon(cannonPacket);
		}
	}

	/**
	 * Note that subclasses should override this method if a separate cannon
	 * processor is to be used.
	 * @return A new {@code CannonProcessor} object.
	 */
	protected PacketProcessor getCannonProcessor(){return new CannonProcessor();}
	
	/**
	 * Processes Fuel packets.
	 * @author Vlad Firoiu
	 */
	protected class FuelProcessor implements PacketProcessor {
		protected final FuelPacket fuelPacket = new FuelPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			fuelPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + fuelPacket.toString());
			
			out.put(PKT_ACK_FUEL);
			out.putInt(last_loops);
			out.putShort((short)fuelPacket.getNum());
			
			client.handleFuel(fuelPacket);
		}
	}

	/**
	 * Note that subclasses should override this method if a separate fuel
	 * processor is to be used.
	 * @return A new {@code FuelProcessor} object.
	 */
	protected PacketProcessor getFuelProcessor(){return new FuelProcessor();}
	
	/**
	 * Processes Missile packets.
	 * @author Vlad Firoiu
	 */
	protected class MissileProcessor implements PacketProcessor {
		protected final MissilePacket missilePacket = new MissilePacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			missilePacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + missilePacket.toString());
			client.handleMissile(missilePacket);
		}
	}

	/**
	 * Note that subclasses should override this method if a separate missile
	 * processor is to be used.
	 * @return A new {@code MissileProcessor} object.
	 */
	protected PacketProcessor getMissileProcessor(){return new MissileProcessor();}
	
	/**
	 * Processes Damaged packets.
	 * @author Vlad Firoiu
	 */
	protected class DamagedProcessor implements PacketProcessor {
		protected final DamagedPacket damagedPacket = new DamagedPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			damagedPacket.readPacket(in);
			if (PRINT_PACKETS) System.out.println('\n' + damagedPacket.toString());
			//TODO: Implement handling of Damaged packets.
		}
	}

	/**
	 * Note that subclasses should override this method if a separate damaged
	 * processor is to be used.
	 * @return A new {@code DamagedProcessor} object.
	 */
	protected PacketProcessor getDamagedProcessor(){return new DamagedProcessor();}
	
	/**
	 * Processes Laser packets.
	 * @author Vlad Firoiu
	 */
	protected class LaserProcessor implements PacketProcessor {
		protected final LaserPacket laserPacket = new LaserPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			laserPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + laserPacket.toString());
			client.handleLaser(laserPacket);
		}
	}

	/**
	 * Note that subclasses should override this method if a separate laser
	 * processor is to be used.
	 * @return A new {@code LaserProcessor} object.
	 */
	protected PacketProcessor getLaserProcessor(){return new LaserProcessor();}
	
	/**
	 * Processes ECM packets.
	 * @author Vlad Firoiu
	 */
	protected class ECMProcessor implements PacketProcessor {
		protected final ECMPacket ecmPacket = new ECMPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			ecmPacket.readPacket(in);
			if (PRINT_PACKETS) System.out.println('\n' + ecmPacket.toString());
			client.handleECM(ecmPacket);
		}
	}

	/**
	 * Note that subclasses should override this method if a separate ECM
	 * processor is to be used.
	 * @return A new {@code ECMProcessor} object.
	 */
	protected PacketProcessor getECMProcessor(){return new ECMProcessor();}
	
	/**
	 * Processes Refuel packets.
	 * @author Vlad Firoiu
	 */
	protected class RefuelProcessor implements PacketProcessor {
		protected final RefuelPacket refuelPacket = new RefuelPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			refuelPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + refuelPacket.toString());
			client.handleRefuel(refuelPacket);
		}
	}

	/**
	 * Note that subclasses should override this method if a separate refuel
	 * processor is to be used.
	 * @return A new {@code RefuelProcessor} object.
	 */
	protected PacketProcessor getRefuelProcessor(){return new RefuelProcessor();}
	
	/**
	 * Processes Talk Ack packets.
	 * @author Vlad Firoiu
	 */
	protected class TalkAckProcessor implements PacketProcessor {
		protected final TalkAckPacket talkAckPacket = new TalkAckPacket();
		@Override
		public void processPacket(ByteBuffer in) throws ReliableReadException {
			talkAckPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + talkAckPacket.toString());
			if (talkAckPacket.getTalkAck() >= talk_pending) {
				talk_pending = 0;
			}
		}
	}

	/**
	 * Note that subclasses should override this method if a separate talk ack
	 * processor is to be used.
	 * @return A new {@code TalkAckProcessor} object.
	 */
	protected PacketProcessor getTalkAckProcessor(){return new TalkAckProcessor();}
	
	/**
	 * Processes Timing packets.
	 * @author Vlad Firoiu
	 */
	protected class TimingProcessor implements PacketProcessor {
		protected final TimingPacket timingPacket = new TimingPacket();
		@Override
		public void processPacket(ByteBuffer in) throws ReliableReadException {
			timingPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + timingPacket.toString());
			//TODO Implement handling of Timing packet.
		}
	}

	/**
	 * Note that subclasses should override this method if a separate timing
	 * processor is to be used.
	 * @return A new {@code TimingProcessor} object.
	 */
	protected PacketProcessor getTimingProcessor(){return new TimingProcessor();}
	
	/**
	 * Processes target packets.
	 * @author Vlad Firoiu
	 */
	protected class TargetProcessor implements PacketProcessor {
		protected TargetPacket targetPacket = new TargetPacket();
		
		public void processPacket(ByteBuffer in) throws PacketReadException {
			targetPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + targetPacket.toString());
			//TODO Implement handling of Target packet.
			//acks target packet
			out.put(PKT_ACK_TARGET).putInt(last_loops).putShort(targetPacket.getNum());
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate target
	 * processor is to be used.
	 * @return A new TargetProcessor object.
	 */
	protected PacketProcessor getTargetProcessor(){return new TargetProcessor();}
	
	/**
	 * Processes eyes packets.
	 * @author Vlad Firoiu
	 */
	protected class EyesProcessor implements PacketProcessor {
		protected EyesPacket eyesPacket = new EyesPacket();
		
		public void processPacket(ByteBuffer in) throws PacketReadException {
			eyesPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + eyesPacket.toString());
			client.handleEyes(eyesPacket.getId());
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate eyes
	 * processor is to be used.
	 * @return A new EyesProcessor object.
	 */
	protected PacketProcessor getEyesProcessor(){return new EyesProcessor();}
	
	/**
	 * Processes Team Score packets.
	 * @author Vlad Firoiu
	 */
	protected class TeamScoreProcessor implements PacketProcessor {
		protected final TeamScorePacket teamScorePacket = new TeamScorePacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			teamScorePacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + teamScorePacket.toString());
			//TODO Implement handling of Team score.
		}
	}
	/**
	 * Note that subclasses should override this method if a separate team
	 * score processor is to be used.
	 * @return A new TeamScoreProcessor object.
	 */
	protected PacketProcessor getTeamScoreProcessor(){return new TeamScoreProcessor();}
	
	/**
	 * Processes Seek packets.
	 * @author Vlad Firoiu
	 */
	protected class SeekProcessor implements PacketProcessor {
		protected final SeekPacket seekPacket = new SeekPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			seekPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + seekPacket.toString());
			client.handleSeek(seekPacket);
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate seek
	 * processor is to be used.
	 * @return A new {@code SeekProcessor} object.
	 */
	protected PacketProcessor getSeekProcessor(){return new SeekProcessor();}
	
	/**
	 * Processes String packets. Note that this has no function,
	 * as it is an experimental packet type.
	 * @author Vlad Firoiu
	 */
	protected class StringProcessor implements PacketProcessor {
		protected byte pkt_type, type;
		protected int arg1, arg2;
		
		public byte getPacketType(){return pkt_type;}
		public byte getType(){return type;}
		public int getArg1(){return arg1;}
		public int getArg2(){return arg2;}
		
		protected void readPacket(ByteBuffer in) {
			pkt_type = in.get();
			type = in.get();
			arg1 = Short.toUnsignedInt(in.getShort());
			arg2 = Short.toUnsignedInt(in.getShort());
		}
		
		@Override
		public void processPacket(ByteBuffer in) {
			readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + this.toString());
		}
		
		@Override
		public String toString() {
			return "String Packet\npacket type = " + pkt_type +
					"\ntype = " + type +
					"\narg1 = " + arg1 + 
					"\narg2 = " + arg2;
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate string
	 * processor is to be used.
	 * @return A new StringProcessor object.
	 */
	protected PacketProcessor getStringProcessor(){return new StringProcessor();}
	
	/**
	 * Processes wormhole packets.
	 * @author Vlad Firoiu
	 * @since xpilot version 4.5.0
	 */
	protected class WormholeProcessor implements PacketProcessor {
		protected final WormholePacket wormholePacket = new WormholePacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			wormholePacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + wormholePacket.toString());
			//TODO Implement handling of Wormhole packet.
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate wormhole
	 * processor is to be used.
	 * @return A new {@code WormholeProcessor} object.
	 * @since xpilot version 4.5.0
	 */
	protected PacketProcessor getWormholeProcessor(){return new WormholeProcessor();}
	
	/**
	 * Processes asteroid packets.
	 * @author Vlad Firoiu
	 * @since xpilot version 4.4.0
	 */
	protected class AsteroidProcessor implements PacketProcessor {
		protected final AsteroidPacket asteroidPacket = new AsteroidPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			asteroidPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + this.toString());
			client.handleAsteroid(asteroidPacket);
		}
	}
	/**
	 * Note that subclasses should override this method if a separate asteroid
	 * processor is to be used.
	 * @return A new AsteroidProcessor object.
	 * @since xpilot version 4.4.0
	 */
	protected PacketProcessor getAsteroidProcessor(){return new AsteroidProcessor();}
	
	/**
	 * Processes lose item packets.
	 * @author Vlad Firoiu
	 */
	protected class LoseItemProcessor implements PacketProcessor {
		protected final LoseItemPacket loseItemPacket = new LoseItemPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			loseItemPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + loseItemPacket.toString());
			//TODO Implement handling of Lose Item packet.
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate lose
	 * item processor is to be used.
	 * @return A new LoseItemProcessor object.
	 */
	protected PacketProcessor getLoseItemProcessor(){return new LoseItemProcessor();}
	
	/**
	 * Processes round delay packets.
	 * @author Vlad Firoiu
	 */
	protected class RoundDelayProcessor implements PacketProcessor {
		protected final RoundDelayPacket roundDelayPacket = new RoundDelayPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			roundDelayPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + roundDelayPacket.toString());
			//TODO Implement handling of round delay.
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate round
	 * delay processor is to be used.
	 * @return A new RoundDelayProcessor object.
	 */
	protected PacketProcessor getRoundDelayProcessor(){return new RoundDelayProcessor();}
	
	/**
	 * Processes Phasing Time packets.
	 * @author Vlad Firoiu
	 */
	protected class PhasingTimeProcessor implements PacketProcessor {
		protected final PhasingTimePacket phasingTimePacket = new PhasingTimePacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			phasingTimePacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + phasingTimePacket.toString());
			//TODO Implement handling of Phasing Time packet.
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate phasing
	 * time processor is to be used.
	 * @return A new PhasingTimeProcessor object.
	 */
	protected PacketProcessor getPhasingTimeProcessor(){return new PhasingTimeProcessor();}
	
	/**
	 * Processes Thrust Time packets.
	 * @author Vlad Firoiu
	 */
	protected class ThrustTimeProcessor implements PacketProcessor {
		protected final ThrustTimePacket thrustTimePacket = new ThrustTimePacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			thrustTimePacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + thrustTimePacket.toString());
			//TODO Implement handling of Thrust Time packet.
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate thrust
	 * time processor is to be used.
	 * @return A new ThrustTimeProcessor object.
	 */
	protected PacketProcessor getThrustTimeProcessor(){return new ThrustTimeProcessor();}
	
	/**
	 * Processes Shield Time packets.
	 * @author Vlad Firoiu
	 */
	protected class ShieldTimeProcessor implements PacketProcessor {
		protected final ShieldTimePacket shieldTimePacket = new ShieldTimePacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			shieldTimePacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + shieldTimePacket.toString());
			//TODO Implement handling of Shield Time packet.
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate shield
	 * time processor is to be used.
	 * @return A new ShieldTimeProcessor object.
	 */
	protected PacketProcessor getShieldTimeProcessor(){return new ShieldTimeProcessor();}
	
	/**
	 * Processes MOTD (Message of the Day) packets.
	 * @author Vlad Firoiu
	 */
	protected class MOTDProcessor implements PacketProcessor {
		protected final MOTDPacket motdPacket = new MOTDPacket();
		@Override
		public void processPacket(ByteBuffer in) throws ReliableReadException {
			motdPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + motdPacket.toString());
			//TODO Implement handling of MOTD packet.
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate motd
	 * processor is to be used.
	 * @return A new {@code MOTDProcessor} object.
	 */
	protected PacketProcessor getMOTDProcessor(){return new MOTDProcessor();}
	
	/**
	 * Processes radar packets.
	 * @author Vlad Firoiu
	 */
	protected class RadarProcessor implements PacketProcessor {
		protected final RadarPacket radarPacket = new RadarPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			radarPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + radarPacket.toString());
			client.handleRadar(radarPacket);
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate radar
	 * processor is to be used.
	 * @return A new {@code RadarProcessor} object.
	 */
	protected PacketProcessor getRadarProcessor(){return new RadarProcessor();}

	/**
	 * Processes destruct packets.
	 * @author Vlad Firoiu
	 */
	protected class DestructProcessor implements PacketProcessor {
		protected final DestructPacket destructPacket = new DestructPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			destructPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + destructPacket.toString());
			//TODO Implement handling of shutdown.
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate destruct
	 * processor is to be used.
	 * @return A new {@code DestructProcessor} object.
	 */	
	protected PacketProcessor getDestructProcessor(){return new DestructProcessor();}
	
	/**
	 * Processes shutdown packets.
	 * @author Vlad Firoiu
	 */
	protected class ShutdownProcessor implements PacketProcessor {
		protected final ShutdownPacket shutdownPacket = new ShutdownPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			shutdownPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + shutdownPacket.toString());
			client.handleShutdown(shutdownPacket);
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate shutdown
	 * processor is to be used.
	 * @return A new {@code ShutdownProcessor} object.
	 */	
	protected PacketProcessor getShutdownProcessor(){return new ShutdownProcessor();}
	
	/**
	 * Processes trans packets.
	 * @author Vlad Firoiu
	 */
	protected class TransProcessor implements PacketProcessor {
		protected final TransPacket transPacket = new TransPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			transPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + transPacket.toString());
			//TODO Implement handling of trans.
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate trans
	 * processor is to be used.
	 * @return A new TransProcessor object.
	 */	
	protected PacketProcessor getTransProcessor(){return new TransProcessor();}
	
	/**
	 * Processes audio packets.
	 * 
	 * @author Vlad Firoiu
	 */
	protected class AudioProcessor implements PacketProcessor {
		protected final AudioPacket audioPacket = new AudioPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			audioPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + audioPacket.toString());
			//TODO Implement handling of audio.
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate audio
	 * processor is to be used.
	 * @return A new {@code AudioProcessor} object.
	 */	
	protected PacketProcessor getAudioProcessor(){return new AudioProcessor();}
	
	/**
	 * Processes time left packets.
	 * @author Vlad Firoiu
	 */
	protected class TimeLeftProcessor implements PacketProcessor {
		protected final TimeLeftPacket timeLeftPacket = new TimeLeftPacket();
		@Override
		public void processPacket(ByteBuffer in) throws PacketReadException {
			timeLeftPacket.readPacket(in);
			if(PRINT_PACKETS) System.out.println('\n' + timeLeftPacket.toString());
			//TODO Implement handling of time left.
		}
	}
	
	/**
	 * Note that subclasses should override this method if a separate time
	 * left processor is to be used.
	 * @return A new {@code TimeLeftProcessor} object.
	 */	
	protected PacketProcessor getTimeLeftProcessor(){return new TimeLeftProcessor();}
}
