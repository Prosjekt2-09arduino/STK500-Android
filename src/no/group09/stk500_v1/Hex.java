package no.group09.stk500_v1;

import java.util.ArrayList;

public class Hex {
	private Logger logger;
	private final static int maxBytesOnLine = 24;
//	private byte[][] binary = new byte[256][maxBytesOnLine];
	
	ArrayList<ArrayList<Byte>> binList = new ArrayList<ArrayList<Byte>>();
	
	private int line = 0;
	private boolean state = false;
	private static final int ADDITION_TO_LENGTH_IN_CHECKSUM = 4;
	
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
//		byte[] tempHex = new byte[maxBytesOnLine-3]; // we do not need start byte, checksum and record
		
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
			
//			tempHex[0] = subHex[1];	// size
			binList.get(line).add(subHex[1]); // size
			
//			tempHex[1] = subHex[2];	// start address
			binList.get(line).add(subHex[2]); // start address
			
//			tempHex[2] = subHex[3];	// end address
			binList.get(line).add(subHex[3]); // start address
			
//			tempHex[3] = subHex[4];	// record, only used to calculate checksum
			binList.get(line).add(subHex[4]); // record
			
			//save length
//			int dataLength = tempHex[0];
			int dataLength = binList.get(line).get(0);

			//save data
			for (int i = 5; i < dataLength+5; i++) {
//				tempHex[i] = subHex[i+2];
				binList.get(line).add(subHex[i]);
			}
			
			//save checksum
//			tempHex[3+dataLength] = subHex[5+dataLength];
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
<<<<<<< HEAD
	 * Format line in hex file,
	 * @param l
	 * @return one line from hex, including size, address (high + low) and data.
=======
	 * Format line in hex file into an array with:
	 * 1 byte size
	 * 2 byte address
	 * n byte address
	 * @param line number in hex file
	 * @return byte array, empty if the line is out of bounds
>>>>>>> Change array to ArrayList #22
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
<<<<<<< HEAD
		int length = binary[line][0] + ADDITION_TO_LENGTH_IN_CHECKSUM;
=======
		int length = binList.get(line).size();
>>>>>>> Change array to ArrayList #22
		System.out.println("Number of bytes of data: " + length);
		
		int byteValue = 0;
		
		//Add the values of all the fields together
		for(int i=0; i<length-1; i++) {
<<<<<<< HEAD
			byteValue += binary[line][i];
=======
			byteValue += binList.get(line).get(i);
>>>>>>> Change array to ArrayList #22
		}
		
		int b = 0x100;
		
		byte check = (byte) (b-byteValue);
		
<<<<<<< HEAD
		return (check&0xFF) == (binary[line][length-1]&0xFF);
=======
		return (check&0xFF) == (binList.get(line).get(length-1)&0xFF);
>>>>>>> Change array to ArrayList #22
	}
	
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
