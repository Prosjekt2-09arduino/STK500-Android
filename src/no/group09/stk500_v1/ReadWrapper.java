package no.group09.stk500_v1;

import java.io.*;

/**
 * This class is responsible for mediating with the actual reading thread.
 * Blocking reads are performed in the readerThread, while this thread deals
 * with requests from the caller and feedback from the reading thread.
 * 
 * The sockets and streams used do not support interrupts or timeouts, so the
 * reading thread may block for a long time. When it eventually finishes, the
 * returned byte(s) may be relevant for a different (newer) request than the
 * one that started the request to begin with. 
 */
public class ReadWrapper implements Runnable {
	private InputStream input;
	private Logger logger;
	private Reader reader;
	private Thread readerThread;
	
	private boolean strictPolicy;
	
	private volatile State state;
	private State oldState;
	
	byte[] buffer;
	int bytesRead;
	
	int byteResult;
	
	/**Set when reader has something to report**/
	private volatile boolean readerAlert;
	
	/**Set when caller wants to cancel a request**/
	private volatile boolean cancelRequest;
	
	/**Set when caller wants to stop completely - attempt graceful termination**/
	private volatile boolean terminate;
	
	
	/**
	 * Instance the wrapper for reading. See details in the main class documentation.
	 * @param input
	 * @param logger
	 */
	public ReadWrapper(InputStream input, Logger logger) {
		this.input = input;
		this.logger = logger;
		readerAlert = false;
		cancelRequest = false;
		terminate = false;
		oldState = null;
		
		strictPolicy = false;
		
		buffer = null;
		byteResult = -2;
		bytesRead = -1;
		
		reader = new Reader();
		readerThread = new Thread(reader);
		readerThread.start();
		state = State.WAITING;
		
		
	}
	
	/**
	 * Set whether results arriving after cancellation but before new requests should
	 * be accepted. If buffers differ or different operations are run (buffer vs single
	 * byte), this will not be considered at all (result will be discarded).
	 * @param choice true to enable, false to disable
	 */
	public synchronized void setStrictPolicy(boolean choice) {
		strictPolicy = choice;
	}
	
	/**
	 * Check if a result can be fetched
	 * @return true if it's ready
	 */
	public synchronized boolean isDone() {
		return (state == State.RESULT_READY);
	}
	
	/**
	 * Check if the request failed
	 * @return true if fail
	 */
	public synchronized boolean isFailed() {
		return (state == State.FAIL);
	}
	
	/**
	 * Check if a read request can be submitted
	 * @return true if ready
	 */
	public synchronized boolean canAcceptWork() {
		return (state == State.WAITING || state == State.REQUEST_CANCELLED_RESULT_READY ||
				(state == State.REQUEST_CANCELLED && !strictPolicy));
	}
	
	/**
	 * Ask the reader to read a single byte. Returns immediately.
	 * @return true if request was accepted, false if not
	 */
	public synchronized boolean requestReadByte() {
		if (canAcceptWork()) {
			oldState = state;
			if (state == State.REQUEST_CANCELLED_RESULT_READY) {
				if (buffer != null) {
					//the last read was with a buffer - discard that result
					oldState = State.WAITING;
					state = State.READING;
					buffer = null;
				} else if (!strictPolicy) {
					//accept the result
					oldState = State.READING;
					state = State.RESULT_READY;
				} else {
					//strict policy enabled, discard result
					oldState = State.WAITING;
					state = State.READING;
					buffer = null;
				}
			} else if (state == State.REQUEST_CANCELLED) {
				//non-strict is implied from canAcceptWork allowing this state through
				if (buffer != null) {
					//old read was with buffer, unable to continue
					return false;
				}
				state = State.READING_CONTINUED;
			} else {
				//this is request arriving to a prepared wrapper
				buffer = null;
				state = State.READING;
			}
		} else {
			//can't accept work
			return false;
		}
		return true;
	}
	
