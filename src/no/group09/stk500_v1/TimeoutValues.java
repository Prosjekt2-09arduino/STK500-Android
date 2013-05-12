package no.group09.stk500_v1;

/**
 *  Copyright 2013 UbiCollab
 *  
 *  This file is part of STK500ForJava.
 *
 *	STK500ForJava is free software: you can redistribute it and/or modify
 *	it under the terms of the GNU General Public License as published by
 *	the Free Software Foundation, either version 3 of the License, or
 *	(at your option) any later version.
 *
 *	STK500ForJava is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *	GNU General Public License for more details.
 *
 *	You should have received a copy of the GNU General Public License
 *	along with STK500ForJava.  If not, see <http://www.gnu.org/licenses/>.
 */

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