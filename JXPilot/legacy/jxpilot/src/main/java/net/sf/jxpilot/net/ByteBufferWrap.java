package net.sf.jxpilot.net;

import java.io.*;
import java.nio.*;
import java.nio.channels.*;
import java.net.*;

import net.sf.jxpilot.util.Utilities;

/**
 * Wrapper class that handles buffer flips automatically. It does not actually extend ByteBuffer since
 * ByteBuffer has only private methods/constructors, UNFORTUNATELY.
 * @author Vlad Firoiu
 */

public class ByteBufferWrap {
	/**
	 * Marks the end of a C-style string.
	 */
	public static final byte END_OF_STRING = 0;
	/**
	 * The key for encoding bytes in ASCII.
	 */
	public static final String ASCII_ENCODING = "US-ASCII";
	
	private boolean writing=true;
	private ByteBuffer buffer;
	private int mark=0;
	
	public ByteBufferWrap(int capacity) {
		buffer = ByteBuffer.allocate(capacity);
	}
	
	public boolean isWriting(){return writing;}
	public boolean isReading(){return !writing;}
	
	/**
	 * Sets the buffer to start writing.
	 */
	public void setWriting() {
		if (!writing) {
			mark = buffer.position();
			//buffer.mark();
			buffer.position(buffer.limit());
			buffer.limit(buffer.capacity());
			writing = true;
		}
	}
	
	/**
	 * Sets the buffer to start reading.
	 */
	public void setReading() {
		if (writing) {
			buffer.limit(buffer.position());
			buffer.position(mark);
			writing = false;
		}
	}
	
	public void clear() {
		writing = true;
		buffer.clear();
		mark = 0;
	}
	
	/**
	 * This method does not automatically clear the underlying ByteBuffer before reading a UDP packet.
	 * @param channel The UDP channel to read from.
	 * @return The number of bytes read.
	 * @throws IOException if {@link DatagramChannel#read(ByteBuffer)} throws an IOException.
	 * @return The number of bytes read (possibly 0) or -1 if no packets available
	 * 			and the channel is in non-blocking mode.
	 * @see {@link DatagramChannel#read(ByteBuffer)}
	 */
	public int readPacket(DatagramChannel channel) throws IOException {
		setWriting();
		return channel.read(buffer);
	}
	
	/**
	 * Writes from this buffer into the channel.
	 * @param channel The desired channel.
	 * @return The number of bytes written.
	 * @throws IOException Passes along any exceptions thrown by {@code channel.write(ByteBuffer)}.
	 * @see {@link DatagramChannel#write(ByteBuffer)}
	 */
	public int writePacket(DatagramChannel channel) throws IOException {
		setReading();
		return channel.write(buffer);
	}
	
	/**
	 * @see {@link DatagramChannel#receive(ByteBuffer)}
	 */
	public SocketAddress receivePacket(DatagramChannel channel) throws IOException {
		setWriting();
		return channel.receive(buffer);
	}
	
	/**
	 * Sends all remaining bytes in this buffer.
	 * @param channel The channel to send through.
	 * @param address The address to send to.
	 * @return The number of bytes sent.
	 * @throws IOException Passes along any IOExceptions thrown by {@code DatagramChannel.send(ByteBuffer, SocketAddress)}
	 * @see {@link DatagramChannel#send(ByteBuffer)}
	 */
	public int sendPacket(DatagramChannel channel, SocketAddress address) throws IOException {
		setReading();
		return channel.send(buffer, address);
	}
	
	public int remaining() {
		if (writing) return buffer.position()-mark;
		else return buffer.remaining();
	}
	
	/**
	 * @return The position of the buffer.
	 */
	public int position() {
		return buffer.position();
	}
	
	/**
	 * Sets the buffer's position.
	 * @param pos The desired position.
	 */
	public void position(int pos) {
		buffer.position(pos);
	}
	
	/**
	 * Compacts the buffer, and sets writing mode.
	 */
	public void compact() {
		buffer.compact();
		mark = 0;
		writing = true;
	}
	
	/**
	 * @return The backing array of this buffer.
	 */
	public byte[] getArray() {
		return buffer.array();
	}
	
	/**
	 * Puts a byte into this buffer.
	 * @param b The desired byte.
	 * @return This {@code ByteBufferWrap}, for convenience.
	 */
	public ByteBufferWrap putByte(byte b) {
		setWriting();
		buffer.put(b);
		return this;
	}
	
	/**
	 * Puts an array of bytes into this buffer.
	 * @param bytes The desired bytes.
	 * @return This {@code ByteBufferWrap}, for convenience.
	 */
	public ByteBufferWrap putBytes(byte[] bytes) {
		setWriting();
		buffer.put(bytes);
		return this;
	}
	
	/**
	 * Puts another buffer's bytes into this buffer.
	 * @param bytes The buffer containing the desired bytes.
	 * @return This {@code ByteBufferWrap}, for convenience.
	 */
	public ByteBufferWrap putBytes(ByteBufferWrap bytes) {
		setWriting();
		bytes.setReading();
		buffer.put(bytes.buffer);
		return this;
	}
	