	/**
	 * Ask the reader to read bytes into the buffer
	 * @return 
	 * @throws IllegalArgumentException if a different buffer is already being read into
	 */
	public synchronized boolean requestReadIntoBuffer(byte[] buffer) {
		if (canAcceptWork()) {
			oldState = state;
			if (state == State.REQUEST_CANCELLED_RESULT_READY) {
				if (this.buffer == null) {
					//previous read was without a buffer, discard that result
					oldState = State.WAITING;
					state = State.READING;
					this.buffer = buffer;
				} else if (buffer != this.buffer) {
					//previous read was with a buffer, but a different one - discard
					oldState = State.WAITING;
					state = State.READING;
					this.buffer = buffer;
				} else if (buffer == this.buffer && !strictPolicy) {
					//buffers identical and non-strict policy: accept the result
					oldState = State.READING;
					state = State.RESULT_READY;
				} else {
					//strict: discard
					this.buffer = buffer;
					oldState = State.WAITING;
					state = State.READING;
				}
			} else if (state == State.REQUEST_CANCELLED) {
				//non-strict as per canAcceptWork(). Check if possible to resume.
				if (this.buffer != buffer) {
					state = State.READING_CONTINUED;
				} else {
					return false;
				}
			}
		} else {
			//can't accept work
			return false;
		}
		//work accepted (possibly already done, in the case of
		//REQUEST_CANCELLED_RESULT_READY)
		return true;
	}
	
	/**
	 * Get the result. This is either a read byte or the number of read bytes.
	 * This depends on whether the singleRead of buffer methods are used.
	 */
	public synchronized int getResult() {
		if (!(state == State.RESULT_READY)) {
			throw new IllegalStateException("Can't get results until ready");
		}
		return (buffer == null) ? byteResult: bytesRead;
	}
	
	/**
	 * Used when a result is no longer needed or wanted. Typically used on timeouts.
	 * Make sure to wake the thread if it's idle before running.
	 **/
	public void cancelRequest() {
		cancelRequest = true;
	}
	
	/**
	 * Set flag to indicate a complete termination of the reader and wrapper.
	 * Should probably only be used before shutting down the protocol work.
	 */
	public void terminate() {
		terminate = true;
	}

	@Override
	public void run() {
		while (!terminate) {
			switch (state) {
			case WAITING : {
				if (oldState != state) {
					logger.debugTag("Wrapper idling...");
				}
				break;
			}
			case READING : {
				if (oldState != state) {
					logger.debugTag("Wrapper starting read action");
					//read to buffer or just a single byte
					if (buffer == null) {
						reader.requestRead();
					} else {
						reader.requestRead(buffer, 0, buffer.length);
					}
				}
				
				if (reader.isDone()) {
					state = State.RESULT_READY;
				}
				
				if (reader.operationFailed()) {
					state = State.FAIL;
				}
				
				//cancel the read
				if (cancelRequest) {
					state = State.REQUEST_CANCELLED;
					//no point to notify reader, as it can't be stopped
				}
				break;
			}
			case RESULT_READY : {
				if (oldState == State.READING) {
					//get the result - first check what kind
					if (buffer == null) {
						//get single byte
						byteResult = reader.getSingleByteResult();
					} else {
						//get number of read bytes. actual bytes stored in buffer
						bytesRead = reader.getReadBytesFromResult();
					}
				}
				break;
			}
			case REQUEST_CANCELLED: {
				if (state != oldState) {
					logger.debugTag("Read request cancelled");
				}
				break;
			}
			case REQUEST_CANCELLED_RESULT_READY : {
				if (state != oldState) {
					logger.debugTag("Result arrived after cancellation");
					//get result based on request type
					if (buffer == null) {
						byteResult = reader.getSingleByteResult();
					} else {
						bytesRead = reader.getReadBytesFromResult();
					}
				}
				break;
			}
			case READING_CONTINUED : {
				if (oldState == State.REQUEST_CANCELLED) {
					logger.debugTag("Resumed reading from a cancelled request");
				}
				
				if (reader.isDone()) {
					state = State.RESULT_READY;
				}
				
				if (reader.operationFailed()) {
					state = State.FAIL;
				}
				
				//cancel resumed read
				if (cancelRequest) {
					state = State.REQUEST_CANCELLED;
					//no point to notify reader, as it can't be stopped
				}
				break;
			}
			case FAIL : {
				if (oldState != state) {
					logger.debugTag("Wrapper class failed to get a result from the reader");
				}
				break;
			}
			default : {
				throw new IllegalArgumentException("Unhandled state:" + state);
			}
			}
			//end switch
			
			oldState = state;
		}
		reader.fullStop = true;
	}
	
