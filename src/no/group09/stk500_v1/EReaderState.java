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
 * This enum contains every possible state an {@link IReader} can be in. 
 */
public enum EReaderState {
	/**
	 * Not yet started. Eventually changes to WAITING**/
	STARTING,
	
	/**
	 * Ready for requests. Go to READING on read(), or STOPPING on stop()
	 */
	WAITING,
	
	/**
	 * IO in progress goes to RESULT_READY once reading is completed, or to
	 * TIMEOUT_OCCURRED if nothing arrives in time.
	 */
	READING,
	
	/**
	 * Result ready - goes to WAITING after the result has been retrieved using
	 * getResult().
	 */
	RESULT_READY,
	
	/**
	 * A timeout occurred while reading. Call forget() on this state after spamming
	 * requests to regain communications to ignore the eventual responses. Any response
	 * at all will set the TIMEOUT_BYTE_RECEIVED to be returned by getResult().
	 * When that byte is returned, the state will switch back to WAITING.
	 */
	TIMEOUT_OCCURRED,
	
	/**
	 * Termination requested, just let the state finish shutting down before doing
	 * anything.
	 */
	STOPPING,
	
	/**
	 * Fully stopped. An IReader can be started or completely shut down while in this
	 * state.
	 */
	STOPPED,
	
	/**
	 * If operations failed completely. Most likely caused by IOException.
	 * Returns to WAITING after getResult() is called.
	 **/
	FAIL
}
