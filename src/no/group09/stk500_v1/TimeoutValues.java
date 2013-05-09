package no.group09.stk500_v1;

/**
 * Standard timeout values for how long to wait for reading results.
 */
public enum TimeoutValues{
	DEFAULT(450),
	CONNECT(450),
	READ(800),
	SHORT_READ(800),
	RESTART(800),
	WRITE(75);

	private final long timeout;

	private TimeoutValues(long t)
	{
		timeout = t;
	}

	/**
	 * Get the milliseconds assigned to the timeout
	 * @return timeout as a long in milliseconds
	 */
	public long getTimeout() {
		return timeout;
	}
}