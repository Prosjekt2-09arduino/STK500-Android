package no.group09.stk500Test;

import no.group09.stk500.Message;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

/**
 * User: Nina Margrethe Smørsgård
 * GitHub: https://github.com/NinaMargrethe/
 * Date: 3/18/13
 */
public class MessageTest {

    private byte b;
    private byte[] body;
    private Message message;

    public MessageTest() {
        b = 0xf;
        body = new byte[8];

        for (int i = 0; i < body.length; i++) {
            body[i] = (byte) i;
        }

        message = new Message(b, body);
    }

    @Test
    public void testCalculateChecksum() {
        assertEquals(23, message.getCalculatedChecksum(body));
    }

    @Test
    public void testGetMessageSize() throws Exception {
        byte[] expectedMessageSize = new byte[2];
        expectedMessageSize[0] = (byte) 1;
        expectedMessageSize[1] = (byte) 2;

        assertEquals(expectedMessageSize.length, message.getMessageSize(body).length);
        assertArrayEquals(expectedMessageSize, message.getMessageSize(body));

    }
}
