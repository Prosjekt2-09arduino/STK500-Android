package no.group09.stk500_v1;

/**
 * This enum contains every possible state an {@link IReader} can be in. 
 */
public enum EReaderState {
	/**
	 * Not yet started. Eventually changes to WAITING**/
	STARTING,
	
	/**
	 * Ready for requests. Go to READING on read(), or STOPPING on stop()
	 */
	WAITING,
	
	/**
	 * IO in progress goes to RESULT_READY once reading is completed, or to
	 * TIMEOUT_OCCURRED if nothing arrives in time.
	 */
	READING,
	
	/**
	 * Result ready - goes to WAITING after the result has been retrieved using
	 * getResult().
	 */
	RESULT_READY,
	
	/**
	 * A timeout occurred while reading. Call forget() on this state after spamming
	 * requests to regain communications to ignore the eventual responses. Any response
	 * at all will set the TIMEOUT_BYTE_RECEIVED to be returned by getResult().
	 * When that byte is returned, the state will switch back to WAITING.
	 */
	TIMEOUT_OCCURRED,
	
	/**
	 * Termination requested, just let the state finish shutting down before doing
	 * anything.
	 */
	STOPPING,
	
	/**
	 * Fully stopped. An IReader can be started or completely shut down while in this
	 * state.
	 */
	STOPPED,
	
	/**
	 * If operations failed completely. Most likely caused by IOException.
	 * Returns to WAITING after getResult() is called.
	 **/
	FAIL
}
