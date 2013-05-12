package no.group09.stk500_v2;

/**
 *  Copyright 2013 UbiCollab
 *  
 *  This file is part of STK500ForJava.
 *
 *	STK500ForJava is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	STK500ForJava is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with STK500ForJava.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Provides an abstraction level for messages in the STK500 protocol
 */
public class Message {

    private byte[] body;
    private byte checkSum;
    private byte sequenceNumber;
    private byte[] messageSize;
    private byte[] completeMessage;

    /**
     * Constructor for message received from Arduino device.
     *
     * @param completeMessage
     */
    public Message(ArrayList<Byte> completeMessage) {

        this.completeMessage = new byte[completeMessage.size()];

        for (int i = 0; i < completeMessage.size(); i++) {
            this.completeMessage[i] = completeMessage.get(i);
        }

        sequenceNumber = this.completeMessage[2];
        body = new byte[this.completeMessage.length - 6];

        for (int i = 5; i < this.completeMessage.length; i++) {
            for (int j = 0; j < body.length; j++) {
                body[j] = this.completeMessage[i];
            }
        }
        checkSum = this.completeMessage[this.completeMessage.length - 1];
    }

    /**
     * Constructor for message to send to Arduino device.
     *
     * @param sequenceNumber
     * @param body
     * @throws ArrayStoreException on illegal body message size.
     */
    public Message(byte sequenceNumber, byte[] body) {

        this.body = body;
        this.sequenceNumber = sequenceNumber;
        messageSize = new byte[2];
        packMessageSize();

        completeMessage = new byte[body.length + 6];
        completeMessage[0] = STK_Message.MESSAGE_START.getByteValue();
        completeMessage[1] = sequenceNumber;
        completeMessage[2] = messageSize[0];
        completeMessage[3] = messageSize[1];
        completeMessage[4] = STK_Message.TOKEN.getByteValue();

        for (int i = 5; (i - 5) < body.length; i++) {
            completeMessage[i] = body[i - 5];
        }
        byte[] messageWithoutChecksum = new byte[completeMessage.length - 1];

        for (int i = 0; i < messageWithoutChecksum.length; i++) {
            messageWithoutChecksum[i] = completeMessage[i];
        }
        checkSum = calculateChecksum(messageWithoutChecksum);
        completeMessage[completeMessage.length - 1] = checkSum;
    }


    /**
     * Constructor for responses from the Arduino
     * @param header
     * @param body
     */
    public Message(byte[] header, byte[] body) {
    	sequenceNumber = header[1];
    	this.body = body;
    	completeMessage = new byte[header.length + body.length];
    	for (int i = 0, headC = 0, bodyC = 0; i < body.length; i++) {
    		//stop short of checksum
			if (i < header.length - 1) {
    			completeMessage[i] = header[headC];
    			headC++;
    		} else if (bodyC < body.length) {
    			//copy the body
    			completeMessage[i] = body[bodyC];
    			bodyC++;
    		} else {
    			//add the checksum
    			checkSum = header[headC];
    			completeMessage[i] = checkSum; 
    		}
    	}
	}

	/**
     * @param message The full message.
     * @return Size of the message as two bytes, most significant first
     */
    private void packMessageSize() {

        //store the 8 least significant bits
        messageSize[1] = (byte) (body.length & 0xFF);
        //store the next 8 bits
        messageSize[0] = (byte) ((body.length >> 8) & 0xFF);
    }

    /**
     * Used after instancing to get the bytes to send
     *
     * @return
     */
    public byte[] getCompleteMessage() {
        return completeMessage.clone();
    }

    /**
     * Get the body of a message, used on responses
     * @return
     */
    public byte[] getBody() {
        return body.clone();
    }

    /**
     * @return unsigned value of sequenceNumber
     */
    public int getSequenceNumber() {
        return 0xFF & (int) sequenceNumber;
    }
    
    /**
	 * Uses all characters in message including MESSAGE_START and
	 * MESSAGE_BODY. XOR of all bytes.
	 *
	 * @param message The full message.
	 * @return Checksum (byte) of message.
	 */
	private static byte calculateChecksum(byte[] message, int endIndex) {
		byte checkSum = message[0];
		for (int i = 1; i < endIndex; i++) {
			checkSum ^= message[i];
		}
	    return checkSum;
	}

	/**
	 * Uses all characters in message including MESSAGE_START and
	 * MESSAGE_BODY. XOR of all bytes.
	 * Provided for convenience; simply uses the other variant
	 * 
	 * @param message The full message.
	 * @return Checksum (byte) of message.
	 */
	private static byte calculateChecksum(byte[] message) {
		return calculateChecksum(message, message.length);
	}

	/**
     * Check if the message is valid
     * @return
     */
    public boolean isValidChecksum() {
    	return (checkSum == calculateChecksum(completeMessage, completeMessage.length - 1));
    }

    /**
     * Check if the message in byte form is valid
     * @param b Array of bytes, should include checksum byte
     * @return
     */
    public static boolean isValidChecksum(byte[] b) {

        if (b[b.length - 1] == calculateChecksum(b, b.length - 1)) {
            return true;
        }
        return false;
    }

}
