package no.group09.stk500_v1;

/**
 * This interface describes what operations can be performed on the states themselves.
 * See {@link EReaderState} for possible states.
 */
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
	
	/**
	 * Checks if the state has been initialized.
	 * @return true if the state is the current state and it has run its
	 * initialization code.
	 * @return false if the state is not the current state or it has yet to be
	 * initialized.
	 */
	public boolean hasStateBeenActivated();
}
