package no.group09.stk500_v1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import javax.xml.ws.Response;

import com.sun.xml.internal.bind.v2.runtime.reflect.opt.Const;


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
	
	/**
	 * Attempt to handshake with the Arduino. The method is modified to account for
	 * the Optiboot loader not returning a version string, just the
	 * in sync and OK bytes.
	 * @return -1 on failure and Arduino otherwise
	 */
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
					version = "Arduino";
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
	
	/**
	 * Command to try to regain synchronization when sync is lost. Returns when
	 * sync is regained, or it exceeds 100 tries.
	 * 
	 * @return true if sync is regained, false if number of tries exceeds 100
	 */
	private boolean getSynchronization() {
		byte[] getSyncCommand = {ConstantsStk500v1.STK_GET_SYNC, ConstantsStk500v1.CRC_EOP};
		int tries = 0;
		
		while (tries < 100) {
			tries++;
			
			try {
				output.write(getSyncCommand);
			} catch (IOException e) {
				logger.debugTag("Unable to write output in getSynchronization");
				e.printStackTrace();
				return false;
			}
			//If the response is valid, return. If not, continue
			if (checkInput()) return true;
		}
		return false;
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
		
//		byte[] command
		
	}
	
	/**
	 * Load 16-bit address down to starterkit. This command is used to set the 
	 * address for the next read or write operation to FLASH or EEPROM. Must 
	 * always be used prior to Cmnd_STK_PROG_PAGE or Cmnd_STK_READ_PAGE.
	 * 
	 * @param address the address that is to be written as an int
	 * 
	 * @return true if it is OK to write the address, false if not.
	 */
	private boolean loadAddress(int address) {
		byte[] addr = packTwoBytes(address);
		byte[] loadAddr = new byte[4];
		
		loadAddr[0] = ConstantsStk500v1.STK_LOAD_ADDRESS;
		loadAddr[1] = addr[1];
		loadAddr[2] = addr[0];
		loadAddr[3] = ConstantsStk500v1.CRC_EOP;
		
		try {
			output.write(loadAddr);
		} catch (IOException e) {
			logger.debugTag("Unable to write output in loadAddress");
			e.printStackTrace();
			return false;
		}
		
		return checkInput();
	}
	
	/**
	 * Takes an integer, splits it into bytes, and puts it in an byte array
	 * 
	 * @param integer the integer that is to be split
	 * @return an array with the integer as bytes
	 */
	private byte[] packTwoBytes(int integer) {
		byte[] bytes = new byte[2];
        //store the 8 least significant bits
        bytes[1] = (byte) (integer & 0xFF);
        //store the next 8 bits
        bytes[0] = (byte) ((integer >> 8) & 0xFF);
		return bytes;
	}
		
	private void programDataMemory() {
	}
	
	private void programLockBits() {
	}
	
	private void programPage() {
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
	
	private void readFlashMemory() {
	}
	
	/**
	 * Method used to get and check input from the Arduino. It reads the input, and
	 * check whether the response is STK_INSYNC and STK_OK, or STK_NOSYNC. If the
	 * response is STK_INSYNC and STK_OK the operation was successful. If not 
	 * something went wrong.
	 * 
	 * @return true if response is STK_INSYNC and STK_OK, false if not.
	 */
	private boolean checkInput() {
		
		int intInput = -1;
		try {
			intInput = input.read();
		} catch (IOException e) {
			logger.debugTag("Unable to read input in checkInput");
			e.printStackTrace();
			return false;
		}
		
		if (intInput == -1) {
			logger.debugTag("End of stream encountered in checkInput");
			return false;
		}
		
		byte byteInput;
		
		if (intInput == ConstantsStk500v1.STK_INSYNC){
			try {
				intInput = input.read();
			} catch (IOException e) {
				logger.debugTag("Unable to read input in checkInput");
				e.printStackTrace();
			}
			
			if (intInput == -1) {
				logger.debugTag("End of stream encountered in checkInput");
				return false;
			}
			//Input is not equal to -1. Cast to byte
			byteInput = (byte)intInput;
			
			if (byteInput == ConstantsStk500v1.STK_OK) {
				
				//Two bytes sent. Response OK. Return true
				return true;
			}
			logger.debugTag("Reponse was STK_INSYNC but not STK_OK in checkInput");
			return false;
		}
		else {
			logger.debugTag("Response was not STK_INSYNC in checkInput");
			getSynchronization();
			//Synchronization is in place, but the operation was not successful. Try again.
			return false;
		}
	}
	
	/**
	 * Method used to program one word to the flash memory.
	 * 
	 * @param flash_low first byte of the word to be programmed.
	 * @param flash_high last byte of the word to be programmed.
	 * 
	 * @return true if the method was able to program the word to the flash memory,
	 * false if not.
	 */
	private boolean programFlashMemory(byte flash_low, byte flash_high) {
		
		byte[] uploadFile = new byte[4];
		
		uploadFile[0] = ConstantsStk500v1.STK_PROG_FLASH;
		uploadFile[1] = flash_low;
		uploadFile[2] = flash_high;
		uploadFile[3] = ConstantsStk500v1.CRC_EOP;
		
		try {
			output.write(uploadFile);
		} catch (IOException e) {
			logger.debugTag("Unable to write output in programFlashMemory");
			e.printStackTrace();
			return false;
		}
		
		return checkInput();
	}
	
	/**
	 * Used to upload files to the flash memory. This method sends the content
	 * of the binary byte array in pairs of two to the flash memory.
	 */
	private void uploadFile() {
		
		byte flash_low, flash_high;
		byte[] uploadFile = new byte[4];
		int i = 0;
		
		while (i < binary.length) {
			
			//If i >= binary.length the file is uploaded
			if (i >= binary.length){
				logger.printToConsole("End of file. Upload finished with success.");
				return;
			}
			
			//If i <= binary.length, fetch the two next bytes from the binary array
			if (i + 1 <= binary.length) {
				//Fetch the two next bytes in the binary array
				flash_low = binary[i];
				flash_high = binary[i+1];
			}
			//Fetch the last byte in the binary array
			else {
				flash_low = binary[i];
				
				//FIXME: The low byte is now the last element in the binary array. What to do?
				flash_high = 0;
			}
			
			//Program the flash and store the result
			boolean programFlashSuccess = programFlashMemory(flash_low, flash_high);
			
			if (programFlashSuccess) {
				//Increment position in binary array
				i += 2;
				//Two bytes sent. Response OK. Repeat.
				continue;
			}
			else if (!programFlashSuccess) {
				//Not able to program flash. Retry.
				logger.debugTag("programFlashMemory returned false. Unable to program flash. Retrying");
				continue;
			}
		}
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