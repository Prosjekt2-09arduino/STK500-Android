package no.group09.stk500;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

public class STK500 {
	/**Size of message field without the message body**/
	public static final int MESSAGE_HEADER_SIZE = 6;
	
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
		System.out.println(getProgrammerVersion());
	}
	
	/**
	 * Discover if you're dealing with a STK500 or AVRISP
	 * @return
	 */
	private String getProgrammerVersion() {
		//send command to get AVRISP or STK500 response
		byte[] signBody = {STK_Message.CMD_SIGN_ON.getByteValue()}; 
		
		try {
			send(signBody);
			Message version = read();

			byte[] body;
			String ret = "";
			body = version.getBody();
			if (body[0] != STK_Message.CMD_SIGN_ON.getByteValue() {
				throw new RuntimeException("Wrong response ID: expected " +
						STK_Message.CMD_SIGN_ON.getByteValue() + ", but " +
						"received " + body[0] + ".");
			}
			//convert byte array to string
			for (int i = 0; i < body.length; i++) {
				byte b = body[i];
				ret += (char) decodeByte(b);
			}
		} catch (IOException e) {
			// TODO Retry x times
			e.printStackTrace();
		}
	}
	
	private void EnterProgrammingMode() {
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
	
	
	/**
	 * Create a message and send it to the Arduino
	 * @param body Command to send
	 * @throws IOException
	 */
	private void send(byte[] body) throws IOException {
		Message message = new Message(getNewSequenceNumber(), body);
		byte[] bytes = message.getData();
		output.write(bytes, 0, bytes.length);
	}

	/**
	 * Reads the response from the Arduino, sends it to the Message class
	 * for parsing and returns the completed message.
	 * @return Message with response
	 * @throws IOException
	 */
	private Message read() throws IOException {
		Message response;
		//The read byte as an int or -1 for nothing more to read
		int read = 0;
		ArrayList<Byte> data = new ArrayList<Byte>();
		while(read != -1) {
			read = input.read();
			if (read != -1) {
				data.add((byte) read);
			} else {
				//stop reading - create message and change sequence number
				response = new Message(data);
				int responseSequenceNumber = response.getSequenceNumber();
				if (responseSequenceNumber == sequenceNumber + 1 ||
						(responseSequenceNumber == 0 && sequenceNumber == 255)) {
				} else {
					//sequence number doesn't match
					throw new IllegalArgumentException("Wrong sequence number:" +
							"old was " + sequenceNumber + " and the received "+ 
							"message is " + responseSequenceNumber);
				}
				return response;
			}
		}
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
