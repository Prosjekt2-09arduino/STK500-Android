package no.group09.stk500_v1;

public enum EReaderState {
	/**Not yet started. Go to WAITING**/
	STARTING,
	/**Ready for requests. Go to READING, or STOPPING**/
	WAITING,
	
	/**IO in progress - can go to RESULT_READY or TIMEOUT_OCCURRED**/
	READING,
	
	/**
	 * Result ready - goes to WAITING after the result has been retrieved using
	 * getResult().
	 */
	RESULT_READY,
	
	/**
	 * A timeout occurred while reading. Call forget on this state after spamming
	 * requests to regain communications to ignore the eventual responses. Any response
	 * at all will set the TIMEOUT_BYTE_RECEIVED to be returned by getResult().
	 * When that byte is returned, the state will switch back to waiting.
	 */
	TIMEOUT_OCCURRED,
	
	/**
	 * Termination requested
	 */
	STOPPING,
	
	/**
	 * Fully stopped
	 */
	STOPPED,
	
	/**If it failed completely. Most likely caused by IOException.**/
	FAIL
}
