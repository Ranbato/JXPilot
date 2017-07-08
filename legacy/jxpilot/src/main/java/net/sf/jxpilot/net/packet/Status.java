package net.sf.jxpilot.net.packet;

/**
 * Describes the possible error codes of a ReplyMessage.
 * @author Vlad Firoiu
 */
public enum Status {
	/*
	 * Possible error codes returned.
	 */
	/** Operation successful */
	SUCCESS((byte)0x00),
	/** Permission denied, not owner */
	E_NOT_OWNER((byte)0x01),
	/** Game is full, play denied */
	E_GAME_FULL((byte)0x02),
	/** Team is full, play denied */
	E_TEAM_FULL((byte)0x03),
	/** Need to specify a team */
	E_TEAM_NOT_SET((byte)0x04),
	/** Game is locked, entry denied */
	E_GAME_LOCKED((byte)0x05),
	/** Player was not found */
	E_NOT_FOUND((byte)0x07),
	/** Name is already in use */
	E_IN_USE((byte)0x08),
	/** Can't setup socket */
	E_SOCKET((byte)0x09),
	/** Invalid input parameters */
	E_INVAL((byte)0x0A),
	/** Incompatible version */
	E_VERSION ((byte)0x0C),
	/** No such variable */
	E_NOENT((byte)0x0D),
	/** Operation undefined */
	E_UNDEFINED((byte)0x0E);
	
	/**
	 * The byte representation of this status.
	 */
	public final byte STATUS;
	private Status(byte status){STATUS = status;}
	
	private static final Status[] STATUSES = values();
	/**
	 * @param status The byte representation of the status.
	 * @return The corresponding {@code Status} object.
	 */
	public static Status getStatus(byte status){return STATUSES[status];}
}
