package no.group09.stk500_v1;
import java.io.*;
import java.util.EnumMap;
import java.util.concurrent.TimeoutException;

public class Reader implements Runnable, IReader {
	private InputStream in;
	private BufferedInputStream bis;
	private Logger logger;
	private volatile Exception lastException;
	private volatile IReaderState currentState;
	/**State to switch to**/
	private volatile EReaderState switchTo;
	private volatile boolean doCompleteStop;
	private EnumMap<EReaderState, IReaderState> states;
	
	private int result;

	

	public Reader(InputStream input, Logger logger) {
		logger.logcat("Reader constructor: Initializing...", "i");
		if (input == null || logger == null) {
			throw new IllegalArgumentException("Reader.constructor: null as argument(s)");
		}
		in = input;
		//single largest expected return is 258
		bis = new BufferedInputStream(in, 1024);
		this.logger = logger; 

		//instance the states
		states = new EnumMap<EReaderState, IReaderState>(EReaderState.class);
		IReaderState state;
		for (EReaderState eState : EReaderState.values()) {
			switch (eState) {
			case STOPPED : {
				state = new StoppedState(this, eState);
				break;
			}
			case STARTING : {
				state = new StoppedState(this, eState);
				break;
			}
			case WAITING : {
				state = new WaitingState(this, eState);
				break;
			}
			case READING : {
				state = new ReadingState(this, eState);
				break;
			}
			case RESULT_READY : {
				state = new ResultReadyState(this, eState);
				break;
			}
			case TIMEOUT_OCCURRED : {
				state = new TimeoutOccurredState(this, eState);
				break;
			}
			case FAIL : {
				state = new FailureState(this, eState);
				break;
			}
			case STOPPING : {
				state = new ResultReadyState(this, eState);
				break;
			}
			default : {
				throw new IllegalStateException("Reader constructor: Unknown state:" +
						eState);
			}
			}
			
			states.put(eState, state);
		}
		currentState = states.get(EReaderState.STOPPED);
		switchTo = EReaderState.STOPPED;
		logger.logcat("Reader constructor: Done", "i");
	}

	private void switchState(EReaderState eState) {
		IReaderState state = states.get(eState);
		logger.logcat("Reader.switchState: Switched to " + state.getEnum(), "d");
		currentState = state;
		state.activate();
	}
	
	@Override
	public void forget() {
		((IReader) currentState).forget();
	}

	@Override
	public EReaderState getState() {
		return currentState.getEnum();
	}

	@Override
	public int getResult() {
		return ((IReader)currentState).getResult();
	}

	@Override
	public int read(TimeoutValues timeout) throws TimeoutException, IOException {
		return ((IReader)currentState).read(timeout);
	}

	@Override
	public void stop() {
		((IReader)currentState).stop();
	}

	@Override
	public void start() {
		((IReader)currentState).start();
	}

	@Override
	public void run() {
		while (!doCompleteStop) {
			currentState.execute();
		}
		logger.logcat("Reader.run: Fully stopped (needs new Thread to restart)", "i");
	}

	/**
	 * Convenience class to limit required code in the actual states
	 */
	abstract class BaseState implements IReaderState, IReader {
		private EReaderState eState;
		protected volatile boolean active;
		protected Reader reader;

		public BaseState(Reader reader, EReaderState eState) {
			this.eState = eState;
			active = true;
			this.reader = reader;
		}

		@Override
		public void execute() {
			if (eState != switchTo) {
				switchState(switchTo);
				return;
			}
			if (!active) {
				synchronized(this) {
					try {
						wait();
					} catch (InterruptedException e)
					{
						active = true;
					}
				}
			}
		}

		/**
		 * Set flag to switch to the selected state. This will wake the current state
		 * if it was sleeping.
		 * If the state is performing blocking operations, the switch may not happen
		 * immediately.
		 * @param state The enumeration associated with the state to switch to.
		 */
		protected synchronized void triggerSwitch(EReaderState state) {
			switchTo = state;
			notifyAll();
		}

		@Override
		public boolean isReadingAllowed() {
			return false;
		}

		@Override
		public EReaderState getState() {
			return this.getEnum();
		}

		@Override
		public int read(TimeoutValues timeout) throws TimeoutException, IOException {
			return RESULT_NOT_DONE;
		}

		@Override
		public int getResult() {
			return RESULT_NOT_DONE;
		}

		@Override
		public EReaderState getEnum() {
			return eState;
		}
		
		@Override
		public void forget() {
			throw new IllegalStateException(String.format("%s.forget: Only call when " +
					"timed out or waiting", eState));
		}
	}

	class StoppedState extends BaseState {

		public StoppedState(Reader reader, EReaderState eState) {
			super(reader, eState);
			active = false;
		}

		@Override
		public void execute() {
			super.execute();
			active = false;
		}

		public void activate() {
			logger.logcat("StoppedState.activate: The reader has stopped", "i");
		}

		@Override
		public void stop() {
			logger.logcat("StoppedState.stop: Already stopped", "i");
		}

		@Override
		public void start() {
			triggerSwitch(EReaderState.STARTING);
		}

	}

