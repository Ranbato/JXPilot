package net.sf.jxpilot.util;

/**
 * Holds a number of bits that is a multiple of 8.
 * @author Vlad Firoiu
 */
public class BitVector implements java.io.Serializable {
	/**
	 * Used for object serialization.
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * The number of bits in a byte, 8.
	 */
	public static final int BITS_PER_BYTE = 8;
	
	/**
	 * The various bit masks, for each possible bit position
	 * within a byte.
	 */
	private static final byte[] masks;
	
	//initializes the bit masks.
	static {
		masks = new byte[BITS_PER_BYTE];
		
		for (int i =0;i<BITS_PER_BYTE;i++) {
			masks[i] = (byte) (1 << i);
		}
	}

	/**
	 * @param bitIndex The index of the desired bit.
	 * @return The index of the bit within its containing byte.
	 */
	private static int bitIndexInByte(int bitIndex) {
		return bitIndex % BITS_PER_BYTE;
	}
	
	/**
	 * @param bitIndex The index of the desired bit.
	 * @return The appropriate bit mask.
	 */
	private static byte getMask(int bitIndex) {
		return masks[bitIndex % BITS_PER_BYTE];
	}
	
	/**
	 * The number of bits being held in this BitVector.
	 */
	public final int numBits;
	/**
	 * The number of bytes used to store the bits in this BitVector.
	 */
	public final int numBytes;
	/**
	 * The bytes in the bit vector.
	 */
	private final byte[] bytes;
	
	/**
	 * Creates a new BitVector.
	 * @param numBits The number of bits to hold.
	 */
	public BitVector(int numBits) {
		this.numBits = numBits;
		numBytes = 1+(numBits-1)/BITS_PER_BYTE;
		bytes = new byte[numBytes];
	}
	
	/**
	 * @return The bytes used to hold this BitVector.
	 * Note that this method returns a reference to the underlying array
	 * to save on memory and time.
	 */
	public byte[] getBytes(){return bytes;}
	
	/**
	 * @param bitIndex The index of the desired bit.
	 * @return The index of the byte that contains the bit.
	 */
	private int byteIndex(int bitIndex) {
		return bitIndex / BITS_PER_BYTE;
	}
	
	/**
	 * @param bitIndex The index of the desired bit.
	 * @return The byte that contains the bit.
	 */
	private byte getByte(int bitIndex) {
		return bytes[bitIndex/BITS_PER_BYTE];
	}
	
	public boolean isSet(int bitIndex) {
		return (getByte(bitIndex) & getMask(bitIndex))!=0;
	}
	
	/**
	 * Sets the desired bit on or off.
	 * @param bitIndex The index of the desired bit.
	 * @param value Whether the bit will be on or off.
	 * @return This BitVector.
	 */
	public BitVector setBit(int bitIndex, boolean value) {
		if (value) {
			bytes[byteIndex(bitIndex)] |= getMask(bitIndex);
		} else {
			bytes[byteIndex(bitIndex)] &= ~getMask(bitIndex);
		}
		return this;
	}
	
	/**
	 * Switches the value of the desired bit.
	 * @param bitIndex The index of the desired bit.
	 * @return This BitVector.
	 */
	public BitVector switchBit(int bitIndex) {
		bytes[byteIndex(bitIndex)] ^= getMask(bitIndex);
		return this;
	}
	
	/**
	 * Turns on all of the bits in this BitVector.
	 * @return This BitVector.
	 */
	public BitVector clearBits()
	{
		for (int i =0;i<bytes.length;i++) {
			bytes[i] = 0;
		}
		return this;
	}
	
	/**
	 * Turns on all of the bits in this BitVector.
	 * @return This BitVector.
	 */
	public BitVector setBits() {
		for (int i =0;i<bytes.length;i++) {
			bytes[i] = ~0;
		}
		return this;
	}
	
	/**
	 * @return The bytes of this BitVector, in hex format.
	 */
	@Override
	public String toString() {
		StringBuilder temp = new StringBuilder();
		
		for (int i=0;i<numBytes;i++) {
			temp.append(String.format("%x", bytes[i]));
		}
		
		return temp.toString();
	}
}
