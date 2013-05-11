package no.group09.stk500_v1;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * Handle reading operations and timeouts on an InputStream
 */
public interface IReader {
	public static final int RESULT_END_OF_STREAM = -1;
	public static final int RESULT_NOT_DONE = -2;
	public static final int TIMEOUT_BYTE_RECEIVED = -3;
	
	/**
	 * Get the state of the reader
	 * @return EReaderState enum value corresponding to the current state
	 */
	public EReaderState getState();
	
	/**
	 * Get the read byte
	 * @return RESULT_NOT_DONE if the state does not have a result or it does not support
	 * reading or returning results
	 * @return TIMEOUT_BYTE_RECEIVED if a timeout has occurred, but new data arrives
	 * @return Otherwise returns an integer from 0-255 (inclusive)
	 */
	public int getResult();
	
	/**
	 * Read a single byte from the InputStream.
	 * @param timeout How long the reading operation can execute before a
	 * TimeoutException should be thrown.
	 * @return int between 0 and 255 (inclusive)
	 * @throws TimeoutException If reading took too long
	 * @throws IOException If a problem occurred with the stream
	 */
	public int read(TimeoutValues timeout) throws TimeoutException, IOException;
	
	/**
	 * Stop the reader. start() will still be able to restart it.
	 * @return true if the reader could be ordered to stop or is currently STOPPING (or
	 * STOPPED)
	 * @return false if stopping isn't possible at the time
	 */
	public boolean stop();
	
	/**
	 * Start the reader
	 * @return true if the reader could be ordered to start or is currently STARTING (or
	 * currently running)
	 * @return false if the reader can't be started
	 */
	public boolean start();
	
	/**
	 * Attempt to discard received unread bytes.
	 */
	public void forget();
	
	/**
	 * Checks if the current reader state has been activated/initialized.
	 * @return true if the state has been setup properly
	 * @return false if it has yet to initialize fully
	 */
	public boolean wasCurrentStateActivated();
}
