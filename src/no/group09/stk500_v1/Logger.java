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
	 * Write a debug message to the adb
	 */
	public void debugTag(String msg);
}
