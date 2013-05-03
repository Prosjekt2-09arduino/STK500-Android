package no.group09.stk500_v1;
import java.io.*;
import java.util.EnumMap;
import java.util.HashMap;

public class Reader implements Runnable, IReader {
	private InputStream in;
	private Logger logger;
	private volatile IReaderState currentState;
	private EnumMap<EReaderState, IReaderState> states;
	
	public Reader(InputStream input, Logger logger) {
		logger.logcat("Reader constructor: Initializing...", "i");
		in = input;
		this.logger = logger; 
		states = new EnumMap<EReaderState, IReaderState>(EReaderState.class);
		//instance the states
		for (EReaderState eState : EReaderState.values()) {
			switch (eState) {
			case STOPPED : {
				states.put((EReaderState)eState, new StateStopped(eState));
			}
			default : {
				throw new IllegalStateException("Reader constructor: Unknown state:" + eState);
			}
			}
		}
		currentState = states.get(EReaderState.STOPPED);
		logger.logcat("Reader constructor: Done", "i");
	}

	@Override
	public IReaderState getState() {
		return currentState;
	}

	@Override
	public int getResult() {
		return ((IReader)currentState).getResult();
	}

	@Override
	public int read() {
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
		while (true) {
			currentState.execute();
		}
	}

	abstract class BaseState implements IReaderState, IReader {
		private EReaderState eState;
		
		public BaseState(EReaderState eState) {
			this.eState = eState;
		}
		
		@Override
		public void execute() {
			synchronized(this) {
				try {
					wait();
				} catch (InterruptedException e) {}
			}
		}

		@Override
		public IReaderState getState() {
			return this;
		}
		
		@Override
		public int read() {
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
	}
	
	class StateStopped extends BaseState {

		public StateStopped(EReaderState eState) {
			super(eState);
		}

		@Override
		public void stop() {
			logger.logcat("StateStopped.stop: Already stopped", "i");
		}

		@Override
		public void start() {
			// TODO Auto-generated method stub
			
		}
		
	}
}
