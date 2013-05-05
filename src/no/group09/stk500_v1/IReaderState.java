package no.group09.stk500_v1;

public interface IReaderState {
	/**
	 * Get the enumeration value corresponding to the state
	 */
	public EReaderState getEnum();
	
	/**Call when the state is switched to**/
	public void activate();
	
	/**Call to process the state logic**/
	public void execute();
	
	/**
	 * Checks if reading is currently legal. This includes checking if the state
	 * accepts reading at all, as well as state-specific conditions.
	 * @return true if it's okay to read
	 */
	public boolean isReadingAllowed();
	
}