	class StartingState extends BaseState {

		public StartingState(Reader reader, EReaderState eState) {
			super(reader, eState);
		}

		@Override
		public void activate() {
			logger.logcat("StartingState.activate: Starting...", "i");
			//TODO: Consider resetting fields
		}
		
		@Override
		public void execute() {
			super.execute();
			triggerSwitch(EReaderState.WAITING);
		}

		@Override
		public void stop() {
			throw new IllegalStateException("Wait until it's running before attempting" +
					" to stop");
		}

		@Override
		public void start() {
			logger.logcat("StartingState.start: Already starting...", "i");
		}

	}

	class WaitingState extends BaseState {

		public WaitingState(Reader reader, EReaderState eState) {
			super(reader, eState);
		}

		@Override
		public void activate() {
			logger.logcat("WaitingState.activate: Ready to work", "d");
			lastException = null;
		}

		@Override
		public void stop() {
			triggerSwitch(EReaderState.STOPPING);
		}

		@Override
		public void start() {
			logger.logcat("WaitingState.start: Already running...", "i");
		}
		
		@Override
		public void forget() {
			int toSkip;
			try {
				toSkip = bis.available();
				logger.logcat("WaitingState.forget: Attempts to skip " + toSkip +
						" bytes...", "d");
				long skipped = bis.skip(toSkip);
				logger.logcat("WaitingState.forget: Skipped " + skipped + " bytes", "d");
			} catch (IOException e) {
				logger.logcat("WaitingState.forget: " + e.getMessage(), "i");
				lastException = e;
				triggerSwitch(EReaderState.FAIL);
			}
		}
		
		@Override
		public boolean isReadingAllowed() {
			return true; //TODO: Consider if checks are needed
		}
		
		@Override
		public int read(TimeoutValues timeout) throws TimeoutException, IOException {
			triggerSwitch(EReaderState.READING);
			while (true) {
				EReaderState s = currentState.getEnum();
				IReader state = (IReader)currentState;
				switch (s) {
				case RESULT_READY : {
					return state.getResult();
				}
				case TIMEOUT_OCCURRED : {
					throw new TimeoutException("Reader.read: Reading timed out!");
				}
				case FAIL : {
					int res = state.getResult();
					if (res == RESULT_END_OF_STREAM) {
						return res;
					} else {
						if (lastException != null &&
								lastException instanceof IOException) {
							throw ((IOException)lastException);
						}
					}
				}
				case READING : {break;}
				case STOPPED : {} //fall through to stopping
				case STOPPING : {
					logger.logcat("Reader.read: Terminated by request while reading!",
							"w");
					return RESULT_NOT_DONE;
				}
				default: {
					throw new IllegalArgumentException("Unexpected state " + s);
				}
				}
				try {
					Thread.sleep(1);
				} catch (InterruptedException e) {}
			}
		}
	}
	
	class ReadingState extends BaseState {
		private long readInitiated;

		public ReadingState(Reader reader, EReaderState eState) {
			super(reader, eState);
			readInitiated = -1;
		}

		@Override
		public void activate() {
			logger.logcat("ReadingState.activate: Reading started...", "d");
			synchronized(reader) {
				result = RESULT_NOT_DONE;
			}
			readInitiated = System.currentTimeMillis();
		}
		
		@Override
		public void execute() {
			super.execute();
			try {
				long now = System.currentTimeMillis();
				if (now - readInitiated > TimeoutValues.DEFAULT.getTimeout()) {
					triggerSwitch(EReaderState.TIMEOUT_OCCURRED);
				}
				//check if there are bytes in the buffer
				else if (bis.available() > 0) {
					int b = bis.read();
					//end of stream occurred, further operations will trigger IOException
					if (b == RESULT_END_OF_STREAM) {
						logger.logcat("ReadingState.execute: EndOfStream", "w");
						synchronized(reader) {
							result = b;
						}
						triggerSwitch(EReaderState.FAIL);
					} else {
						//All good
						synchronized(reader) {
							result = b;
						}
						triggerSwitch(EReaderState.RESULT_READY);
					}
				}
			} catch (IOException e) {
				logger.logcat("ReadingState.execute: " + e.getMessage(), "e");
				lastException = e;
				triggerSwitch(EReaderState.FAIL);
			}
		}

		@Override
		public void stop() {
			logger.logcat("ReadingState.stop: Stopping, this might take some time", "i");
			triggerSwitch(EReaderState.STOPPING);
		}

		@Override
		public void start() {
			logger.logcat("ReadingState.start: Already running...", "i");
		}
		
	}
	
	class ResultReadyState extends BaseState {

		public ResultReadyState(Reader reader, EReaderState eState) {
			super(reader, eState);
		}
		
		@Override
		public void execute() {
			super.execute();
			active = false;
		}

		@Override
		public void activate() {
			logger.logcat("ResultReadyState.activate: result arrived!", "d");
		}

		@Override
		public int getResult() {
			synchronized(reader) {
				int res = result;
				result = RESULT_NOT_DONE;
				triggerSwitch(EReaderState.WAITING);
				return res;
			}
		}
		
