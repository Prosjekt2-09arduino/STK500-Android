package no.group09.stk500_v1;

public class HexTest {

	public static void main(String[] args) {
		
		String hexData = 
				"3A100000000C9461000C947E000C947E000C947E0095" +
				"3A100010000C947E000C947E000C947E000C947E0068" +
				"3A100010000C947E000C947E000C947E000C947E0068" +
				"3A00000001FF";
		
		byte[] bytes = new byte[hexData.length() / 2];
		
		for(int i=0; i < hexData.length(); i+=2)
		{
			bytes[i/2] = Integer.decode("0x" + hexData.substring(i, i + 2)).byteValue();
		}
		
		Logger log = null;
		
		Hex hex = new Hex(bytes, log);
		
		for(int line=0; line<4; line++) {
			System.out.println("\n#### LINE: " + line);
			
			byte[] testArray = hex.getHexLine(line);
			for (int i = 0; i < testArray.length; i++) {
				System.out.println(testArray[i]);
			}
		}
		
		System.out.println(hex.getLines());
	}

}
