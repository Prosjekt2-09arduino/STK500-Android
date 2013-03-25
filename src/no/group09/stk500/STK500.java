package no.group09.stk500;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;

public class STK500 {
	/**Size of message field without the message body**/
	public static final int MESSAGE_HEADER_SIZE = 6;
	
	private OutputStream output;
	private InputStream input;
	private Logger logger;
	/**Set as int due to no unsigned byte in Java**/
	private int sequenceNumber;
	//TODO: Add field for binary file to program and add to constructor - determine field type
	
	//TODO: Incorporate Message class abstraction layer
	
	public STK500 (OutputStream output, InputStream input, Logger logger) {
		this.output = output;
		this.input = input;
		this.logger = logger;
		sequenceNumber = 1;
		logger.debugTag("Initializing programmer");
		/*
		 * To connect:
		 * • CMD_SIGN_ON
		 * • CMD_GET_PARAMETER, PARAM_TOPCARD_DETECT
		 * • CMD_GET_PARAMETER, PARAM_HW_VER
		 * • CMD_GET_PARAMETER, PARAM_SW_MAJOR
		 * • CMD_GET_PARAMETER, PARAM_SW_MINOR
		 */
		//TODO No retries until timeouts are implemented
		String ver = getProgrammerVersion(0);
		logger.debugTag(ver);
		logger.printToConsole(ver);
	}
	
	/**
	 * Discover if you're dealing with a STK500 or AVRISP
	 * @return Can return an error message
	 */
	private String getProgrammerVersion(int retries) throws IllegalStateException {
		//send command to get AVRISP or STK500 response
		byte[] signBody = {STK_Message.CMD_SIGN_ON.getByteValue()}; 
		
		try {
			//Send the command, then read the response
			//TODO Add timeouts
			send(signBody);
			Message version = read();

			byte[] body;
			//String to return
			String ret = "";
			body = version.getBody();
			if (body[0] != STK_Message.CMD_SIGN_ON.getByteValue()) {
				throw new IllegalStateException("Wrong response ID: expected " +
						STK_Message.CMD_SIGN_ON.getByteValue() + ", but " +
						"received " + body[0] + ".");
			}
			//convert byte array to string
			for (int i = 0; i < body.length; i++) {
				byte b = body[i];
				ret += (char) decodeByte(b);
			}
			return ret;
		} catch (IOException e) {
			//Retry process if there are communication issues
			if (retries > 0) {
				return getProgrammerVersion(retries - 1);
			} else {
				return "Error: Communication problem";
			}
		}
	}
	
	private void EnterProgrammingMode() {
		throw new RuntimeException("Not yet implemented");
	}
	
	/**
	 * @return Sequence number between 0 and 255; wraps back to 0.
	 */
	private byte getNewSequenceNumber() {
		sequenceNumber = (sequenceNumber == 255) ? 0 : sequenceNumber++;
		return (byte) sequenceNumber;
	}
	
	
	/**
	 * Create a message and send it to the Arduino
	 * read() should be called after this to get the response
	 * @param body Command to send
	 * @throws IOException
	 */
	private void send(byte[] body) throws IOException {
		Message message = new Message(getNewSequenceNumber(), body);
		byte[] bytes = message.getCompleteMessage();
		String txt = "Sending bytes: " + Arrays.toString(bytes);
		logger.debugTag(txt);
		logger.printToConsole(txt);
		output.write(bytes, 0, bytes.length);
	}

