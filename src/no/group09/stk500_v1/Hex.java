package no.group09.stk500_v1;

import java.util.Arrays;

public class Hex {
	private Logger logger;
	private final static int maxBytesOnLine = 24;
	private byte[][] binary = new byte[256][maxBytesOnLine];
	private int line = 0;
	private boolean state = false;
	
	public Hex(byte[] bin, Logger log) {
		this.logger = log;
		
		// count lines and create an array
		state = splitHex(bin);
		
//		logger.debugTag("State: " + state);
	}
	
	/**
	 * Get a line from hex
	 * @param line
	 * @return array with size, address (high), address (low) and data
	 */
	public byte[] getHexLine(int line)
	{
		return formatHexLine(line);
	}
	
	/**
	 * 
	 * @return 
	 */
	public int getLines()
	{
		return line;
	}
	
	/**
	 * 
	 * @param l Line of hex
	 * @return hex file status
	 */
	public boolean getChecksumStatus(int l)
	{
//		return checkData(l);
		return true;
	}	
	
	/**
	 * Split the hex input into an array.
	 * Size, address (high), address (low), data
	 * @param subHex
	 * @return true if the hex file is correct.
	 */
	private boolean splitHex(byte[] subHex) {
		byte[] tempHex = new byte[maxBytesOnLine-3]; // we do not need start byte, checksum and record
		
//		If no bytes left
		if(subHex.length == 0) {
//			logger.debugTag("ERROR: No bytes left.");
			System.out.println("ERROR: No bytes left.");
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
//			logger.debugTag("End of hex file.");
			System.out.println("End of hex file.");
			return true;
		}
		else {
			//TODO: need to verify checksum here
			
			tempHex[0] = subHex[1];	// size
			tempHex[1] = subHex[2];	// start address
			tempHex[2] = subHex[3];	// end address
//			tempHex[3] = subHex[4];	// record
			
//			data
			int dataLength = tempHex[0];
			
			for (int i = 3; i < dataLength+3; i++) {
				tempHex[i] = subHex[i+2];
			}
			
			tempHex[3+dataLength] = subHex[5+dataLength];	// checksum
//			System.out.println("Checksum: " + tempHex[3+dataLength]);
			
//			for(int i=0; i<dataLength+6; i++) {
//				binary[line][i] = tempHex[i];
//				binary[line] = Arrays.copyOfRange(subHex, 6+dataLength, subHex.length);
//			}
			
			binary[line] = Arrays.copyOfRange(subHex, 1, dataLength+3);
			
			//Send array recursiv
			line++;
			return splitHex(Arrays.copyOfRange(subHex, dataLength+6, subHex.length));
		}
	}
	
	
	/**
	 * Format line in hex file,
	 * @param line
	 * @return one line from hex, including size, address (high + low) and data.
	 */
	private byte[] formatHexLine(int l)
	{
		byte tempBinary[] = new byte[maxBytesOnLine];
		
		//Check if the line is out of bounds
		if(l <= line) {
			int length = binary[l][0];
			
			tempBinary[0] = binary[l][0];	// Length
			tempBinary[1] = binary[l][1];	// Address 1
			tempBinary[2] = binary[l][2];	// Address 2
			
			//Data
			for(int i=3; i<length+2; i++) {
				tempBinary[i] = binary[l][i];
			}
		}
		
		return tempBinary;
	}
	
//	/**
//	 * Check if the checksum is correct.
//	 * @param line
//	 * @return true if checksum is correct
//	 */
//	private boolean checkData(int line) {
//		int checksum = 0;
//		
//		System.out.println("\nChecksum on line " + line);
//		
//		//length of data
//		int length = binary[line][0];
//		System.out.println("Length:     " + length);
//		
//		//data
//		for(int i=0; i<length; i++) {
//			checksum += binary[line][i];
//		}
//		
//		System.out.println("Last byte:  " + binary[line][length+2]);
//		System.out.println("Sum:        " + checksum);
//		
//		System.out.println("Checksum done.\n");
//		
//		return true;
//	}
//	
	/**
	 * Read two unsigned bytes into an integer
	 * @param high Most significant byte
	 * @param low  Least significant byte
	 * @return
	 */
	private static int unPackTwoBytes(byte high, byte low) {
		int out = (decodeByte(high) << 8) | (decodeByte(low));
		return out;
	}
	
	/**
	 * Get the unsigned value of a byte
	 * @param unsignedByte
	 * @return
	 */
	private static int decodeByte(byte unsignedByte) {
		return 0xFF & unsignedByte;
	}
}
