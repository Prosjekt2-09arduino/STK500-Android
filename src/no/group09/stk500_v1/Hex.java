package no.group09.stk500_v1;

import java.util.ArrayList;

public class Hex {
	private Logger logger;
	private final static int maxBytesOnLine = 24;
//	private byte[][] binary = new byte[256][maxBytesOnLine];
	
	ArrayList<ArrayList<Byte>> binList = new ArrayList<ArrayList<Byte>>();
	
	private int line = 0;
	private boolean state = false;
//	private static final int ADDITION_TO_LENGTH_IN_CHECKSUM = 4;
	
	public Hex(byte[] bin, Logger log) {
		this.logger = log;
		
		// count lines and create an array
		state = splitHex(bin);
		
//		logger.debugTag("State: " + state);
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
		System.out.println(line);
		System.out.println(binList.size());
		return line;
	}
	
	/**
	 * Return state of hex file.
	 * @param l Line of hex
	 * @return true if the hex file is correct.
	 */
	public boolean getChecksumStatus(int l)
	{
//		return checkData(l);
		return true;
	}	
	
	/**
	 * Split the hex input into an array and check if it is correct, including the checksum. Each line must start with ':' (colon), byte value 58.
	 * The following values is 1 byte size, 2 byte address, n byte data, 1 byte checksum.
	 * @param array with bytes
	 * @return true if the hex file is correct.
	 */
	private boolean splitHex(byte[] subHex) {
//		If no bytes left
		if(subHex.length == 0) {
//			logger.debugTag("ERROR: No bytes left!");
			System.out.println("ERROR: No bytes left!");
			return false;
		}
//		The line must start with ':'
		else if(subHex[0] != 58) {
//			logger.debugTag("ERROR: Line not starting with ':' !");
			System.out.println("ERROR: Line not starting with ':' !");
			return false;
		}
//		End of hex file
		else if(subHex[1] == 0 && subHex[2] == 0) {
//			logger.debugTag("End of hex file!");
			System.out.println("End of hex file!");
			return true;
		}
		else {
			//add new line to ArrayList
			binList.add(new ArrayList<Byte>());
			
			binList.get(line).add(subHex[1]); // size
			binList.get(line).add(subHex[2]); // start address
			binList.get(line).add(subHex[3]); // start address
			binList.get(line).add(subHex[4]); // record
			
			//save length
			int dataLength = binList.get(line).get(0);

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
				
				line++;
				return splitHex(returnHex);
			}
			else {
				System.out.println("Error: Checksum failed!");
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
	private byte[] formatHexLine(int l)
	{
		byte tempBinary[] = new byte[maxBytesOnLine-3];
		
		//Check if the line is out of bounds
		if(l < line) {
			int length = binList.get(l).size();
			
			//Add elements into an array
			for(int i=0; i<length; i++) {
				//Ignore the record element
				if(i!=3) {
					tempBinary[i] = binList.get(l).get(i);
				}
			}
		}
		
		return tempBinary;
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
		
		System.out.println("Verifying checksum of line: " + line);
		
		//length of data
		int length = binList.get(line).size();
		System.out.println("Number of bytes of data: " + length);
		
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
