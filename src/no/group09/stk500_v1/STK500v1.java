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
		
		for (int i = 0; i < 10; i++) {
			if (enterProgramMode()) {
				logger.debugTag("The ardunino has entered programming mode. Trying to leave...");
				for (int j = 0; j < 10; j++) {
					if(leaveProgramMode()) {
						logger.debugTag("The arduino has now left programming mode.");
						break;
					}
				}
				break;
			}
		}
		
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

	/**
	 * Enter programming mode. Set device and programming parameters before calling.
	 * 
	 * @return true if the connected device was able to enter programming mode.
	 * False if not.
	 */
	private boolean enterProgramMode() {
		//send command
		byte[] command = new byte[] {
				ConstantsStk500v1.STK_ENTER_PROGMODE, ConstantsStk500v1.CRC_EOP 	
		};

		try {
			output.write(command);
		} catch (IOException e) {
			logger.debugTag("Communication problem on sending request to enter programming mode");
			return false;
		}

		//check response
		boolean ok = checkInput(true, ConstantsStk500v1.STK_ENTER_PROGMODE);
		if (!ok) {
			logger.debugTag("Unable to enter programming mode");
		}
		return ok;
	}

	/**
	 * Leave programming mode.
	 * 
	 * @return True if the arduino was able to leave programming mode. false if not.
	 */
	private boolean leaveProgramMode() {
		//send command
		byte[] command = new byte[] {
				ConstantsStk500v1.STK_LEAVE_PROGMODE, ConstantsStk500v1.CRC_EOP
		};

		try {
			output.write(command);
		} catch (IOException e) {
			logger.debugTag("Communication problem on leaving programming mode");
		}

		//check response
		boolean ok = checkInput();
		if (!ok) {
			logger.debugTag("Unable to leave programming mode");
		}
		return ok;
	}

	private void chipErase() {
	}

	/**
	 * Check if the write/read address is automatically incremented while using 
	 * the Cmnd_STK_PROG/READ_FLASH/EEPROM commands. Since STK500 always 
	 * auto-increments the address, this command will always be successful.
	 * 
	 * @return true if response is STK_INSYNC and STK_OK, false if not.
	 */
	private boolean checkForAddressAutoincrement() {

		//TODO: Add call to this method.

		byte[] command = new byte[2];

		command[0] = ConstantsStk500v1.STK_CHECK_AUTOINC;
		command[1] = ConstantsStk500v1.CRC_EOP;

		try {
			output.write(command);
		} catch (IOException e) {
			logger.debugTag("Unable to write output in checkForAddressAutoincrement");
			e.printStackTrace();
			return false;
		}

		return checkInput();
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

	/**
	 * Method used to program one byte in EEPROM memory
	 * 
	 * @param data the byte of data that is to be programmed
	 * @return true if response is STK_INSYNC and STK_OK, false if not.
	 */
	private boolean programDataMemory(byte data) {

		byte[] programCommand = new byte[3];

		programCommand[0] = ConstantsStk500v1.STK_PROG_DATA;
		programCommand[1] = data;
		programCommand[2] = ConstantsStk500v1.CRC_EOP;

		try {
			output.write(programCommand);
		} catch (IOException e) {
			logger.debugTag("Could not write output in programDataMemory");
			e.printStackTrace();
			return false;
		}

		return checkInput();
	}

	private void programLockBits() {
	}

	/**
	 * Download a block of data to the starterkit and program it in FLASH or 
	 * EEPROM of the current device. The data block size should not be larger 
	 * than 256 bytes. bytes_high and bytes_low
	 * are part of an integer that describes the address to be written/read
	 * 
	 * @param bytes_high most significant byte of the address
	 * @param bytes_low least significant byte of the address
	 * @param writeFlash boolean indicating if it should be written to flash memory
	 * or EEPROM. True = flash. False = EEPROM
	 * @param data byte array of data
	 */
	private void programPage(byte bytes_high, byte bytes_low, boolean writeFlash, byte[] data) {

		byte[] programPage = new byte[6];
		byte memtype;

		if (writeFlash) memtype = (byte)'F';
		else memtype = (byte)'E';

		programPage[0] = ConstantsStk500v1.STK_PROG_PAGE;
		programPage[1] = bytes_high;
		programPage[2] = bytes_low;
		programPage[3] = memtype;

		for (int i = 4; i < data.length; i++) {
			programPage[i] = data[i];
		}
		programPage[data.length] = ConstantsStk500v1.CRC_EOP;
	}



	private void readLockBits() {
	}

	private void readSignatureBits() {
	}

	private void readOscillatorCalibrationByte() {
	}

	private void universalCommand() {
	}

	/**
	 * Read a block of data from FLASH or EEPROM of the current device. The data
	 * block size should not be larger than 256 bytes. bytes_high and bytes_low
	 * are part of an integer that describes the address to be written/read
	 * 
	 * @param bytes_high most significant byte of the address
	 * @param bytes_low least significant byte of the address
	 * @param writeFlash boolean indicating if it should be written to flash memory
	 * or EEPROM. True = flash. False = EEPROM 
	 * 
	 * @return an byte array with the response from the selected device on the format
	 * [Resp_STK_INSYNC, data, Resp_STK_OK] or 
	 * [Resp_STK_NOSYNC] (If no Sync_CRC_EOP received). If the response does not
	 * match any of the above, something went wrong and the method returns null.
	 * The caller should then retry.
	 */
	private byte[] readPage(byte bytes_high, byte bytes_low, boolean writeFlash) {
		
		byte[] readCommand = new byte[5];
		byte[] in = new byte[3];
		byte memtype;
		
		if (writeFlash) memtype = (byte)'F';
		else memtype = (byte)'E';
		
		readCommand[0] = ConstantsStk500v1.STK_READ_PAGE;
		readCommand[1] = bytes_high;
		readCommand[2] = bytes_low;
		readCommand[3] = memtype;
		readCommand[4] = ConstantsStk500v1.CRC_EOP;
		
		try {
			output.write(readCommand);
		} catch (IOException e) {
			logger.debugTag("Could not write output read command in readPage");
			e.printStackTrace();
		}
		
		int numberOfBytes = 0;
		
		try {
			input.read(in);
		} catch (IOException e) {
			logger.debugTag("Could not read input in readPage");
			e.printStackTrace();
		}
		
		if (numberOfBytes == 3 && in[0] == ConstantsStk500v1.STK_INSYNC &&
				in[2] == ConstantsStk500v1.STK_OK) return in;
		
		else if (numberOfBytes == 1 && in[0] == ConstantsStk500v1.STK_NOSYNC) return in;
		
		//If the method does not return in one of the above, something went wrong
		return null;
	}

	/**
	 * Read one byte from EEPROM memory.
	 * 
	 * @return an byte array with the response from the selected device on the format
	 * [Resp_STK_INSYNC, data, Resp_STK_OK] or 
	 * [Resp_STK_NOSYNC] (If no Sync_CRC_EOP received). If the response does not
	 * match any of the above, something went wrong and the method returns null.
	 * The caller should then retry.
	 */
	private byte[] readDataMemory() {
		
		byte[] readCommand = new byte[2];
		byte[] in = new byte[3];
		
		readCommand[0] = ConstantsStk500v1.STK_READ_DATA;
		readCommand[1] = ConstantsStk500v1.CRC_EOP;
		
		try {
			output.write(readCommand);
		} catch (IOException e) {
			logger.debugTag("Could not write output read command in readDataMemory");
			e.printStackTrace();
		}
		
		int numberOfBytes = 0;
		
		try {
			numberOfBytes = input.read(in);
		} catch (IOException e) {
			logger.debugTag("Could not read input in readDataMemory");
			e.printStackTrace();
		}
		
		if (numberOfBytes == 3 && in[0] == ConstantsStk500v1.STK_INSYNC && 
				in[2] == ConstantsStk500v1.STK_OK) return in;
		
		else if (numberOfBytes == 1 && in[0] == ConstantsStk500v1.STK_NOSYNC) return in;
		
		//If the method does not return in one of the above, something went wrong
		return null;
	}

	/**
	 * Read one word from FLASH memory.
	 * 
	 * @return an byte array with the response from the selected device on the format
	 * [Resp_STK_INSYNC, flash_low, flash_high, Resp_STK_OK] or 
	 * [Resp_STK_NOSYNC] (If no Sync_CRC_EOP received). If the response does not
	 * match any of the above, something went wrong and the method returns null.
	 * The caller should then retry.
	 */
	private byte[] readFlashMemory() {

		byte[] readCommand = new byte[2];
		byte[] in = new byte[4];

		readCommand[0] = ConstantsStk500v1.STK_READ_FLASH;
		readCommand[1] = ConstantsStk500v1.CRC_EOP;

		try {
			output.write(readCommand);
		} catch (IOException e) {
			logger.debugTag("Could not write output read command in readFlashMemory");
			e.printStackTrace();
		}

		int numberOfBytes = 0;
		try {
			numberOfBytes = input.read(in);
		} catch (IOException e) {
			logger.debugTag("Could not read input in readFlashMemory");
			e.printStackTrace();
		}

		if (numberOfBytes == 4 && in[0] == ConstantsStk500v1.STK_INSYNC 
				&& in[3] == ConstantsStk500v1.STK_OK) return in;

		else if (numberOfBytes == 1 && in[0] == ConstantsStk500v1.STK_NOSYNC) return in;

		//If the method does not return in one of the above, something went wrong
		return null;
	}

	/**
	 * Check input from the Arduino.
	 * Uses checkInput(boolean checkCommand, byte command) internally
	 * @return true if response is STK_INSYNC and STK_OK, false if not
	 */
	private boolean checkInput() {
		return checkInput(false, (byte) 0);

	}

	/**
	 * Method used to get and check input from the Arduino. It reads the input, and
	 * check whether the response is STK_INSYNC and STK_OK, or STK_NOSYNC. If the
	 * response is STK_INSYNC and STK_OK the operation was successful. If not 
	 * something went wrong. <br><br>
	 * 
	 * If only STK_INSYNC and STK_OK is supposed to be returned from this method,
	 * use {@link #checkInput()}
	 * 
	 * @param checkComman boolean used set if it is possible that this method
	 * returns something else than STK_INSYNC and STK_OK. If this is possible,
	 * set checkCommand to true.
	 * @param command byte used to identify what command is sent to the connected
	 * device. Only used if checkCommand is true.
	 * 
	 * @return true if response is STK_INSYNC and STK_OK, false if not.
	 */
	private boolean checkInput(boolean checkCommand, byte command) {

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

			//if this is a command expected to return other things in addition to sync and ok:
			if (checkCommand) {
				switch (command) {
				case ConstantsStk500v1.STK_ENTER_PROGMODE : {
					if (byteInput == ConstantsStk500v1.STK_NODEVICE) {
						logger.debugTag("Error entering programming mode: Programmer not found");
						//impossible to recover from
						throw new RuntimeException("STK_NODEVICE returned");
					} else if (byteInput == ConstantsStk500v1.STK_OK) {
						return true;
					} else {
						return false;
					}
				}
				default : {
					throw new IllegalArgumentException("Unhandled argument:" + command);
				}
				}

			} else {
				if (byteInput == ConstantsStk500v1.STK_OK) {

					//Two bytes sent. Response OK. Return true
					return true;
				}
				logger.debugTag("Reponse was STK_INSYNC but not STK_OK in checkInput");
				return false;
			}
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
