package no.group09.stk500_v1;

import java.util.ArrayList;

public class Hex {
	private Logger logger;
	ArrayList<ArrayList<Byte>> binList = new ArrayList<ArrayList<Byte>>();
	
	private int line = -1;
	private boolean state = false;
	
	public Hex(byte[] bin, Logger log) {
		this.logger = log;
		
		// count lines and create an array
		state = splitHex(bin);
		
		logger.debugTag("Hex file status: " + state);
	}
	
	/**
	 * Return a line from hex.
	 * @param line
	 * @return array with size, address (high), address (low) and data
	 */
	public byte[] getHexLine(int line)
	{
		return formatHexLine(line);
	}
	
	/**
	 * Returns number of lines in hex file.
	 * @return line from hex file
	 */
	public int getLines()
	{
		return line;
	}
	
	/**
	 * Return state of hex file
	 * @return true if the hex file is correct
	 */
	public boolean getChecksumStatus()
	{
		return state;
	}	
	
	/**
	 * Split the hex input into an array and check if it is correct, including the checksum. Each line must start with ':' (colon), byte value 58.
	 * The following values is 1 byte size, 2 byte address, n byte data, 1 byte checksum.
	 * @param array with bytes
	 * @return true if the hex file is correct.
	 */
	private boolean splitHex(byte[] subHex) {
		int dataLength = 0;
		
		//The minimum length of a line is 6, including the start byte ':'
		if(subHex.length<6) {
			logger.debugTag("ERROR: The minimum size of a line is 6, this line was " + subHex.length);
			return false;
		}
		//If no bytes left
		else if(subHex.length == 0) {
			logger.debugTag("ERROR: No bytes left!");
			return false;
		}
		else {
			//save length
			dataLength = subHex[1];
		}
		
		//The line must start with ':'
		if(subHex[0] != 58) {
			logger.debugTag("ERROR: Line not starting with ':' !");
			return false;
		}
		//If record type is 0x01 (file end) and data size > 0, return false
		else if(subHex[4]==1 && dataLength>0) {
			logger.debugTag("ERROR: Contains data, but are told to stop!");
			return false;
		}
		//If record type is 0x00 (data record) and data size equals 0, return false
		else if(subHex[4]==0 && subHex[1]==0) {
			logger.debugTag("ERROR: Told to send data, but contains no data!");
			return false;
		}
		else {
			//add new line to ArrayList
			line++;
			
			binList.add(new ArrayList<Byte>());
			
			//Save data size
			binList.get(line).add(subHex[1]); // size
			binList.get(line).add(subHex[2]); // start address
			binList.get(line).add(subHex[3]); // start address
			binList.get(line).add(subHex[4]); // record

			//save data
			for (int i = 5; i < dataLength+5; i++) {
				binList.get(line).add(subHex[i]);
			}
			
			//save checksum
			binList.get(line).add(subHex[5+dataLength]);
			
			//Check if the checksum is correct
			if(checkData(line)==true) {
				//Split the rest of the array
				byte[] returnHex = new byte[subHex.length-dataLength-6];
				for (int i = dataLength+6; i < subHex.length; i++) {
					returnHex[i-dataLength-6] = subHex[i];
				}
				
				//End of hex file
				if(subHex[1] == 0) {
					//Print to log if hex file contains more lines, but are told to stop
					if(returnHex.length >0) {
						logger.debugTag("ERROR: Told to stop, but contains more data!");
//						return false;
					}
					logger.debugTag("End of hex file!");
					return true;
				}
				else {
					return splitHex(returnHex);
				}
			}
			else {
				return false;
			}
		}
	}
	
	
	/**
	 * Format line in hex file into an array with:
	 * 1 byte size
	 * 2 byte address
	 * n byte address
	 * @param line number in hex file
	 * @return byte array, empty if the line is out of bounds
	 */
	private byte[] formatHexLine(int line)
	{		
		byte tempBinary[] = null;
		
		//Check if the line is out of bounds
		try {
			//Create a new temporary array
			tempBinary = new byte[binList.get(line).size()];
						
			//Add elements into an array
			for(int i=0; i<binList.get(line).size(); i++) {
				//Ignore the record element
				if(i!=3) {
					tempBinary[i] = binList.get(line).get(i);
				}
			}
			return tempBinary;
			
		} catch (Exception e) {
			logger.debugTag("ERROR: Out of bounds!");
			tempBinary = new byte[0];
			return tempBinary;
		}
	}
	
	/**
	 * Calculate and check the checksum of the given line in the binary array.
	 * 
	 * @param line integer telling which line in the binary array that is to be
	 * checksummed.
	 * 
	 * @return true if checksum is correct, false if not.
	 */
	private boolean checkData (int line) {
		//length of data
		int length = binList.get(line).size();
		
		int byteValue = 0;
		
		//Add the values of all the fields together
		for(int i=0; i<length-1; i++) {
			byteValue += binList.get(line).get(i);
		}
		
		int b = 0x100;
		
		byte check = (byte) (b-byteValue);
				
		return (check&0xFF) == (binList.get(line).get(length-1)&0xFF);
	}
}
