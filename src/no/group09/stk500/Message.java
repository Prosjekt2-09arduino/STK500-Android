package no.group09.stk500;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Provides an abstraction level for messages in the STK500 protocol
 */
public class Message {
	private byte[] body;
	private byte checksum;
	private byte sequenceNumber;
	/**The complete message**/
	private byte[] data;
	
	public Message(byte sequenceNumber, byte[] body) {
		this.body = body;
		this.sequenceNumber = sequenceNumber;
		//TODO: Build message with constants and make checksum
		throw new NotImplementedException();
	}
	
	
	/**
	 * Uses all characters in message including MESSAGE_START and
	 * MESSAGE_BODY. XOR of all bytes.
	 */
	private byte calculateChecksum() {
		throw new NotImplementedException();
	}
	
	/**
	 * @return Size of the message as two bytes, most significant first
	 */
	private byte[] getMessageSize() {
		throw new NotImplementedException();
	}


	/**
	 * Used after instancing to get the bytes to send 
	 * @return
	 */
	public byte[] getData() {
		return data.clone();
	}

}
