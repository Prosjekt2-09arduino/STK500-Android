package no.group09.stk500_v1;

public class HexTest {

	public static void main(String[] args) {
		
		String hexData = 
				"3A100000000C9461000C947E000C947E000C947E0095" +
				"3A100010000C947E000C947E000C947E000C947E0068" +
				"3A100010000C947E000C947E000C947E000C947E0068" +
				"3A00000001FF";
		
		//Convert string to byte array
		byte[] bytes = new byte[hexData.length() / 2];
		for(int i=0; i < hexData.length(); i+=2)
		{
			bytes[i/2] = Integer.decode("0x" + hexData.substring(i, i + 2)).byteValue();
		}
		
		// Empty logger
		Logger log = null;
		
		Hex hex = new Hex(bytes, log);
		
		byte[][] testArray = new byte[100][24];
		
		// Test 5 lines, only the first 4 will work
		for(int line=0; line<5; line++) {
			System.out.println("\n#### LINE: " + line);
			System.out.println("Checksum: " + hex.getChecksumStatus(line));
			
			testArray[line] = hex.getHexLine(line);
			for (int i = 0; i < testArray[line].length; i++) {
				System.out.println(testArray[line][i]);
			}
		}
	}

}
