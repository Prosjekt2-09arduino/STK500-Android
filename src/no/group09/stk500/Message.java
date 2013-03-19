package no.group09.stk500;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides an abstraction level for messages in the STK500 protocol
 */
public class Message {
    private byte[] body;
    private byte checksum;
    private byte sequenceNumber;
    /**
     * The complete message
     */
    private byte[] data;
    private Logger logger;

    /**
     * Constructor for message to Arduino
     *
     * @param completeMessage
     */
    public Message(ArrayList<Byte> completeMessage) {

    }

    /**
     * Constructor for message from Arduino device
     *
     * @param sequenceNumber
     * @param body
     */
    public Message(byte sequenceNumber, byte[] body) {

        this.body = body;
        this.sequenceNumber = sequenceNumber;
        //TODO: Build message with constants and make checksum

        try {

        } catch (NotImplementedException e) {
            logger.log(Level.parse(e.getLocalizedMessage()), e.getMessage(), e.getStackTrace());
        }
    }


    /**
     * Uses all characters in message including MESSAGE_START and
     * MESSAGE_BODY. XOR of all bytes.
     *
     * @param message The full message.
     * @return Checksum (byte) of message.
     */
    private byte calculateChecksum(byte[] message) {

        byte checkSum = 0;

        for (byte b : message) {
            checkSum ^= b;
        }
        return checkSum;
    }

    /**
     * @param messages The full message.
     * @return Size of the message as two bytes, most significant first
     */
    public byte[] getMessageSize(byte[] messages) {

        byte[] messageSize = new byte[2];
        messageSize[0] = (byte) (messages.length & 0xFF);

        for (int i = 0; i < messages.length; i++) {
            messageSize[1] += (byte) (messages.length & 0xFF);
        }

        return messageSize;
    }

    /**
     * Used after instancing to get the bytes to send
     *
     * @return
     */
    public byte[] getData() {
        return data.clone();
    }

    public boolean isValidChecksum(byte[] b) {

        if (b[b.length - 1] == calculateChecksum(b)) {
            return true;
        }
        return false;
    }

}