		@Override
		public void stop() {
			triggerSwitch(EReaderState.STOPPING);
		}

		@Override
		public void start() {
			logger.logcat("" + this.getClass() + ".start: Already running...", "i");
		}
		
	}
	
	class TimeoutOccurredState extends BaseState {
		private volatile boolean readInProgress;
		private volatile boolean forgetInProgress;
		private volatile boolean receivedSomething;

		public TimeoutOccurredState(Reader reader, EReaderState eState) {
			super(reader, eState);
		}

		@Override
		public void execute() {
			super.execute();
			int skippableBytes = 0;
			//only run while ready
			if (!forgetInProgress && !readInProgress && !receivedSomething) {
				try {
					//see estimate of skippable bytes (should be number of buffered and 
					//those in the socket receiver buffer)
					skippableBytes = bis.available();
					logger.logcat(this.getClass() + ".execute: " + skippableBytes +
							" possible to skip.", "i");
					if (skippableBytes > 0) {
						receivedSomething = true;
					}
				} catch (IOException e) {
					lastException = e;
					triggerSwitch(EReaderState.FAIL);
				}
			}
		}

		@Override
		public boolean isReadingAllowed() {
			//return (!readInProgress && !forgetInProgress);
			return false;
		}

		@Override
		public int read(TimeoutValues timeout) throws TimeoutException, IOException {
			if (!isReadingAllowed()) {
				throw new IllegalStateException("Reading not allowed while reading or " +
						"forgetting!");
			}
			return super.read(timeout);
		}

		@Override
		public int getResult() {
			if (receivedSomething) {
				triggerSwitch(EReaderState.WAITING);
				return TIMEOUT_BYTE_RECEIVED;
			}
			return RESULT_NOT_DONE;
		}

		@Override
		public void forget() {
			if (readInProgress || forgetInProgress) {
				throw new IllegalStateException("Can't start forget process while " +
						"already reading or forgetting");
			}
			forgetInProgress = true;
			int toSkip;
			try {
				toSkip = bis.available();
				logger.logcat("TimeoutOccurred.forget: Attempts to skip " + toSkip +
						" bytes...", "d");
				long skipped = bis.skip(toSkip);
				logger.logcat("TimeoutOccurred.forget: Skipped " + skipped + " bytes",
						"d");
			} catch (IOException e) {
				logger.logcat("TimeoutOccurred.forget: " + e.getMessage(), "i");
				lastException = e;
				triggerSwitch(EReaderState.FAIL);
			}
			finally {
				forgetInProgress = false;
			}
		}


		@Override
		public void activate() {
			readInProgress = false;
			forgetInProgress = false;
			receivedSomething = false;
			int avail = 0;
			try {
				avail = bis.available();
				if (avail > 0) {
					logger.logcat(this.getClass() + ".activate: " + avail + "unread " +
							"bytes already in the buffer!", "w");
					//ignore the byte(s)
					forget();
				}
			} catch (IOException e) {
				lastException = e;
				triggerSwitch(EReaderState.FAIL);
			}
		}

		@Override
		public void stop() {
			logger.logcat("TimeoutOccurredState.stop: Stopping... Might take a while " +
					"if blocking operations are in progress", "i");
			triggerSwitch(EReaderState.STOPPING);
		}

		@Override
		public void start() {
			logger.logcat("TimeoutOccurredState.start: Already running", "i");
		}
		
	}
	
	class FailureState extends BaseState {

		public FailureState(Reader reader, EReaderState eState) {
			super(reader, eState);
		}

		@Override
		public void activate() {
			logger.logcat(this.getClass() + ".activate: Reader failed!", "e");
		}
		
		@Override
		public void execute() {
			super.execute();
			active = false;
		}

		@Override
		public int getResult() {
			if (lastException != null) {
				if (lastException instanceof IOException) {
					//if an IOException was encountered outside of read().
					throw new RuntimeException(lastException);
				} else {
					throw new RuntimeException("Unexpected exception of type " +
							lastException.getClass(), lastException);
				}
			} else {
				throw new RuntimeException("An Unknown problem occured!");
			}
		}

		@Override
		public void stop() {
			triggerSwitch(EReaderState.STOPPING);
			logger.logcat(this.getClass() + ".stop: Stopping...", "i");
		}

		@Override
		public void start() {
			logger.logcat(this.getClass() + ".start: Already running, though currently" +
					" in a failure state.", "i");
		}
		
	}
	
	class StoppingState extends BaseState {

		public StoppingState(Reader reader, EReaderState eState) {
			super(reader, eState);
		}

		@Override
		public void activate() {
			logger.logcat("StoppingState.activate: Shutdown in progress...", "i");
		}

		@Override
		public void execute() {
			super.execute();
			triggerSwitch(EReaderState.STOPPED);
		}
		
		@Override
		public void stop() {
			logger.logcat("StoppingState.stop: Already stopping...", "i");
		}

		@Override
		public void start() {
			throw new IllegalStateException("Can't start during shutdown!");
		}
		
	}
}
