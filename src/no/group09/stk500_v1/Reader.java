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
	private static final int RESULT_NOT_DONE = -2;
	private static final int RESULT_END_OF_STREAM = -1;
	

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
	public int read() throws TimeoutException, IOException {
		return ((IReader)currentState).read();
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
		public int read() throws TimeoutException, IOException {
			return Integer.MAX_VALUE;
		}

		@Override
		public int getResult() {
			return Integer.MAX_VALUE;
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
			}
		}
		
		@Override
		public boolean isReadingAllowed() {
			return true; //TODO: Consider if checks are needed
		}
		
		@Override
		public int read() throws TimeoutException, IOException {
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
		private boolean reading;

		public TimeoutOccurredState(Reader reader, EReaderState eState) {
			super(reader, eState);
		}

		@Override
		public void execute() {
			// TODO Auto-generated method stub
			super.execute();
		}

		@Override
		public boolean isReadingAllowed() {
			// TODO Auto-generated method stub
			return super.isReadingAllowed();
		}

		@Override
		public int read() throws TimeoutException, IOException {
			// TODO Auto-generated method stub
			return super.read();
		}

		@Override
		public int getResult() {
			// TODO Auto-generated method stub
			return super.getResult();
		}

		@Override
		public void forget() {
			if (reading) {
				return;
			}
			//TODO: Something like in WaitingState
		}


		@Override
		public void activate() {
			reading = false;
		}

		@Override
		public void stop() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void start() {
			// TODO Auto-generated method stub
			
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
