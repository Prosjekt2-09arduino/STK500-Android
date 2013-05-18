package no.group09.stk500_v1;

/**
 *  Copyright 2013 UbiCollab
 *  
 *  This file is part of STK500ForJava.
 *
 *	STK500ForJava is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU Lesser General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	STK500ForJava is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public License
 *	along with STK500ForJava.  If not, see <http://www.gnu.org/licenses/>.
 */

/**
 * Interface used to provide log output and short user notifications.
 */
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
