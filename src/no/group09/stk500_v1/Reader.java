package no.group09.stk500_v1;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.*;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Observer;
import java.util.Queue;
import java.util.Stack;
import java.util.concurrent.TimeoutException;

public class Reader implements Runnable, IReader, Observable {
	private InputStream in;
	private BufferedInputStream bis;
	private Logger logger;
	private volatile Exception lastException;
	private volatile IReaderState currentState;
	/**State to switch to**/
	private volatile EReaderState switchTo;
	private volatile boolean doCompleteStop;
	private EnumMap<EReaderState, IReaderState> states;

	private List<PropertyChangeListener> listeners;
	private Queue<IReaderState> eventQueue;
	private int result;
	private volatile byte[] resultArray;
	private volatile int offset;



	public Reader(InputStream input, Logger logger) {
		logger.logcat("Reader constructor: Initializing...", "i");
		if (input == null || logger == null) {
			throw new IllegalArgumentException("Reader.constructor: null as argument(s)");
		}

		listeners = new ArrayList<PropertyChangeListener>();

		eventQueue = new LinkedList<IReaderState>();

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
			listeners.add((PropertyChangeListener) state);
		}
		currentState = states.get(EReaderState.STOPPED);
		switchTo = EReaderState.STOPPED;
		logger.logcat("Reader constructor: Done", "i");
	}

	private void switchState(EReaderState newState) {
		switchTo = newState;
		addToEventQueue(states.get(newState));
	}

	private synchronized void addToEventQueue(IReaderState newState) {
		//TODO: Check if queue is full
		logger.logcat("addToEventQueue: adding newState " + newState.getEnum() + " to queue", "d");
		eventQueue.add(newState);
		logger.logcat("addToEventQueue: states in queue after adding: " + eventQueue.size(), "d");
		synchronized (newState) {
			logger.logcat("addToEventQueue: notifying all", "d");
			notifyAll();
		}
	}

	private synchronized IReaderState pollEventQueue() {
		IReaderState state = eventQueue.poll();
		if (state != null) {
			logger.logcat("pollEventQueue: polling event " + state.getEnum() + " from queue", "d");
		}
		return state;
	}

	private synchronized void resetQueue() {
		eventQueue = new LinkedList<IReaderState>();
	}

	//	private void switchState(EReaderState eState) {
	//		IReaderState state = states.get(eState);
	//		logger.logcat("Reader.switchState: Scheduled switch to " + state.getEnum(), "d");
	//		((BaseState)currentState).activated = false;
	//		currentState = state;
	//	}

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
	public int read(byte[] saveTo, int offset, TimeoutValues timeout)
			throws TimeoutException, IOException {
		return ((IReader)currentState).read(saveTo, offset, timeout);
	}

	@Override
	public boolean stop() {
		return ((IReader)currentState).stop();
	}

	@Override
	public boolean start() {
		return ((IReader)currentState).start();
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

	@Override
	public synchronized void addStateChangedListener(PropertyChangeListener listener) {
		if (listeners.contains(listener)) {
			listeners.add(listener);
		}
	}

	@Override
	public synchronized void removeStateChangedListener(PropertyChangeListener listener) {
		listeners.remove(listener);
	}

	@Override
	public void fireStateChanged() {

		PropertyChangeEvent event = new PropertyChangeEvent(this, "state", currentState, states.get(switchTo));

		for (PropertyChangeListener listener : listeners) {
			listener.propertyChange(event);
		}
	}

	/**
	 * Sets the current state of the system. Should be called every time the state is changed.
	 * 
	 * @param currentState the new current state
	 */
	public void setCurrentState(IReaderState currentState) {
		this.currentState = currentState;
	}

	/**
	 * Convenience class to limit required code in the actual states
	 */
	abstract class BaseState implements IReaderState, IReader, PropertyChangeListener {

		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			IReaderState oldState = (IReaderState) evt.getOldValue();
			IReaderState newState = (IReaderState) evt.getNewValue();
		}

		private EReaderState eState;
		protected volatile boolean active;
		protected Reader reader;
		protected volatile boolean activated;
		protected boolean abort;

		public BaseState(Reader reader, EReaderState eState) {
			this.eState = eState;
			this.reader = reader;
			active = true;
			activated = false;
			abort = false;
		}
		
		@Override
		public boolean wasCurrentStateActivated() {
			return hasStateBeenActivated();
		}
		
		@Override
		public boolean hasStateBeenActivated() {
			return (currentState == this && activated);
		}

		@Override
		public void execute() {
			if (!activated && !abort) {
				activate();
			}
			else {
				//Check if it should switch state
				IReaderState nextState = pollEventQueue();
				//Change state
				if (nextState != null) {
					currentState = nextState;
					((BaseState) nextState).abort = false;
					abort = true;
					activated = false;
					return;
				}
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
			//			switchTo = state;
			//			logger.logcat(getEnum()+ " triggerSwitch: notifying all.", "d");
			//			notifyAll();

			//HACK.
			switchState(state);
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
		public int read(byte[] saveTo, int offset, TimeoutValues timeout) throws TimeoutException, IOException {
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
			if (abort) return;
			active = false;
		}

		public void activate() {
			logger.logcat("StoppedState.activate: The reader has stopped", "i");
			bis = null;
			activated = true;
			abort = false;
		}

		@Override
		public boolean stop() {
			logger.logcat("StoppedState.stop: Already stopped", "i");
			return true;
		}

		@Override
		public boolean start() {
			logger.logcat(getEnum() + " start: Starting...", "d");
			triggerSwitch(EReaderState.STARTING);
			return true;
		}

	}

	class StartingState extends BaseState {

		public StartingState(Reader reader, EReaderState eState) {
			super(reader, eState);
			if (abort) return;
			active = true;
		}

		@Override
		public void activate() {
			logger.logcat("StartingState.activate: Starting...", "i");
			activated = true;
			active = true;
			abort = false;
			bis = new BufferedInputStream(in);
			switchState(EReaderState.WAITING);
			//TODO: Consider resetting fields
		}

		@Override
		public void execute() {
			super.execute();
			if (abort) return;
		}

		@Override
		public boolean stop() {
			logger.logcat("StartingState: Wait until it's running before attempting" +
					" to stop", "e");
			return false;
		}

		@Override
		public boolean start() {
			logger.logcat("StartingState.start: Already starting...", "i");
			return true;
		}

	}

	class WaitingState extends BaseState {

		public WaitingState(Reader reader, EReaderState eState) {
			super(reader, eState);
		}

		@Override
		public void activate() {
			logger.logcat("WaitingState.activate: Ready to work", "d");
			resultArray = null;
			lastException = null;
			active = true;
			activated = true;
			abort = false;
		}

		@Override
		public boolean stop() {
			triggerSwitch(EReaderState.STOPPING);
			return true;
		}

		@Override
		public boolean start() {
			logger.logcat("WaitingState.start: Already running...", "i");
			return true;
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
			return read(null, 0, timeout);
		}

		@Override
		public int read(byte[] saveTo, int offset, TimeoutValues timeout) throws TimeoutException, IOException {
			logger.logcat(getEnum() + " read: entered read method in Reader.java", "i");
			resultArray = saveTo;
			triggerSwitch(EReaderState.READING);
			while (true) {
				EReaderState s = currentState.getEnum();
				IReader state = (IReader)currentState;
				switch (s) {
				case RESULT_READY : {
					int res = state.getResult();
					if (resultArray != null) {
						this.reader.offset = offset;
						logger.logcat(getEnum() + ".read: result: " + 
								Hex.oneByteToHex((byte) res), "i");
					} else {
						logger.logcat(getEnum() + ".read: read: " + 
								res + " bytes", "i");
					}
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
		private int minBytesToRead;

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
			minBytesToRead = (resultArray == null) ? 1 : resultArray.length;
			readInitiated = System.currentTimeMillis();
			active = true;
			activated = true;
			abort = false;
		}

		@Override
		public void execute() {
			super.execute();
			if (abort) return;
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
					int b;
					if (bytesInBuffer >= minBytesToRead) {
						logger.logcat(getEnum() + ".execute: bytes in buffer: " + bytesInBuffer, "d");
						if (minBytesToRead > 1) {
							b = bis.read(resultArray, reader.offset, minBytesToRead);
						} else {
							b = bis.read();
						}
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
		public boolean stop() {
			logger.logcat("ReadingState.stop: Stopping, this might take some time", "i");
			triggerSwitch(EReaderState.STOPPING);
			return true;
		}

		@Override
		public boolean start() {
			logger.logcat("ReadingState.start: Already running...", "i");
			return true;
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
			if (abort) return;
			if (resultFetched) {
				logger.logcat("ResultReadyState: execute: should switch state to WAITING.", "i");
				switchState(EReaderState.WAITING);
			}
		}

		@Override
		public void activate() {
			resultFetched = false;
			logger.logcat("ResultReadyState.activate: result arrived!", "d");
			activated = true;
			abort = false;
		}

		@Override
		public int getResult() {
			int res = result;
			if (resultArray == null) {
				logger.logcat(getEnum() + " getResult: " + Hex.oneByteToHex((byte)res), "d");
			} else {
				logger.logcat(getEnum() + " getResult; " + "read " + res + " bytes", "d");
			}
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
		public boolean stop() {
			triggerSwitch(EReaderState.STOPPING);
			return true;
		}

		@Override
		public boolean start() {
			logger.logcat("" + getEnum() + ".start: Already running...", "i");
			return true;
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
			if (abort) return;
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
			abort = false;
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
		public boolean stop() {
			logger.logcat("TimeoutOccurredState.stop: Stopping... Might take a while " +
					"if blocking operations are in progress", "i");
			triggerSwitch(EReaderState.STOPPING);
			return true;
		}

		@Override
		public boolean start() {
			logger.logcat("TimeoutOccurredState.start: Already running", "i");
			return true;
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
			abort = false;
		}

		@Override
		public void execute() {
			super.execute();
			if (abort) return;
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
		public boolean stop() {
			triggerSwitch(EReaderState.STOPPING);
			logger.logcat(getEnum() + ".stop: Stopping...", "i");
			return true;
		}

		@Override
		public boolean start() {
			logger.logcat(getEnum() + ".start: Already running, though currently" +
					" in a failure state.", "i");
			return true;
		}

	}

	class StoppingState extends BaseState {

		public StoppingState(Reader reader, EReaderState eState) {
			super(reader, eState);
		}

		@Override
		public void activate() {
			resetQueue();
			logger.logcat("StoppingState.activate: Shutdown in progress...", "i");
			active = true;
			((BaseState)states.get(EReaderState.STOPPED)).abort = false;
			activated = true;
			abort = false;
		}

		@Override
		public void execute() {
			super.execute();
			if (abort) return;
			switchState(EReaderState.STOPPED);
		}

		@Override
		public boolean stop() {
			logger.logcat("StoppingState.stop: Already stopping...", "i");
			return true;
		}

		@Override
		public boolean start() {
			logger.logcat(getEnum() + ".start: Can't start during shutdown!", "e");
			return false;
		}

	}

	@Override
	public boolean wasCurrentStateActivated() {
		return currentState.hasStateBeenActivated();
	}

	


}
