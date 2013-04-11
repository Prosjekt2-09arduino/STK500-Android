package no.group09.stk500_v1;

public interface Logger {
	/**
	 * Display a message for a brief time
	 */
	public void makeToast(String msg);

	/**
	 * Append text to the console
	 */
	public void printToConsole(String msg);
	
	/**
	 * Used to print messages to Logcat
	 * 
	 * @param msg A string with the message you want to print
	 * @param level A string with the level of the message you want to print. This
	 * string must match one of the following: <br>
	 * v = verbose <br>
	 * d = debug <br>
	 * i = info <br>
	 * w = warning <br>
	 * e = error
	 */
	public void logcat(String msg, String level);
}
