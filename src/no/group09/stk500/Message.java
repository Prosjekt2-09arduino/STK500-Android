package no.group09.stk500;

import java.util.ArrayList;

/**
 * Provides an abstraction level for messages in the STK500 protocol
 */
public class Message {

    private byte[] body;
    private byte checkSum;
    private byte sequenceNumber;
    private byte[] completeMessage;

    /**
     * Constructor for message to Arduino device.
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
    }

    /**
     * Constructor for message from Arduino device.
     *
     * @param sequenceNumber
     * @param body
     * @throws ArrayStoreException on illegal body message size.
     */
    public Message(byte sequenceNumber, byte[] body) {

        this.body = body;
        this.sequenceNumber = sequenceNumber;

        completeMessage = new byte[body.length + 6];
        completeMessage[0] = STK_Message.MESSAGE_START.getByteValue();
        completeMessage[1] = sequenceNumber;
        completeMessage[2] = getMessageSize(body)[0];
        completeMessage[3] = getMessageSize(body)[getMessageSize(body).length - 1];
        completeMessage[4] = STK_Message.TOKEN.getByteValue();

        for (int i = 5; i < body.length; i++) {
            completeMessage[i] = body[i];
        }
        byte[] messageWithoutChecksum = new byte[completeMessage.length - 1];

        for (int i = 0; i < messageWithoutChecksum.length; i++) {
            messageWithoutChecksum[i] = completeMessage[i];
        }
        completeMessage[completeMessage.length - 1] = calculateChecksum(messageWithoutChecksum);
    }


    /**
     * Uses all characters in message including MESSAGE_START and
     * MESSAGE_BODY. XOR of all bytes.
     *
     * @param message The full message.
     * @return Checksum (byte) of message.
     */
    private byte calculateChecksum(byte[] message) {

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
    public byte[] getCompleteMessage() {
        return completeMessage.clone();
    }

    public byte[] getBody() {
        return body.clone();
    }

    /**
     * @return unsigned value of sequenceNumber
     */
    public int getSequenceNumber() {
        return 0xFF & (int) sequenceNumber;
    }

    public boolean isValidChecksum(byte[] b) {

        if (b[b.length - 1] == calculateChecksum(b)) {
            return true;
        }
        return false;
    }

}
