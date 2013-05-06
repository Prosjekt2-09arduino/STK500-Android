package no.group09.stk500_v1;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public interface IReader {
	public static final int RESULT_END_OF_STREAM = -1;
	public static final int RESULT_NOT_DONE = -2;
	public static final int TIMEOUT_BYTE_RECEIVED = -3;
	
	/**Get the state of the reader**/
	public EReaderState getState();
	
	//TODO: consider startRead();
	
	/**
	 * Get the read byte
	 * @return Integer.MAX_VALUE if the state does not have a result or supports reading
	 * results
	 * @return Otherwise returns an integer from 0-255 (inclusive)
	 */
	public int getResult();
	
	public int read(TimeoutValues timeout) throws TimeoutException, IOException;
	
	/**Stop the reader**/
	public void stop();
	
	/**Start the reader**/
	public void start();
	
	/**
	 * Attempt to forget received unread bytes.
	 */
	public void forget();
}
