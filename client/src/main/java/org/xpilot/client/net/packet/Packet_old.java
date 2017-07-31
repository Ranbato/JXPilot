package org.xpilot.client.net.packet;

/**
 * Holds various constants used in network communications.
 * Corresponds to packet.h and pack.h in xpilot/src/common.
 */
public class Packet_old {
	//pack.h info
	/*
	 * Different contact pack types.
	 */
	public static final byte
	ENTER_GAME_pack =		0x00,
	ENTER_QUEUE_pack =		0x01,
	REPLY_pack =			0x10,
	REPORT_STATUS_pack =	0x21,
	OPTION_LIST_pack =		0x28,
	//CORE_pack	=			0x30,
	CONTACT_pack =			0x31,
	/* The owner-only commands have a common bit high. */
	PRIVILEGE_PACK_MASK =	0x40,
	LOCK_GAME_pack =		0x62,
	MESSAGE_pack =			0x63,
	SHUTDOWN_pack =			0x64,
	KICK_PLAYER_pack =		0x65,
	//MAX_ROBOT_pack =		0x66,
	OPTION_TUNE_pack =		0x67,
	CREDENTIALS_pack =		0x69,

	/*
	 * Possible error codes returned.
	 */
	/** Operation successful */
	SUCCESS	=			0x00,
	/** Permission denied, not owner */
	E_NOT_OWNER =		0x01,
	/** Game is full, play denied */
	E_GAME_FULL	= 		0x02,
	/** Team is full, play denied */
	E_TEAM_FULL	=		0x03,
	/** Need to specify a team */
	E_TEAM_NOT_SET =	0x04,
	/** Game is locked, entry denied */
	E_GAME_LOCKED =		0x05,
	/** Player was not found */
	E_NOT_FOUND	=		0x07,
	/** Name is already in use */
	E_IN_USE =			0x08,
	/** Can't setup socket */
	E_SOCKET =			0x09,
	/** Invalid input parameters */
	E_INVAL	=			0x0A,
	/** Incompatible version */
	E_VERSION =			0x0C,
	/** No such variable */
	E_NOENT	=			0x0D,
	/** Operation undefined */
	E_UNDEFINED =		0x0E;
	
	//packet.h info
	
	/* before version 3.8.0 this was 8 bytes. */
	public static final byte
	KEYBOARD_SIZE	=9,

	/*
	 * Definition of various client/server packet types.
	 */

	/* packet types: 0 - 9 */
	PKT_UNDEFINED	=	0,
	PKT_VERIFY		=	1,
	PKT_REPLY		=	2,
	PKT_PLAY		=	3,
	PKT_QUIT		=	4,
	PKT_MESSAGE	=	5,
	PKT_START		=	6,
	PKT_END		=	7,
	PKT_SELF		=	8,
	PKT_DAMAGED	=	9,

	/* packet types: 10 - 19 */
	PKT_CONNECTOR	=	10,
	PKT_REFUEL		=	11,
	PKT_SHIP		=	12,
	PKT_ECM		=	13,
	PKT_PAUSED		=	14,
	PKT_ITEM		=	15,
	PKT_MINE		=	16,
	PKT_BALL		=	17,
	PKT_MISSILE	=	18,
	PKT_SHUTDOWN	=	19,

	/* packet types: 20 - 29 */
	PKT_STRING		=	20,
	PKT_DESTRUCT	=	21,
	PKT_RADAR		=	22,
	PKT_TARGET		=	23,
	PKT_KEYBOARD	=	24,
	PKT_SEEK		=	25,
	PKT_SELF_ITEMS	=	26,	/* still under development */
	PKT_TEAM_SCORE	=	27,	/* was PKT_SEND_BUFSIZE */
	PKT_PLAYER		=	28,
	PKT_SCORE		=	29,

	/* packet types: 30 - 39 */
	PKT_FUEL		=	30,
	PKT_BASE		=	31,
	PKT_CANNON		=	32,
	PKT_LEAVE		=	33,
	PKT_POWER		=	34,
	PKT_POWER_S	=	35,
	PKT_TURNSPEED	=	36,
	PKT_TURNSPEED_S=	37,
	PKT_TURNRESISTANCE=38,
	PKT_TURNRESISTANCE_S=	39,

	/* packet types: 40 - 49 */
	PKT_WAR		=	40,
	PKT_MAGIC	=		41,
	PKT_RELIABLE	=	42,
	PKT_ACK		=	43,
	PKT_FASTRADAR	=	44,
	PKT_TRANS		=	45,
	PKT_ACK_CANNON	=	46,
	PKT_ACK_FUEL	=	47,
	PKT_ACK_TARGET	=	48,
	PKT_SCORE_OBJECT=	49,

	/* packet types: 50 - 59 */
	PKT_AUDIO		=	50,
	PKT_TALK		=	51,
	PKT_TALK_ACK	=	52,
	PKT_TIME_LEFT	=	53,
	PKT_LASER		=	54,
	PKT_DISPLAY	=	55,
	PKT_EYES		=	56,
	PKT_SHAPE		=	57,
	PKT_MOTD		=	58,
	PKT_LOSEITEM	=	59,

	/* packet types: 60 - 69 */
	PKT_NOT_USED_60	=	60,
	PKT_NOT_USED_61	=	61,
	PKT_NOT_USED_62	=	62,
	PKT_NOT_USED_63	=	63,
	PKT_NOT_USED_64	=	64,
	PKT_NOT_USED_65	=	65,
	PKT_NOT_USED_66	=	66,
	PKT_NOT_USED_67	=	67,
	PKT_MODIFIERS		=	68,
	PKT_FASTSHOT		=	69,	/* replaces SHOT/TEAMSHOT */

	/* packet types: 70 - 79 */
	PKT_THRUSTTIME		=	70,
	PKT_MODIFIERBANK	=	71,
	PKT_SHIELDTIME		=	72,
	PKT_POINTER_MOVE	=	73,
	PKT_REQUEST_AUDIO	=	74,
	PKT_ASYNC_FPS		=	75,
	PKT_TIMING			=	76,
	PKT_PHASINGTIME	=	77,
	PKT_ROUNDDELAY		=	78,
	PKT_WRECKAGE		=	79,

	/* packet types: 80 - 89 */
	PKT_ASTEROID		=	80,
	PKT_WORMHOLE		=	81,
	PKT_NOT_USED_82	=	82,
	PKT_NOT_USED_83	=	83,
	PKT_NOT_USED_84	=	84,
	PKT_NOT_USED_85	=	85,
	PKT_NOT_USED_86	=	86,
	PKT_NOT_USED_87	=	87,
	PKT_NOT_USED_88	=	88,
	PKT_NOT_USED_89	=	89,

	/* packet types: 90 - 99 */
	/*
	 * Use these 10 packet type numbers for
	 * experimenting with new packet types.
	 */

	/* status reports: 101 - 102 */
	PKT_FAILURE	=	101,
	PKT_SUCCESS	=	102,

	/* optimized packet types: 128 - 255 */
	PKT_DEBRIS		=	(byte)128;	/* + color + x + y */
	
	public static final int DEBRIS_TYPES	=	(8 * 4 * 4);//128
	
	public static final int NUM_PACKET_TYPES = 256;
}