	/**
	 * States the ReadWrapper can be in.
	 */
	public enum State {
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
		
		/**If it failed completely**/
		FAIL
	}

	/**
	 * Class doing the blocking IO.
	 */
	class Reader implements Runnable {
		/**Stops the thread (when the reading is eventually done, not very responsive)**/
		public volatile boolean fullStop;
		
		//vars for work control
		private boolean startWork;
		private boolean resultReady;
		private boolean ready;
		
		//vars for buffered reading
		private byte[] buffer;
		private int readOffset;
		private int maxReadLength;
		private int bytesRead;
		
		//hold the single byte return
		private int singleResult;
		
		/**Get the read byte. Must be called to return the reader to ready state**/
		synchronized int getSingleByteResult() {
			if (buffer != null) {
				throw new IllegalStateException("Can't get single byte when buffer" +
						"was requested");
			}
			resultReady = false;
			ready = true;
			return singleResult;
		}
		
		/**Get the number of bytes read into the buffer. This has to be called to reset
		 * the reader back to a ready state.
		 * @return The number of bytes read, the bytes themselves are stored in the buffer
		 */
		synchronized int getReadBytesFromResult() {
			if (buffer == null) {
				throw new IllegalStateException("Can't get read byte count when single" +
						" byte was requested");
			}
			resultReady = false;
			ready = true;
			buffer = null;
			return bytesRead;
		}
		
		/**Request the reading of a single byte**/
		synchronized void requestRead() {
			ready = false;
			startWork = true;
			singleResult = -2;
			buffer = null;
		}
		
		/**Request a read to fill the supplied buffer**/
		synchronized void requestRead(byte[] buffer) {
			requestRead(buffer, 0, buffer.length);
		}
		
		/**Request a read to fill the specified section of the buffer**/
		synchronized void requestRead(byte[] buffer, int offset, int len) {
			this.buffer = buffer;
			this.maxReadLength = len;
			this.readOffset = offset;
			ready = false;
			startWork = true;
			bytesRead = 0;
		}
		
		/**Read several bytes**/
		private void readToBuffer() {
			try {
				bytesRead = input.read(buffer, readOffset, maxReadLength);
				resultReady = true;
				startWork = false;
			} catch (IOException e) {
				setFailureFlags();
				logger.debugTag("IOException on attempt to read " + maxReadLength +
						" byte(s). Message: " + e.getMessage());
			}
		}
		
		
		/**Read a single byte**/
		private void read() {
			try {
				singleResult = input.read();
				resultReady = true;
				startWork = false;
			} catch (IOException e) {
				setFailureFlags();
				logger.debugTag("IOException on attempt to read single byte." +
						"Message: " + e.getMessage());
			}
		}
		
		private void setFailureFlags() {
			resultReady = false;
			startWork = false;
			ready = false;
		}
		
		/**Whether the reader is ready to receive work or not**/
		public synchronized boolean isReady() {
			return ready;
		}

		/**Whether the reading is completed or not**/
		public synchronized boolean isDone() {
			return resultReady;
		}
		
		/**
		 * If work has stopped but no result been generated.
		 * Probably caused by IOException
		 * @return true if the read operation failed entirely
		 */
		public synchronized boolean operationFailed() {
			return (!resultReady && !ready && !startWork);
		}
		
		@Override
		public void run() {
			fullStop = false;
			resultReady = false;
			ready = true;
			while (!fullStop) {
			
				if (ready) {
					try {
						Thread.currentThread().wait();
					} catch (InterruptedException e) {
						logger.debugTag("Reader woken up." + 
								" Message: " +e.getMessage());
					}					
				} else if (startWork) {
					//check if buffered or single reading should be used
					if (buffer == null) {
						read();
					} else {
						readToBuffer();
					}
				} else if (resultReady) {
					try {
						Thread.sleep(30);
					} catch (InterruptedException e) {
						logger.debugTag("Reader woken from rest");
					}
				}
			}
		}
		
	}
}
