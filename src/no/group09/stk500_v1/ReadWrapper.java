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
	private boolean resultsSet;
	
	private volatile int byteReceivedAfterCancellation = 0;
	
	byte[] buffer;
	int bytesRead;
	
	int byteResult;
	
	/**Set when caller wants to cancel a request**/
	private boolean cancelRequest;
	
	/**Set when caller wants to stop completely - attempt graceful termination**/
	private boolean terminate;
	
	
	/**
	 * Instance the wrapper for reading. See details in the main class documentation.
	 * @param input
	 * @param logger
	 */
	public ReadWrapper(InputStream input, Logger logger) {
		this.input = input;
		this.logger = logger;
		cancelRequest = false;
		terminate = false;
		oldState = null;
		resultsSet = false;
		
		strictPolicy = false;
		
		buffer = null;
		byteResult = -2;
		bytesRead = -1;
		
		logger.logcat("ReadWrapper: Initializing readWrapper", "v");
		state = State.STARTING;
		reader = new Reader(this);
		readerThread = new Thread(reader);
		readerThread.start();
		while (!reader.isReady()) {
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				//nothing to do
			}
		}
		logger.logcat("ReadWrapper: ReadWrapper registered with ready reader", "v");
	}
	
	/**
	 * Set whether results arriving after cancellation but before new requests should
	 * be accepted. If buffers differ or different operations are run (buffer vs single
	 * byte), this will not be considered at all (result will be discarded).
	 * @param choice true to enable, false to disable
	 */
	public synchronized void setStrictPolicy(boolean strict) {
		logger.logcat("setStrictPolicy: Set strict policy to " + strict, "d");
		strictPolicy = strict;
	}
	
	/**
	 * Check if a result can be fetched
	 * @return true if it's ready
	 */
	public synchronized boolean isDone() {
		return (state == State.RESULT_READY && resultsSet);
	}
	
	/**
	 * Check if the request failed. Returns to idle state afterwards.
	 * @return true if fail
	 */
	public synchronized boolean checkIfFailed() {
		if (state == State.FAIL) {
			state = State.WAITING;
			notifyAll();
			return true;
		}
		return false;
	}
	
	/**
	 * Check if a read request can be submitted
	 * @return true if ready
	 */
	public synchronized boolean canAcceptWork() {
//		boolean check = (state == State.WAITING || state == State.REQUEST_CANCELLED_RESULT_READY ||
//				(state == State.REQUEST_CANCELLED && !strictPolicy));
		boolean check = (state == State.WAITING || state == State.REQUEST_CANCELLED_RESULT_READY);
		if(!check) {
			logger.logcat("canAcceptWork: Work not accepted, state: " + 
					state.toString(), "v");
		}
		return check;
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
				}
