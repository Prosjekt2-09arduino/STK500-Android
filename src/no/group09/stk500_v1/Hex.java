package no.group09.stk500_v1;

import java.util.ArrayList;

public class Hex {
	private Logger logger;
	
	private ArrayList<ArrayList<Byte>> binList = new ArrayList<ArrayList<Byte>>();
	
	private ArrayList<Byte> dataList = new ArrayList<Byte>();
	
	private byte[] subHex; 
	
	private boolean state = false;
	
	public Hex(byte[] bin, Logger log) {
		this.logger = log;
		this.subHex = bin;
		
		// create a new ArrayList and save state
		state = splitHex();
		
		logger.logcat("Hex file status: " + state, "v");
	}
	
	/**
	 * Return number of data bytes from hex file.
	 * 
	 * @return Number of data bytes.
	 */
	public int getDataSize() {
		return dataList.size();
	}
	
	/**
	 * Return state of hex file.
	 * 
	 * @return True if the hex file is correct.
	 */
	public boolean getChecksumStatus()
	{
		return state;
	}
	
	/**
	 * Return data bytes.
	 * 
	 * @param startByte Where to start loading bytes
	 * @param numberOfBytes Number of bytes to return.
	 * 
	 * @return Array with data bytes, maximum <code>numberOfBytes</code>.
	 */
	public byte[] getHexLine(int startByte, int numberOfBytes)
	{
		try {
			logger.logcat("Hex.getHexLine: startByte: " + startByte +
					", numberOfBytes: " + numberOfBytes, "d");
			return formatHexLine(startByte, numberOfBytes);
		} catch (IndexOutOfBoundsException e) {
			// There was no bytes, return an empty array
			logger.logcat("Hex.getHexLine: startByte is out of bounds! Value was: " +
					startByte + ", max value: " + dataList.size(), "w");
			byte[] temp = new byte[0]; 
			return temp;
		}
	}
	
	/**
	 * Format hex file and return data bytes.
	 * 
	 * @param startByte Where to start loading bytes.
	 * @param numberOfBytes Number of bytes to load.
	 * 
	 * @return Byte array with data bytes.
	 * 
	 * @throws IndexOutOfBoundsException When <code>startByte</code> does not exist.
	 */
	private byte[] formatHexLine(int startByte, int numberOfBytes)
			throws IndexOutOfBoundsException
	{
		try {
			dataList.get(startByte);
		} catch (Exception e) {
			throw new IndexOutOfBoundsException("Index " + startByte + " is out of bounds!");
		}
		
		int dataLength = numberOfBytes;
		
		// Check if it is enough data bytes to read
		if((startByte + numberOfBytes) > dataList.size()) {
			dataLength = dataList.size() - startByte;
			logger.logcat("Hex.formatHexLine: Could not read " + numberOfBytes +
					" bytes, changed to " + dataLength, "i");
		}
		
		// Create a new temporary array
		byte[] tempArray = new byte[dataLength];
		
		// Store data bytes into array
		for(int i=0; i<dataLength; i++) {
			tempArray[i] = dataList.get(startByte+i);
		}
		
		return tempArray;
	}
	
	/**
	 * Split the hex input into an array and check if it's correct.
	 * Each line must start with ':' (colon), byte value 58. The following
	 * values is 1 byte size, 2 byte address, n byte data, 1 byte checksum.
	 * The record is not saved, only used to check what kind of data this is.
	 * This will start with line 0 and parse through the whole file.
	 * 
	 * @return True if the hex file is correct.
	 */
	private boolean splitHex() {
		int b = 0;
		
		// Keep splitting the hex file until there is no more data bytes
		while(b < subHex.length) {
			b = splitHex(b);
			
			// Something went wrong
			if(b < 0) return false;
		}
		
		return true;
	}
	
