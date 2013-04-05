package no.group09.stk500_v1;

import java.io.UnsupportedEncodingException;

public class HexTest {

	public static void main(String[] args) {
		
		String hexData = 
				"3A100000000C9461000C947E000C947E000C947E0095" +
				"3A100010000C947E000C947E000C947E000C947E0068" +
				"3A100010000C947E000C947E000C947E000C947E0068" +
				"3A00000001FF";
		
//		String hexData =
//				"3A0300300002337A1E";
		
//		String hexData =
//				"3A10246200464C5549442050524F46494C4500464C33";
		
		byte[] bytes = new byte[hexData.length() / 2];
		
		for(int i=0; i < hexData.length(); i+=2)
		{
			bytes[i/2] = Integer.decode("0x" + hexData.substring(i, i + 2)).byteValue();
		}
		
		Logger log = null;
		
		Hex hex = new Hex(bytes, log);
//		boolean state = hex.getChecksumStatus(0);
		
//		System.out.println(hex.getLines());
//		System.out.println(state);
		
//		System.out.println("#############");
//		System.out.println("Line 1:");
		
		byte[] testArray = hex.getHexLine(0);
		System.out.println("#### LINE 0");
		for (int i = 0; i < testArray.length; i++) {
			System.out.println(testArray[i]);
		}
		
		
		byte[] testArray1 = hex.getHexLine(1);
		System.out.println("#### LINE 1");
		for (int i = 0; i < testArray.length; i++) {
			System.out.println(testArray1[i]);
		}
	}

}
