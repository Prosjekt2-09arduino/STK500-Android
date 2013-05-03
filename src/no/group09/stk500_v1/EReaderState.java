package no.group09.stk500_v1;

public enum EReaderState {
	/**Not yet started. Go to reading**/
	STARTING,
	/**Ready for requests. Go to reading or terminate the entire thing**/
	WAITING,
	
	/**IO in progress - can go to ResultReady or RequestCancelled**/
	READING,
	
	/**result ready - goes to Waiting after result is retrieved**/
	RESULT_READY,
	
	/**
	 * Caller no longer interested in result. Goes to REQUEST_CANCELLED_RESULT_READY
	 * if the reader returns something before a new request is received, or to
	 * READING_CONTINUED if a new request is received before that.  
	 */
	REQUEST_CANCELLED,
	
	/**
	 * Caller not waiting for result, but something was received. Decide whether to
	 * accept the newly received data or not as a result based on a previously
	 * selected policy. Never accepted if different buffer sizes are used.
	 **/
	REQUEST_CANCELLED_RESULT_READY,
	
	/**
	 * A request had been cancelled but a response never came before an identical
	 * request arrived. Let the reader continue to read (there's no sane way to
	 * stop it reliably, so attempts to use different buffer sizes may cause issues).
	 **/
	READING_CONTINUED,
	
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