	/**
	 * Split the hex input into an array and check if it's correct.
	 * Each line must start with ':' (colon), byte value 58. The following
	 * values is 1 byte size, 2 byte address, n byte data, 1 byte checksum.
	 * The record is not saved, only used to check what kind of data this is.
	 * 
	 * @param startOnDataByte Start reading at this byte number. When finished
	 * with this line, starting on the next line.
	 * 
	 * @return Index to last checked byte.
	 */
	private int splitHex(int startOnDataByte) {
		int dataLength = 0;
		
		//The minimum length of a line is 6, including the start byte ':'
		if((subHex.length + startOnDataByte)<6) {
			logger.logcat("splitHex(): The minimum size of a line is 6, this line was " 
					+ subHex.length, "w");
			return -1;
		}
		
		//save length
		dataLength = subHex[startOnDataByte + 1];
		
		//The line must start with ':'
		if(subHex[startOnDataByte] != 58) {
			logger.logcat("splitHex(): Line not starting with ':' !", "w");
			return -1;
		}
		//If record type is 0x01 (file end) and data size > 0, return false
		else if(subHex[startOnDataByte + 4]==1 && dataLength>0) {
			logger.logcat("splitHex(): Contains data, but are told to stop!", "w");
			return -1;
		}
		//If record type is 0x01 (file end) and it exist more bytes to read, return false
		else if(subHex[startOnDataByte + 4]==1 &&
				subHex.length>startOnDataByte + dataLength + 6) {
			logger.logcat("splitHex(): Contains more lines with data, " +
					"but are told to stop!", "w");
			return -1;
		}
		//If record type is 0x00 (data record) and data size equals 0, return false
		else if(subHex[startOnDataByte + 4]==0 && subHex[startOnDataByte + 1]==0) {
			logger.logcat("splitHex(): Told to send data, but contains no data!", "w");
			return -1;
		}
		else {
			binList.add(new ArrayList<Byte>());
			
			byte[] tempBytes = new byte[subHex[startOnDataByte + 1] + 6];
			
			tempBytes[0] = subHex[startOnDataByte + 1]; //size
			tempBytes[1] = subHex[startOnDataByte + 2]; //start address
			tempBytes[2] = subHex[startOnDataByte + 3]; //start address
			tempBytes[3] = subHex[startOnDataByte + 4]; //record
			
			//save data
			for (int i = 0; i < dataLength; i++) {
				tempBytes[i] = subHex[i + startOnDataByte + 5];
			}
			
			//save checksum
			tempBytes[tempBytes.length - 1] = subHex[startOnDataByte + 5 + dataLength];
			
			//Check if the checksum is correct
			if(checkData(tempBytes, startOnDataByte)) {
				//End of hex file
				try {
					// Save data
					for(int i=0; i<dataLength; i++) {
						dataList.add(subHex[startOnDataByte + i + 5]); 
					}
					
					return (startOnDataByte + dataLength + 6);
				} catch (ArrayIndexOutOfBoundsException e) {
					// Array out of bounds
					return -1;
				}
			}
			//Checksum not correct
			else {
				logger.logcat("splitHex(): Checksum failed!", "w");
				return -1;
			}
		}
	}
	
	/**
	 * Calculate and check the checksum of the given byte array
	 * with the checksum from hex file.
	 * 
	 * @param data Byte array to check.
	 * @param startByte Where to start loading bytes from hex file.
	 * 
	 * @return True if checksum is correct, false if not.
	 */
	private boolean checkData (byte[] data, int startByte) {
		int byteValue = 0;

		//Add the values of all the fields together, except checksum 
		for(int i=0; i<data.length-2; i++) {
			byteValue += subHex[startByte + i + 1];
		}

		int b = 0x100;

		byte check = (byte) (b-byteValue);
		
		return (byte)(check&0xFF) == (data[(data.length-1)&0xFF]);
	}
	
	/**
	 * Convert a byte array into a hex string. Useful for debugging.
	 * @param bytes
	 * @return string with hex
	 */
	public static String bytesToHex(byte[] bytes) {
		final char[] hexArray = {'0','1','2','3','4','5','6','7','8','9',
				'a','b','c','d','e','f'};
		char[] hexChars = new char[bytes.length * 5];
		int v;
		for ( int j = 0; j < bytes.length; j++ ) {
			v = bytes[j] & 0xFF;
			hexChars[j * 5] = 91;
			hexChars[j * 5 + 1] = hexArray[v >>> 4];
			hexChars[j * 5 + 2] = hexArray[v & 0x0F];
			hexChars[j * 5 + 3] = 93;
			hexChars[j * 5 + 4] = 32;
		}
		return new String(hexChars);
	}
	
	/**
	 * Convert a byte into hex.
	 * @param b One byte to convert
	 * @return String with one hex
	 */
	public static String oneByteToHex(byte b) {
		byte[] tempB = new byte[1];
		tempB[0] = b;
		return new String(bytesToHex(tempB));
	}
}