	/**
	 * Reads the response from the Arduino, sends it to the Message class
	 * for parsing and returns the completed message.
	 * @return Message with response
	 * @throws IOException
	 * @throws IllegalStateException
	 */
	private Message readLegacy() throws IOException, IllegalStateException {
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
				//The response sequence number should be the same as the one
				//belonging to the command
				if (responseSequenceNumber == sequenceNumber) {
					if (!response.isValidChecksum()) {
						throw new IllegalStateException("Response checksum mismatch");
					}
				} else {
					//sequence number doesn't match
					throw new IllegalStateException("Wrong sequence number:" +
							"old was " + sequenceNumber + " and the received "+ 
							"message is " + responseSequenceNumber);
				}
				return response;
			}
		}
		//TODO Ensure this can't be reached
		return null;
	}
	
	private Message read() throws IOException {
		Message response = null;
		byte[] header = new byte[6];
		/**Number of bytes to read of the header**/
		int headerBytes = 0;
		byte[] body = null;
		/**Number of bytes read of the body**/
		int bodyBytes = 0;
		/**Holds -1 if there are no bytes to read, otherwise the byte as an integer**/
		int readResult = 0;
		/**Total number of bytes in the body to read**/
		int bodySize = 0;
		
		logger.debugTag("Preparing to read");
		//Stops when end of stream is found.
		//Also broken after the switch statement if full message read
		//TODO Add the protocol timeout restrictions
		while (readResult >= 0) {
			 readResult = input.read();
			 if (readResult == -1) {
				 //end of stream
				 logger.debugTag("End of stream encountered");
				 //TODO When does this happen? How to deal with it?
			 } else {
				 byte readByte = (byte) readResult;
				 boolean headerByteFound = false;
				 switch (headerBytes) {
				 //look for start byte
				 case 0 : {
					 if (readByte == getConstantByte("MESSAGE_START")) {
						 logger.debugTag("Start byte read");
						 headerByteFound = true;
					 } else {
						 //start byte not received, keep going
						 continue;
					 }
					 break;
				 }
				 //sequence number
				 case 1: {
					 if (decodeByte(readByte) == sequenceNumber) {
						 headerByteFound = true;
					 } else {
						 //sequence mismatch, back to start
						 headerBytes = 0;
					 }
					 break;
				 }
				 //first part of message size
				 case 2: {
					 headerByteFound = true;
					 break;
				 }
				 //both message size bytes read
				 case 3 : {
					 bodySize = unPackTwoBytes(header[2], header[3]);
					 body = new byte[bodySize];
					 headerByteFound = true;
					 break;
				 }
				 //this should be the token
				 case 4 : {
					 if (readByte == getConstantByte("TOKEN")) {
						 logger.debugTag("Token byte read");
						 headerByteFound = true;
					 } else {
						 //token not found, communication problem - reset
						 headerBytes = 0;
						 logger.debugTag("Expected token, got something else. Resetting.");
					 }
					 break;
				 }
				 //read body, or checksum if body done
				 case 5 : {
					 if (bodyBytes < bodySize) {
						 body[bodyBytes] = readByte;
						 bodyBytes++;
					 } else {
						 headerByteFound = true;
					 }
					 break;
				 }
				 //Should never happen
				 default : {
					 throw new AssertionError("ERROR: Unknown value for " +
							 "headerBytes in read(). Value: " + headerBytes);
				 }
				 }
					 
					 
				 
				 //store header byte if found
				 if (headerByteFound) {
					 header[headerBytes] = readByte;
					 headerBytes++;
					 if (headerBytes == 6) {
						 //All message read, pass on and stop reading
						 //TODO Stop timeout timer
						 logger.debugTag("Read full message!");
						 response = new Message(header, body);
						 break;
					 }
				 }
			 }
		}
		return response;
	}
	
	/**
	 * Get the byte value of a STK_Message constant
	 * @param in
	 * @return
	 */
	private static byte getConstantByte(String in) {
		return STK_Message.valueOf(in).getByteValue();
	}
	
	/**
	 * Read two unsigned bytes into an integer
	 * @param high Most significant byte
	 * @param low  Least significant byte
	 * @return
	 */
	private static int unPackTwoBytes(byte high, byte low) {
		int out = (decodeByte(high) << 8) | (decodeByte(low));
		return out;
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
