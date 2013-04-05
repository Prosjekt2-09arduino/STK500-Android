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
		
		logger.debugTag("State: " + state);
	}
	
	public byte[] getHexLine(int line)
	{
		return formatHexLine(line);
	}
	
	public int getLines()
	{
		return line;
	}
	
	public boolean getChecksumStatus(int l)
	{
		return true;
	}	
	
	private boolean splitHex(byte[] subHex) {
		byte[] tempHex = new byte[maxBytesOnLine];
		
//		If no bytes left
		if(subHex.length == 0) {
			logger.debugTag("ERROR: No bytes left.");
			return false;
		}
//		The line must start with ':'
		else if(subHex[0] != 58) {
			logger.debugTag("ERROR: Line not starting with ':' !");
			return false;
		}
//		End of hex file
		else if(subHex[1] == 0 && subHex[2] == 0) {
			logger.debugTag("End of hex file.");
			return true;
		}
		else {
			tempHex[0] = subHex[1];	// size
			tempHex[1] = subHex[2];	// start address
			tempHex[2] = subHex[3];	// end address
			tempHex[3] = subHex[4];	// record
			
//			data
			int dataLength = tempHex[0];
			
			for (int i = 5; i < dataLength+5; i++) {
				tempHex[i] = subHex[i];
			}
			
			tempHex[4+dataLength] = subHex[5+dataLength];	// checksum
			
			for(int i=0; i<dataLength+6; i++) {
				//binary[line][i] = tempHex[i];
				binary[line] = Arrays.copyOfRange(subHex, 6+dataLength, subHex.length);
			}
			
			//Send array recursiv
			line++;
			return splitHex(Arrays.copyOfRange(subHex, 6+dataLength, subHex.length));
		}
	}
	
	
//	return size, address x 2, data
	private byte[] formatHexLine(int line)
	{
		int length = binary[line][1];
		
		byte tempBinary[] = new byte[maxBytesOnLine];
		
		tempBinary[0] = binary[line][1];	// Length
		tempBinary[1] = binary[line][2];	// Address 1
		tempBinary[2] = binary[line][3];	// Address 2
		
//		Data
		for(int i=4; i<length+4; i++) {
			tempBinary[i] = binary[line][i];
		}
		
		return tempBinary;
	}
	

	private boolean checkData(int line) {
		int checksum = 0;
		
//		System.out.println("First: " + binary[line][2]);
		System.out.println(binary[1]);
		
		int length = unPackTwoBytes(binary[line][2], binary[line][1]);
//		System.out.println("Length: " + length);
		
		for(int i=0; i<length; i++) {
			checksum += binary[line][i];
		}
		
//		System.out.println(checksum);
//		System.out.println(binary[22]);
		return true;
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
