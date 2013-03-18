package no.group09.stk500;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class STK500 {
	
	private BufferedOutputStream output;
	private BufferedInputStream input;
	/**Set as int due to no unsigned byte in Java**/
	private int sequenceNumber;
	//TODO: Add field for binary file to program and add to constructor - determine field type
	
	//TODO: Incorporate Message class abstraction layer
	//TODO: Consider Message.send() vs send(message.getData())
	
	public STK500 (BufferedOutputStream output, BufferedInputStream input, Object binaryFile) {
		this.output = output;
		this.input = input;
		sequenceNumber = 0;
		throw new NotImplementedException();
	}
	
	private void send(byte[] data) {
		throw new NotImplementedException();
	}
	
	
	
	/**
	 * @return Sequence number between 0 and 255; wraps back to 0.
	 */
	private byte getNewSequenceNumber() {
		//TODO: Handle unsigned byte encoding
		sequenceNumber = (sequenceNumber == 255) ? 0 : sequenceNumber++;
		return (byte) sequenceNumber;
	}
	
	
	private byte[] read() {
		throw new NotImplementedException();
	}
	
	/**
	 * Get the unsigned value of a byte
	 * @param unsignedByte
	 * @return
	 */
	private static int decodeByte(byte unsignedByte) {
		return 0xFF & unsignedByte;
	}
}