	/**
	 * Puts a character into this buffer.
	 * @param c The desired {@code char}.
	 * @return This {@code ByteBufferWrap}, for convenience.
	 */
	public ByteBufferWrap putChar(char c) {
		setWriting();
		buffer.putChar(c);
		return this;
	}
	
	/**
	 * Puts a short into this buffer.
	 * @param s The desired{@code short}.
	 * @return This {@code ByteBufferWrap}, for convenience.
	 */
	public ByteBufferWrap putShort(short s) {
		setWriting();
		buffer.putShort(s);
		return this;
	}	

	/**
	 * Puts an integer into this buffer.
	 * @param i The desired {@code int}.
	 * @return This {@code ByteBufferWrap}, for convenience.
	 */
	public ByteBufferWrap putInt(int i) {
		setWriting();
		buffer.putInt(i);
		return this;
	}
	
	/**
	 * Puts a long into this buffer.
	 * @param l The desired {@code long}.
	 * @return This {@code ByteBufferWrap}, for convenience.
	 */
	public ByteBufferWrap putLong(long l) {
		setWriting();
		buffer.putLong(l);
		return this;
	}
	
	/**
	 * Puts a String into this buffer.
	 * @param str The String to be put into the buffer using ASCII encoding.
	 * 				A null char is placed at the end if none is included in the String.
	 * @return This {@code ByteBufferWrap}, for convenience.
	 */
	public ByteBufferWrap putString(String str) {
		setWriting();
		try {
			buffer.put(str.getBytes("US-ASCII"));
			if (!str.endsWith(String.valueOf((char)END_OF_STRING)))
			buffer.put(END_OF_STRING);
		} catch(Exception e) {
			e.printStackTrace();
		}
		
		return this;
	}

	/**
	 * Reads a byte from this buffer.
	 * @return The byte read.
	 */
	public byte getByte() {
		setReading();
		return buffer.get();
	}
	
	/**
	 * Same as {@code getByte()}, but doesn't increment the buffer's position.
	 * @return The {@code byte} read.
	 */
	public byte peekByte() {
		setReading();
		byte b = buffer.get();
		buffer.position(buffer.position()-1);
		return b;
	}
	
	/**
	 * Reads an unsigned byte from this buffer.
	 * @return The {@code byte} read, as a {@code short}.
	 */
	public short getUnsignedByte() {
		return Utilities.getUnsignedByte(getByte());
	}
	
	/**
	 * Same as {@code getUnsignedByte()}, but doesn't increment the buffer's position.
	 * @return The {@code byte} read, as a {@code short}.
	 */
	public short peekUnsignedByte() {
		return Utilities.getUnsignedByte(peekByte());
	}
	
	/**
	 * Reads a character from this buffer.
	 * @return The {@code char} read.
	 */
	public char getChar() {
		setReading();
		return buffer.getChar();
	}
	
	/**
	 * Reads a short integer from this buffer.
	 * @return The {@code short} read.
	 */
	public short getShort() {
		setReading();
		return buffer.getShort();
	}
	
	/**
	 * Reads an unsigned short integer from this buffer.
	 * @return The {@code short} read, as an {@code int}.
	 */
	public int getUnsignedShort() {
		return Utilities.getUnsignedShort(getShort());
	}
	
	/**
	 * Reads an integer from this buffer.
	 * @return The {@code int} read.
	 */
	public int getInt() {
		setReading();
		return buffer.getInt();
	}	
	
	/**
	 * Reads a long integer from this buffer.
	 * @return The {@code long} read.
	 */
	public long getLong() {
		setReading();
		return buffer.getLong();
	}
	
	//private StringBuilder strBuf = new StringBuilder();
	
	/**
	 * Reads a String from this buffer.
	 * @return A String read from the buffer using ASCII encoding.
	 * @throws StringReadException If the buffer is read to the end but no null char is found.
	 */
	public String getString() throws StringReadException {
		setReading();

		StringBuilder temp = new StringBuilder();
		
		byte ch;
		//boolean ends_with_null = false;

		do {
			if (buffer.remaining()>0) {
				ch = buffer.get();

				//if (ch==END_OF_STRING)
				//	ends_with_null = true;

				temp.append((char)ch);
			} else {
				/*
				System.out.println("*****error reading string!******");
				String soFar = "";

				for (byte c : b.toString().getBytes())
				{
					soFar += String.format("%x ", c);
				}
				System.out.println("So far getString() has read:\n" +soFar);
				 */
				throw STRING_READ_EXCEPTION;
			}
		} while(ch != END_OF_STRING);

		return temp.toString();
	}
	
	/**
	 * Default {@code StringReadException} to throw. Prevents needless creation of objects.
	 */
	private final StringReadException STRING_READ_EXCEPTION = new StringReadException();
}