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
				state = new StartingState(this, eState);
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
				state = new StoppingState(this, eState);
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
		((BaseState)currentState).activated = false;
		currentState = state;
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
	
	public void requestCompleteStop() {
		if (currentState.getEnum() == EReaderState.STOPPED) {
			logger.logcat("requestCompleteStop: setting doCompleteStop to true", "d");
			doCompleteStop = true;
		}
		logger.logcat("requestCompleteStop: can only shut down completely while " +
				"stopped. Current state: " + currentState.getEnum(), "d");
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
		protected volatile boolean activated;

		public BaseState(Reader reader, EReaderState eState) {
			this.eState = eState;
			this.reader = reader;
			active = true;
			activated = false;
		}

		@Override
		public void execute() {
			if (!activated) {
				activate();
			}
			if (eState != switchTo) {
				logger.logcat(getEnum() + "(Base).execute: State switch required",
						"v");
				switchState(switchTo);
				return;
			}
			if (!active) {
				synchronized(this) {
					try {
						logger.logcat(getEnum() + "(Base).execute: waiting...", "v");
						wait(1000);
						active = true;
					} catch (InterruptedException e)
					{
						logger.logcat(getEnum() + "(Base).execute: woken up!", "v");
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
			logger.logcat(getEnum()+ " triggerSwitch: notifying all", "d");
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
			activated = true;
		}

		@Override
		public void stop() {
			logger.logcat("StoppedState.stop: Already stopped", "i");
		}

		@Override
		public void start() {
			logger.logcat(getEnum() + " start: Starting...", "d");
			triggerSwitch(EReaderState.STARTING);
		}

	}

	class StartingState extends BaseState {

		public StartingState(Reader reader, EReaderState eState) {
			super(reader, eState);
			active = true;
		}

		@Override
		public void activate() {
			logger.logcat("StartingState.activate: Starting...", "i");
			activated = true;
			active = true;
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
			active = true;
			activated = true;
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
			logger.logcat(getEnum() + " read: entered read method in Reader.java", "i");
			triggerSwitch(EReaderState.READING);
			while (true) {
				EReaderState s = currentState.getEnum();
				IReader state = (IReader)currentState;
				switch (s) {
				case RESULT_READY : {
					int res = state.getResult();
					logger.logcat(getEnum() + ".read: result: " + 
							Hex.oneByteToHex((byte) res), "i");
					return res;
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
				case READING : {} //intentional fall through
				case WAITING : {
					try {
						Thread.sleep(1);
					} catch (InterruptedException e) {}
					break;
				}
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
				//				try {
				//					Thread.sleep(1);
				//				} catch (InterruptedException e) {}
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
			active = true;
			activated = true;
		}

		@Override
		public void execute() {
			super.execute();
			try {
				int bytesInBuffer = -1;
				long now = System.currentTimeMillis();
				if (now - readInitiated > TimeoutValues.DEFAULT.getTimeout()) {
					triggerSwitch(EReaderState.TIMEOUT_OCCURRED);
					return;
				}
				
				//check if there are bytes in the buffer
				else {
					bytesInBuffer = (bis.available());
					if (bytesInBuffer > 0) {
						logger.logcat(getEnum() + "bytes in buffer: " + bytesInBuffer, "d");
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

		private boolean resultFetched = false;
		
		public ResultReadyState(Reader reader, EReaderState eState) {
			super(reader, eState);
		}

		@Override
		public void execute() {
			super.execute();
			if (resultFetched) {
				triggerSwitch(EReaderState.WAITING);
			}
			active = false;
		}

		@Override
		public void activate() {
			resultFetched = false;
			logger.logcat("ResultReadyState.activate: result arrived!", "d");
			activated = true;
		}

		@Override
		public int getResult() {
			int res = result;
			logger.logcat(getEnum() + " getResult: " + res, "d");
			synchronized(reader) {
				result = RESULT_NOT_DONE;
			}
			triggerSwitch(EReaderState.WAITING);
			synchronized (this) {
				resultFetched = true;
			}
			active = true;
			return res;
		}

		@Override
		public void stop() {
			triggerSwitch(EReaderState.STOPPING);
		}

		@Override
		public void start() {
			logger.logcat("" + getEnum() + ".start: Already running...", "i");
		}

	}

	class TimeoutOccurredState extends BaseState {
		private volatile boolean readInProgress;
		private volatile boolean forgetInProgress;
		private volatile boolean receivedSomething;
		private int toSkip = -1;

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
					if (skippableBytes != toSkip) {
						logger.logcat(getEnum() + ".execute: " + skippableBytes +
								" possible to skip.", "i");
						toSkip = skippableBytes;
					}
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
			toSkip = -1;
			
			try {
				avail = bis.available();
				if (avail > 0) {
					logger.logcat(getEnum() + ".activate: " + avail + "unread " +
							"bytes already in the buffer!", "w");
					//ignore the byte(s)
					forget();
				}
			} catch (IOException e) {
				lastException = e;
				triggerSwitch(EReaderState.FAIL);
			}
			active = true;
			activated = true;
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
			logger.logcat(getEnum() + ".activate: Reader failed!", "e");
			activated = true;
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
			logger.logcat(getEnum() + ".stop: Stopping...", "i");
		}

		@Override
		public void start() {
			logger.logcat(getEnum() + ".start: Already running, though currently" +
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
			active = true;
			activated = true;
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
