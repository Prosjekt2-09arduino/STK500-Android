package no.group09.stk500_v1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;


public class STK500v1 {
	private OutputStream output;
	private InputStream input;
	private Logger logger;
	private byte[] binary;
	
	public STK500v1 (OutputStream output, InputStream input, Logger log, byte[] binary) {
		this.output = output;
		this.input = input;
		this.logger = log;
		this.binary = binary;
		log.debugTag("Initializing programmer");
		
		//try to get programmer version
		String version = checkIfStarterKitPresent();
		log.debugTag(version);
		log.printToConsole(version);
	}
	
	
	private String checkIfStarterKitPresent() {
		logger.debugTag("Detect programmer");
		String version = "";
		
		//Send request
		try {
			byte[] out = new byte[] {
					ConstantsStk500v1.STK_GET_SIGN_ON, ConstantsStk500v1.CRC_EOP
			};
			output.write(out);
			logger.debugTag("Sending bytes: " + Arrays.toString(out));
		} catch (IOException e) {
			logger.debugTag("Communication problem: Can't send request for programmer version");
			return "-1";
		}
		
		//Read response
		try {
			char[] response = new char[7];
			int responseIndex = 0;
			int readResult = 0;
			byte readByte;
			while (readResult >= 0) {
				readResult = input.read();
				if (readResult == -1) {
					//TODO: Discover when/if this happens
					logger.debugTag("End of stream encountered in checkIfStarterKitPresent()");
					break;
				}
				readByte = (byte) readResult;
				logger.debugTag("Read byte: " + readByte);
				if (responseIndex == 0 && readByte == ConstantsStk500v1.STK_INSYNC) {
					//good response, next byte should be first part of the string
					//optiboot never sends the string, fix
					responseIndex = 7;
					continue;
				} else if (responseIndex == 7 && readByte == ConstantsStk500v1.STK_OK) {
					//index too high for array writing, return string if all OK
					version = "Arduino (HAX)";
					//version = String.copyValueOf(response);
					return version;
				} else if (responseIndex >= 0 && responseIndex < 7) {
					//read string
					response[responseIndex] = (char) readByte;
					responseIndex++;
				} else if (responseIndex == 0 && readByte == ConstantsStk500v1.STK_NOSYNC){
					//not in sync
					//TODO: Consider attempting to get synch
					logger.debugTag("Unable to synchronize");
					break;
				} else {
					logger.debugTag("Not terminated by STK_OK in checkIfStarterKitPresent()!");
					break;
				}
			}
		} catch (IOException e) {
			logger.debugTag("Communication problem: Can't receive programmer version");
		}
		
		return version;
	}
	
	private void getSynchronization() {
	}
	
	private void getParameterValue() {
	}
	
	private void setParameterValue() {
	}
	
	private void setDeviceProgrammingParameters() {
	}

	private void enterProgramMode() {
	}
	
	private void leaveProgramMode() {
	}
	
	private void chipErase() {
	}
	
	private void checkForAddressAutoincrement() {
	}
	
	private void LoadAddress() {
	}
	
	private void programFlashMemory() {
	}
	
	private void programDataMemory() {
	}
	
	private void programLockBits() {
	}
	
	private void programPage() {
	}

	private void readFlashMemory() {
	}
	
	private void readDataMemory() {
	}
	
	private void readLockBits() {
	}

	private void readPage() {
	}
	
	private void readSignatureBits() {
	}
	
	private void readOscillatorCalibrationByte() {
	}
	
	private void universalCommand() {
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
