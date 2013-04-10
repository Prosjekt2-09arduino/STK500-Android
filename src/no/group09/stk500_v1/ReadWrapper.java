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
	
	byte[] buffer;
	int bytesRead;
	
	int byteResult;
	
	/**Set when reader has something to report**/
	private volatile boolean readerAlert;
	
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
		readerAlert = false;
		cancelRequest = false;
		terminate = false;
		oldState = null;
		resultsSet = false;
		
		strictPolicy = false;
		
		buffer = null;
		byteResult = -2;
		bytesRead = -1;
		
		logger.debugTag("Initializing readWrapper");
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
		logger.debugTag("ReadWrapper registered with ready reader");
	}
	
	/**
	 * Set whether results arriving after cancellation but before new requests should
	 * be accepted. If buffers differ or different operations are run (buffer vs single
	 * byte), this will not be considered at all (result will be discarded).
	 * @param choice true to enable, false to disable
	 */
	public synchronized void setStrictPolicy(boolean choice) {
		logger.debugTag("Set strict policy to " + choice);
		strictPolicy = choice;
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
		logger.debugTag("ReadWrapper thread started");
		while (!terminate) {
			// === begin switch statement ===
			//See State enum or hover over states for explanations
			switch (state) {
			case STARTING : {
				//see if the reader is initialized
				if (reader.isReady()) {
					logger.debugTag("ReadWrapper ready");
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
					logger.debugTag("Wrapper idling...");
				}
				
				//nothing to do for now, pause execution
				synchronized(this) {
					try {
						wait();
					} catch (InterruptedException e) {
						//TODO consider doing something
					}
				}
				break;
			}
			case READING_CONTINUED : {
				if (oldState == State.REQUEST_CANCELLED) {
					//no need to do anything, as the reader was already reading
					logger.debugTag("Resumed reading from a cancelled request");
				}
				//no break, deliberately fall through into READING
			}
			case READING : {
				//if state == READING_CONTINUED, this case will also execute.
				//however, the if block will be skipped
				if (oldState == State.WAITING) {
					resultsSet = false;
					logger.debugTag("Wrapper starting read action");
					//read to buffer or just a single byte
					if (buffer == null) {
						reader.requestRead();
					} else {
						reader.requestRead(buffer, 0, buffer.length);
					}
				}

				//nothing to do for now, pause execution
				synchronized(this) {
					try {
						wait();
					} catch (InterruptedException e) {
						//TODO consider doing something
					}
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
					} else {
						//get number of read bytes. actual bytes stored in buffer
						bytesRead = reader.getReadBytesFromResult();
					}
					logger.debugTag("Result ready: " + byteResult);
					resultsSet = true;
				}

				//nothing to do for now, pause execution
				synchronized(this) {
					try {
						wait();
					} catch (InterruptedException e) {
						//TODO consider doing something
					}
				}
				break;
			}
			case REQUEST_CANCELLED: {
				if (state != oldState) {
					logger.debugTag("Read request cancelled");
				}
				
				if (reader.isDone()) {
					//reader finished before new request arrived
					state = State.REQUEST_CANCELLED_RESULT_READY;
				}

				//nothing to do for now, pause execution
				synchronized(this) {
					try {
						wait();
					} catch (InterruptedException e) {
						//TODO consider doing something
					}
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
					//TODO Consider setting resultSet to true
				}

				//nothing to do for now, pause execution
				synchronized(this) {
					try {
						wait();
					} catch (InterruptedException e) {
						//TODO consider doing something
					}
				}
				break;
			}
			case FAIL : {
				if (oldState != state) {
					logger.debugTag("Wrapper class failed to get a result from the reader");
					//read type won't matter for future requests
					buffer = null;
				}

				//nothing to do for now, pause execution
				synchronized(this) {
					try {
						wait();
					} catch (InterruptedException e) {
						//TODO consider doing something
					}
				}
				break;
			}
			default : {
				throw new IllegalArgumentException("Unhandled state:" + state);
			}
			}
			// === end switch statement ===
			
			//keep track of state transitions
			oldState = state;
		}
		
		//stopping wrapper class stops reader by extension
		reader.fullStop = true;
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
				startWork = false;
				logger.debugTag("Read to buffer in progress");
				bytesRead = input.read(buffer, readOffset, maxReadLength);
				resultReady = true;
			} catch (IOException e) {
				setFailureFlags();
				logger.debugTag("IOException on attempt to read " + maxReadLength +
						" byte(s). Message: " + e.getMessage());
			}
		}
		
		
		/**Read a single byte**/
		private void read() {
			try {
				startWork = false;
				logger.debugTag("Single read in progress");
				singleResult = input.read();
				
				String out = "Reader received byte: " + singleResult;
				logger.printToConsole(out);
				logger.debugTag(out);
				
				resultReady = true;
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
			logger.debugTag("ReadWrapper reader thread started");
			while (!fullStop) {
			
				if (ready || resultReady) {
					//own monitor before waiting:
					synchronized(this) {
						try {
							wait();
						} catch (InterruptedException e) {
							//only log message if reader is supposed to read
							if (!ready && !resultReady) {
								logger.debugTag("Reader woken up to read." + 
										" Message: " +e.getMessage());
							}
						}

					}
				} else if (reading) {
					//this should never occur
					throw new IllegalStateException("Threading issue? Reading true in run");
				} else if (startWork) {
					//check if buffered or single reading should be used
					if (buffer == null) {
						reading = true;
						read();
						reading = false;
					} else {
						reading = true;
						readToBuffer();
						reading = false;
					}
					//tell wrapper to wake up
					synchronized(wrapper) {
						wrapper.notifyAll();
					}
				} else {
					//!startWork, !reading, !ready, !resultReady
				}
			}
		}
		
	}
}