//				else if (!strictPolicy) {
//					//accept the result
//					oldState = State.READING;
//					state = State.RESULT_READY;
				else {
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
			} else if (state == State.WAITING) {
				//this is request arriving to a prepared wrapper
				buffer = null;
				state = State.READING;
			} else {
				logger.logcat("Invalid result from canAcceptWork(), should have returned false", "e");
				return false;
			}
		} else {
			//can't accept work
			return false;
		}
		notifyAll();
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
			else if (state == State.WAITING){
				state = State.READING;
				this.buffer = buffer;
			}
			else {
				logger.logcat("requestReadIntoBuffer: Wierd case, unforseen " +
						"state in request to read into buffer", "i");
				return false;
			}
		} else {
			//can't accept work
			return false;
		}
		//work accepted (possibly already done, in the case of
		//REQUEST_CANCELLED_RESULT_READY)
		notifyAll();
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
		state = State.WAITING;
		notifyAll();
		return (buffer == null) ? byteResult: bytesRead;
	}
	
	/**
	 * Used when a result is no longer needed or wanted. Typically used on timeouts.
	 * Make sure to wake the thread if it's idle before running.
	 **/
	public synchronized void cancelRequest() {
		cancelRequest = true;
		notifyAll();
	}
	
	/**
	 * Set flag to indicate a complete termination of the reader and wrapper.
	 * Should probably only be used before shutting down the protocol work.
	 */
	public synchronized void terminate() {
		terminate = true;
		notifyAll();
	}

	/**
	 * See whether the read wrapper has progressed past the start up state
	 * @return true if finished starting
	 **/
	public synchronized boolean checkIfStarted() {
		return (state != State.STARTING);
	}
	
	@Override
	public void run() {
		//actually started after the reader thread run method
		logger.logcat("run: ReadWrapper thread started", "v");
		while (!terminate) {
			// === begin switch statement ===
			//See State enum or hover over states for explanations
			switch (state) {
			case STARTING : {
				//see if the reader is initialized
				if (reader.isReady()) {
					logger.logcat("run: ReadWrapper ready", "v");
					oldState = State.STARTING;
					state = State.WAITING;
				} else {
					//wait a bit if reader not ready
					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
						//no need to act
					}
				}
				break;
			}
			case WAITING : {
				if (oldState != state) {
					logger.logcat("run: Wrapper idling...", "v");
					oldState = state;
				}
				
				//nothing to do for now, pause execution
				synchronized(this) {
					try {
						wait(1);
					} catch (InterruptedException e) {}
				}
				break;
			}
			case READING_CONTINUED : {
				if (oldState == State.REQUEST_CANCELLED) {
					//no need to do anything, as the reader was already reading
					logger.logcat("run: Resumed reading from a cancelled request", "i");
				}
				//no break, deliberately fall through into READING
			}
			case READING : {
				//if state == READING_CONTINUED, this case will also execute.
				//however, the if block will be skipped
				if (oldState == State.WAITING) {
					resultsSet = false;
					logger.logcat("run: Wrapper starting read action", "v");
					//read to buffer or just a single byte
					if (buffer == null) {
						reader.requestRead();
					} else {
						reader.requestRead(buffer, 0, buffer.length);
					}
				}
				oldState = state;
				
				//nothing to do for now, pause execution
				synchronized(this) {
					try {
						wait(1);
					} catch (InterruptedException e) {}
				}
				
				//code from here on also executed if in READING_CONTINUED state
				//switch to next state if done reading
				if (reader.isDone()) {
					state = State.RESULT_READY;
				}
				
				//probably an IOException, go to failure state
				if (reader.operationFailed()) {
					state = State.FAIL;
				}
				
				//cancel the read if requested by caller
				if (cancelRequest) {
					state = State.REQUEST_CANCELLED;
					//no point to notify reader, as it can't be stopped
				}
				break;
			}
			case RESULT_READY : {
				if (oldState == State.READING || oldState == State.READING_CONTINUED) {
					//get the result - first check what kind
					if (buffer == null) {
						//get single byte
						byteResult = reader.getSingleByteResult();
						logger.logcat("run: Result ready, read byte: " + byteResult, "v");
					} else {
						//get number of read bytes. actual bytes stored in buffer
						bytesRead = reader.getReadBytesFromResult();
						logger.logcat("run: Result ready, read " + byteResult + " bytes", "v");
					}
					
					resultsSet = true;
					oldState = state;
				}

				//nothing to do for now, pause execution
				synchronized(this) {
					try {
						wait(5);
					} catch (InterruptedException e) {}
				}
				break;
			}
			case REQUEST_CANCELLED: {
				if (state != oldState) {
					logger.logcat("run: Read request cancelled", "d");
					oldState = state;
				}
				
				if (reader.isDone()) {
					//reader finished before new request arrived
					state = State.REQUEST_CANCELLED_RESULT_READY;
				}

				//nothing to do for now, pause execution
				synchronized(this) {
					try {
						wait(1);
					} catch (InterruptedException e) {}
				}
				break;
			}
			case REQUEST_CANCELLED_RESULT_READY : {
				if (state != oldState) {
					logger.logcat("run: Result arrived after cancellation", "d");
					//get result based on request type
					if (buffer == null) {
						byteResult = reader.getSingleByteResult();
						byteReceivedAfterCancellation = byteResult;
					} else {
						bytesRead = reader.getReadBytesFromResult();
					}
					oldState = state;
				}

				//nothing to do for now, pause execution
				synchronized(this) {
					try {
						wait(5);
					} catch (InterruptedException e) {}
				}
				break;
			}
			case FAIL : {
				if (oldState != state) {
					synchronized(reader){
						reader.ready = true;
						reader.notifyAll();
					}
					
					logger.logcat("run: Wrapper class failed to get a result " +
							"from the reader", "d");
					//read type won't matter for future requests
					buffer = null;
					oldState = state;
				}

				//nothing to do for now, pause execution
				synchronized(this) {
					try {
						wait(5);
					} catch (InterruptedException e) {}
				}
				break;
			}
			default : {
				throw new IllegalArgumentException("Unhandled state:" + state);
			}
			}
			// === end switch statement ===
			
			//keep track of state transitions
//			oldState = state;
		}
		
		//stopping wrapper class stops reader by extension
		reader.fullStop = true;
		
		long now = System.currentTimeMillis();
		while(readerThread.isAlive() && System.currentTimeMillis()-now > 10000) {
			logger.logcat("run: readerThread is alive", "d");
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}
		}
	}
	
	public int getLateByte() {
		return byteReceivedAfterCancellation;
	}
	
	/**
	 * States the ReadWrapper can be in.
	 */
	public enum State {
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
		
		/**If it failed completely. Most likely caused by IOException.**/
		FAIL
	}

	/**
	 * Class doing the blocking IO.
	 */
	class Reader implements Runnable {
		private ReadWrapper wrapper;
		/**Stops the thread (when the reading is eventually done, not very responsive)**/
		public volatile boolean fullStop;
		
		//vars for work control
		private boolean startWork;
		private boolean resultReady;
		private boolean ready;
		private boolean reading;
		
		//vars for buffered reading
		private byte[] buffer;
		private int readOffset;
		private int maxReadLength;
		private int bytesRead;
		
		//hold the single byte return
		private int singleResult;
		
		public Reader(ReadWrapper wrapper) {
			this.wrapper = wrapper;
		}
		
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
			//stop reader thread from waiting
			notifyAll();
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
			//stop reader thread from waiting
			notifyAll();
		}
		
		/**Read several bytes**/
		private void readToBuffer() {
			try {
//				startWork = false;
				//TODO: change to verbose when buffered reading fixed
				logger.logcat("readToBuffer: Read to buffer in progress", "d");
				bytesRead = input.read(buffer, readOffset, maxReadLength);
				resultReady = true;
			} catch (IOException e) {
				setFailureFlags();
				logger.logcat("readToBuffer: IOException on attempt to read " + 
						maxReadLength + " byte(s). Message: " + e.getMessage(), "v");
			}
		}
		
		
		/**Read a single byte**/
		private void read() {
			try {
//				startWork = false;
//				logger.logcat("read: Single read in progress", "v");
				singleResult = input.read();
				
				String out = "Reader received byte: " + singleResult;
//				logger.printToConsole(out);
//				logger.logcat("read: " + out, "v");
				
				resultReady = true;
			} catch (IOException e) {
				setFailureFlags();
				logger.logcat("read: IOException on attempt to read single byte." +
						"Message: " + e.getMessage(), "v");
			}
		}
		
		private void setFailureFlags() {
			resultReady = false;
//			startWork = false;
			ready = false;
			reading = false;
			//tell wrapper something happened
			synchronized(wrapper) {
				wrapper.notifyAll();
			}
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
			return (!resultReady && !ready && !startWork && !reading);
		}
		
		@Override
		public void run() {
			reading = false;
			fullStop = false;
			resultReady = false;
			ready = true;
			logger.logcat("run: ReadWrapper reader thread started", "v");
			while (!fullStop) {
			
				if (ready || resultReady) {
					//own monitor before waiting:
					synchronized(this) {
						try {
							wait(10);
						} catch (InterruptedException e) {
							//only log message if reader is supposed to read
							if (!ready && !resultReady) {
								logger.logcat("run: Reader woken up to read." + 
										" Message: " +e.getMessage(), "d");
							}
						}

					}
				} else if (reading) {
					//this should never occur
					throw new IllegalStateException("Threading issue? Reading true in run");
				} else if (startWork) {
					startWork = false;
					//check if buffered or single reading should be used
					reading = true;
					if (buffer == null) {
						read();
					} else {
						readToBuffer();
					}
					reading = false;
					//tell wrapper to wake up
					synchronized(wrapper) {
						wrapper.notifyAll();
					}
				} else {
					//!startWork, !reading, !ready, !resultReady
					
					//own monitor before waiting:
					synchronized(this) {
						try {
							wait(10);
						} catch (InterruptedException e) {
							//only log message if reader is supposed to read
							if (ready && !resultReady) {
								logger.logcat("run: Reader woken up after failure." + 
										" Message: " +e.getMessage(), "d");
							}
						}

					}
				}
			}
		}
		
	}
}
