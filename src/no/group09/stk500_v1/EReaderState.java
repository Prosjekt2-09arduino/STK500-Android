package no.group09.stk500_v1;

public enum EReaderState {
	/**Not yet started. Go to WAITING**/
	STARTING,
	/**Ready for requests. Go to READING, or STOPPING**/
	WAITING,
	
	/**IO in progress - can go to RESULT_READY or TIMEOUT_OCCURRED**/
	READING,
	
	/**Result ready - goes to WAITING after the result has been retrieved**/
	RESULT_READY,
	
	/**
	 * A timeout occurred while reading.  
	 */
	TIMEOUT_OCCURRED,
	
	/**
	 * Caller not waiting for result, but something was received. Decide whether to
	 * accept the newly received data or not as a result based on a previously
	 * selected policy. Never accepted if different buffer sizes are used.
	 **/
	TIMEOUT_OCCURRED_RESULT_READY,
	
	/**
	 * 
	 */
	TIMEOUT_OCCURED_NEW_REQUEST,
	
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
