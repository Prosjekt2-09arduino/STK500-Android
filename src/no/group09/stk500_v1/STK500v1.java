package no.group09.stk500_v1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.concurrent.TimeoutException;

public class STK500v1 {
	private OutputStream output;
	private InputStream input;
	private Logger logger;
	private ReadWrapper readWrapper;
	private Thread readWrapperThread;
	/**Used to prevent stack overflow**/
	private int syncStack = 0;
	private int programPageTries = 0;

	/** Used to interact with the binary file */
	private Hex hexParser;

	public STK500v1 (OutputStream output, InputStream input, Logger log, byte[] binary) {
		
		this.hexParser = new Hex(binary, log);
		
		this.output = output;
		this.input = input;
		this.logger = log;
		logger.logcat("STKv1 constructor: Initializing protocol code", "v");

		long startTime;
		long endTime;
		
		readWrapper = new ReadWrapper(input, log);
		readWrapperThread = new Thread(readWrapper);
		readWrapperThread.start();
		while (!readWrapper.checkIfStarted()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				//nothing needs doing
			}
		}
		logger.logcat("STKv1 constructor: ReadWrapper should be started now", "v");
		

		log.logcat("STKv1 constructor: Initializing programmer", "v");
		//try to get programmer version
		
		startTime = System.currentTimeMillis();
		String version = checkIfStarterKitPresent();
		endTime = System.currentTimeMillis();
		
		logger.logcat("STKv1 constructor: checkIfStarterKitPresent took: " + 
				(endTime-startTime) + " ms", "v");
		
		log.logcat("STKv1 constructor: " + version, "d");
		log.printToConsole(version);
		if (!version.equals("Arduino")) {
			readWrapper.terminate();
			while(readWrapperThread.isAlive()) {
				try {
					logger.logcat("STKv1 constructor: readWrapperThread is alive", "v");
					Thread.sleep(100);
				} catch (InterruptedException e) {
				}
			}
			return;
		};
		
		boolean entered;
		startTime = System.currentTimeMillis();
		for (int i = 0; i < 10; i++) {
			logger.logcat("STKv1 constructor: Number of tries: " + i, "v");
			
			entered = enterProgramMode();
			endTime = System.currentTimeMillis();
			
			logger.logcat("STKv1 constructor: enterProgramMode took: " +
					(endTime-startTime) + " ms", "v");
			
			if (entered) {
				long now = System.currentTimeMillis();
				
				int syncFails = 0;
				int syncOk = 0;
				logger.logcat("STKv1 constructor: Spam sync to stay in programming mode.", "v");
				while(System.currentTimeMillis() - now < 500) {
					if(!getSynchronization()) {
						logger.logcat("STKv1 constructor: Sync gave up...", "w");
						syncFails++;
					}
					else {
						syncOk++;
					}
				}
				
				logger.logcat("STKv1 constructor: OK: " + syncOk + ", fails: " + syncFails, "v");
				
				boolean loadOk = false;
				boolean readOk = false;
				byte[] readPage = null;
				
				for (int j = 0; j < 10; j++) {
					if(loadAddress(0)) {
						loadOk = true;
						
						readPage = readPage((byte)0,(byte)0,true);
						if(readPage!=null) {
							readOk = true;
							logger.logcat("STKv1 constructor: readPage not null: "
									+ Arrays.toString(readPage), "d");
				break;
			}
		}
				}
		
				logger.logcat("STKv1 constructor: Starting to write.", "v");
				uploadFile();
				
				logger.logcat("STKv1 constructor: Loading: " + loadOk + "," +
						"Reading: "+ readOk, "v");
				
				if(readOk) {
					logger.logcat("STKv1 constructor: Read page result: " +
							Arrays.toString(readPage), "v");
				}
				
				logger.logcat("STKv1 constructor: The ardunino has entered " +
						"programming mode. Trying to leave...", "i");
				for (int j = 0; j < 10; j++) {
					if(leaveProgramMode()) {
						logger.logcat("STKv1 constructor: The arduino has now " +
								"left programming mode.", "i");
						break;
					}
					if(j>2) {
						logger.logcat("STKv1 constructor: Giving up on leaving " +
								"programming mode.", "i");
						break;
					}
				}
				break;
			}
		}
		
		//shut down readWrapper
		readWrapper.terminate();
	}

	/**
	 * Attempt to handshake with the Arduino. The method is modified to account for
	 * the Optiboot loader not returning a version string, just the
	 * in sync and OK bytes.
	 * @return -1 on failure and Arduino otherwise
	 */
	private String checkIfStarterKitPresent() {
		logger.logcat("checkIfStarterKitPresent: Detect programmer", "v");
		String version = "";

		//Send request
		try {
			byte[] out = new byte[] {
					ConstantsStk500v1.STK_GET_SIGN_ON, ConstantsStk500v1.CRC_EOP
			};
			output.write(out);
			logger.logcat("checkIfStarterKitPresent: Sending bytes to get " +
					"starter kit: " + Arrays.toString(out), "d");
		} catch (IOException e) {
			logger.logcat("checkIfStarterKitPresent: Communication problem: " +
					"Can't send request for programmer version", "i");
			return "-1";
		}

		//Read response
		try {
			char[] response = new char[7];
			int responseIndex = 0;
			int readResult = 0;
			byte readByte;
			while (readResult >= 0) {
				readResult = read(TimeoutValues.CONNECT);
				if (readResult == -1) {
					//TODO: Discover when/if this happens. Separate genuine end of
					//stream from job not accepted.
					logger.logcat("checkIfStarterKitPresent: End of stream encountered", "d");
					break;
				}
				readByte = (byte) readResult;
				logger.logcat("checkIfStarterKitPresent: Read byte: " + readByte, "v");
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
					logger.logcat("checkIfStarterKitPresent: Unable to synchronize", "d");
					break;
				} else {
					logger.logcat("checkIfStarterKitPresent: Not terminated by STK_OK!", "v");
					break;
				}
			}
		} catch (TimeoutException e) {
			logger.logcat("checkIfStarterKitPresent: Timeout in checkIfStarterkitPresent!", "d");
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

		while (tries < 10) {
			tries++;

			try {
				output.write(getSyncCommand);
			} catch (IOException e) {
				logger.logcat("getSynchronization: Unable to write output in " +
						"getSynchronization", "i");
				e.printStackTrace();
				return false;
			}
			//If the response is valid, return. If not, continue
			if (checkInput()) {
				logger.logcat("getSynchronization: Sync achieved after " + 
						(tries+1) + " tries.", "d");
				syncStack = 0;
				return true;
			}
		}
		return false;
	}

	/*
	 * TODO: Fill or remove these methods
	 */
	private void getParameterValue() {
	}
	private void setParameterValue() {
	}
	private void setDeviceProgrammingParameters() {
	}
	private void programLockBits() {
	}
	private void readLockBits() {
	}
	private void readSignatureBits() {
	}
	private void readOscillatorCalibrationByte() {
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
		logger.logcat("enterProgramMode: Sending bytes to enter programming mode: "
				+ Arrays.toString(command), "d");
		try {
			output.write(command);
		} catch (IOException e) {
			logger.logcat("enterProgramMode: Communication problem on sending" +
					"request to enter programming mode", "i");
			return false;
		}

		//check response
		boolean ok = checkInput(true, ConstantsStk500v1.STK_ENTER_PROGMODE, TimeoutValues.CONNECT);
		if (!ok) {
			logger.logcat("enterProgramMode: Unable to enter programming mode", "d");
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
			logger.logcat("leaveProgramMode: Communication problem on leaving" +
					"programming mode", "i");
		}

		//check response
		boolean ok = checkInput();
		if (!ok) {
			logger.logcat("leaveProgramMode: Unable to leave programming mode", "d");
		}
		return ok;
	}

    /**
     * Erase the device to prepare for programming
     * @return true if successful.
     */
    private boolean chipErase() {
        byte[] command = new byte[]{ConstantsStk500v1.STK_CHIP_ERASE, ConstantsStk500v1.CRC_EOP};

        try {
            output.write(command);
        } catch (IOException e) {
            logger.logcat("chipErase: Communication problem on chip erase.", "v");
            return false;
        }

        boolean ok = checkInput();
        if (!ok) {
            logger.logcat("chipErase: No sync. EOP not recieved for chip erase.", "v");
        }
        return ok;
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
			logger.logcat("checkForAddressAutoincrement: Unable to write output " +
					"in checkForAddressAutoincrement", "i");
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

		logger.logcat("loadAddress: Sending bytes to load address: " + 
				Arrays.toString(loadAddr), "d");
		try {
			output.write(loadAddr);
		} catch (IOException e) {
			logger.logcat("loadAddress: Unable to write output in loadAddress", "i");
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

		//TODO: Add call to this method
		
		byte[] programCommand = new byte[3];

		programCommand[0] = ConstantsStk500v1.STK_PROG_DATA;
		programCommand[1] = data;
		programCommand[2] = ConstantsStk500v1.CRC_EOP;

		try {
			output.write(programCommand);
		} catch (IOException e) {
			logger.logcat("programDataMemory: Could not write output in " +
					"programDataMemory", "i");
			e.printStackTrace();
			return false;
		}

		return checkInput();
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
	 * 
	 * @return true if response is STK_INSYNC and STK_OK, false if not.
	 */
	private boolean programPage(byte bytes_high, byte bytes_low, boolean writeFlash, byte[] data) {

		byte[] programPage = new byte[4+data.length];
		byte memtype;

		if (writeFlash) memtype = (byte)'F';
		else memtype = (byte)'E';

		programPage[0] = ConstantsStk500v1.STK_PROG_PAGE;
		programPage[1] = bytes_high;
		programPage[2] = bytes_low;
		programPage[3] = memtype;

		logger.logcat("programPage: Length of data to program: " + data.length, "d");

		//Put all the data together with the rest of the command
		for (int i = 4; i < data.length; i++) {
			programPage[i] = data[i-4];
		}
		
		programPage[data.length] = ConstantsStk500v1.CRC_EOP;

		try {
			output.write(programPage);
		} catch (IOException e) {
			logger.logcat("programPage: Could not write output in programDataMemory", "i");
			e.printStackTrace();
			return false;
	}

		return checkInput();
	}

	/**
	 * Read a block of data from FLASH or EEPROM of the current device. The data
	 * block size should not be larger than 256 bytes. bytes_high and bytes_low
	 * are part of an integer that describes the address to be written/read
	 * 
	 * @param bytes_high most significant byte of block size
	 * @param bytes_low least significant byte of block size
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
		byte[] buffer = new byte[18];
		byte memtype;
		
		if (writeFlash) memtype = (byte)'F';
		else memtype = (byte)'E';
		
		readCommand[0] = ConstantsStk500v1.STK_READ_PAGE;
		readCommand[1] = bytes_high;
		readCommand[2] = bytes_low;
		readCommand[3] = memtype;
		readCommand[4] = ConstantsStk500v1.CRC_EOP;
		
		logger.logcat("readPage: Sending bytes to readPage: " + 
				Arrays.toString(readCommand), "v");
		
		try {
			output.write(readCommand);
		} catch (IOException e) {
			logger.logcat("readPage: Could not write output read command in " +
					"readPage", "i");
			e.printStackTrace();
		}
		
		int numberOfBytes = 0;
		
		try {
			numberOfBytes = read(buffer, TimeoutValues.READ);
		} catch (TimeoutException e) {
			logger.logcat("readPage: Timed out when reading page", "w");
		}
		
		logger.logcat("readPage: readPage buffer: " + Arrays.toString(buffer), "v");
		
		if (numberOfBytes > 2 && buffer[0] == ConstantsStk500v1.STK_INSYNC &&
				buffer[numberOfBytes-1] == ConstantsStk500v1.STK_OK) {
			return buffer;
		}
		
		else if (numberOfBytes == 1 && buffer[0] == ConstantsStk500v1.STK_NOSYNC) {
			return null;
		}
		
		logger.logcat("readPage: readPage didn't receive anything!", "e");
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
		
		//TODO: Add call to this method
		
		byte[] readCommand = new byte[2];
		byte[] in = new byte[3];
		
		readCommand[0] = ConstantsStk500v1.STK_READ_DATA;
		readCommand[1] = ConstantsStk500v1.CRC_EOP;
		
		try {
			output.write(readCommand);
		} catch (IOException e) {
			logger.logcat("readDataMemory: Could not write output read command " +
					"in readDataMemory", "i");
			e.printStackTrace();
		}
		
		int numberOfBytes = 0;
		
		try {
			numberOfBytes = input.read(in);
		} catch (IOException e) {
			logger.logcat("readDataMemory: Could not read input", "i");
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

		//TODO: Add call to this method
		
		byte[] readCommand = new byte[2];
		byte[] in = new byte[4];

		readCommand[0] = ConstantsStk500v1.STK_READ_FLASH;
		readCommand[1] = ConstantsStk500v1.CRC_EOP;

		try {
			output.write(readCommand);
		} catch (IOException e) {
			logger.logcat("readFlashMemory: Could not write output read command " +
					"in readFlashMemory", "i");
			e.printStackTrace();
		}

		int numberOfBytes = 0;
		try {
			numberOfBytes = input.read(in);
		} catch (IOException e) {
			logger.logcat("readFlashMemory: Could not read input in readFlashMemory", "i");
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
		return checkInput(false, (byte) 0, TimeoutValues.DEFAULT);

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
	 * @param timeout 
	 * 
	 * @return true if response is STK_INSYNC and STK_OK, false if not.
	 */
	private boolean checkInput(boolean checkCommand, byte command, TimeoutValues timeout) {

		int intInput = -1;
		
		try {
			intInput = read(timeout);
//			intInput = input.read();
		} catch (TimeoutException e) {
			logger.logcat("checkInput: Timeout!", "d");
		}

		if (intInput == -1) {
			logger.logcat("checkInput: End of stream encountered", "d");
			return false;
		}

		byte byteInput;

		if (intInput == ConstantsStk500v1.STK_INSYNC){
			try {
				intInput = read(timeout);
//				intInput = input.read();
			} catch (TimeoutException e) {
				logger.logcat("checkInput: Timeout!", "d");
			}

			if (intInput == -1) {
				logger.logcat("checkInput: End of stream encountered", "d");
				return false;
			}
			
			//Input is not equal to -1. Cast to byte
			byteInput = (byte)intInput;

			//if this is a command expected to return other things in addition to sync and ok:
			if (checkCommand) {
				switch (command) {
				case ConstantsStk500v1.STK_ENTER_PROGMODE : {
					if (byteInput == ConstantsStk500v1.STK_NODEVICE) {
						logger.logcat("checkInput: Error entering programming " +
								"mode: Programmer not found", "w");
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
				logger.logcat("checkInput: Reponse was STK_INSYNC but not STK_OK", "d");
				return false;
			}
		}
		else {
			if(syncStack>2) {
				logger.logcat("checkInput: Avoid stack overflow, not in sync!", "d");
				return false;
			}
			logger.logcat("checkInput: Response was not STK_INSYNC, attempting " +
					"synchronization.", "d");
			syncStack++;
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

		//TODO: Add call to this method
		
		byte[] uploadFile = new byte[4];

		uploadFile[0] = ConstantsStk500v1.STK_PROG_FLASH;
		uploadFile[1] = flash_low;
		uploadFile[2] = flash_high;
		uploadFile[3] = ConstantsStk500v1.CRC_EOP;

		try {
			output.write(uploadFile);
		} catch (IOException e) {
			logger.logcat("programFlashMemory: Unable to write output in programFlashMemory", "i");
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
		//TODO: Add call to this method

		//These variables is used if the last part of this method is uncommented

		//Get the total length of the hex-file in number of lines.
		int hexLength = hexParser.getLines();
		// Counter used to keep the position in the hex-file
		int hexPosition = 0;

		//Run through the entire hex file, ignoring the last line
		while (hexPosition < hexLength) {
			
			if (hexPosition > hexLength){
				logger.logcat("uploadFile: End of file. Upload finished with success.", "v");
				//TODO: Add proper ending here.
				return;
			}
			else if(programPageTries>5) {
				logger.logcat("uploadFile: Could not write from line " + hexPosition, "w");
				break;
			}
			
			//Fetch the next line to be written from the hex-file
			byte[] nextLine = hexParser.getHexLine(hexPosition);
			//Find the length of the data field in this line
			int dataLength = decodeByte(nextLine[0]);
			//Byte array used to store data only
			byte[] hexData = new byte[dataLength];
			//Store data from the next line in the hex-file in a separate array
			for (int i = 3; i < dataLength; i++) {
				hexData[i] = nextLine[i]; 
			}

			logger.logcat("uploadFile: Trying to write data from line " + hexPosition + " from hex file.", "v");
			boolean programPageSuccess = programPage(nextLine[1], nextLine[2], true, hexData);
			
			//Programming of page was successful. Increment counter and program
			//next page
			if (programPageSuccess) {
				hexPosition++;
				continue;
			}
			//Programming was unsuccessful. Try again without incrementing
			else {
				programPageTries++;
				logger.logcat("uploadFile: Not able to program page. Retrying " +
						"with same page", "d");
				continue;
			}
			
			//This code section is used to program the flash memory. The previous
			//section has been changed to fit programPage, so small changes has
			//to be made if one want to use programFlashMemory() instead.
			/*
			//If i <= binary.length, fetch the two next bytes from the binary array
			if (hexPosition + 1 <= hexLength) {
				//Fetch the two next bytes in the binary array
				bytes_low = binary[hexPosition];
				bytes_high = binary[hexPosition+1];
			}
			//Fetch the last byte in the binary array
			else {
				bytes_low = binary[hexPosition];

				//FIXME: The low byte is now the last element in the binary array. What to do?
				bytes_high = 0;
			}

			//Program the flash and store the result
			boolean programFlashSuccess = programFlashMemory(bytes_low, bytes_high);

			if (programFlashSuccess) {
				//Increment position in binary array
				hexPosition += 2;
				//Two bytes sent. Response OK. Repeat.
				continue;
			}
			else if (!programFlashSuccess) {
				//Not able to program flash. Retry.
				logger.debugTag("programFlashMemory returned false. Unable to program flash. Retrying");
				continue;
			}
			*/
		}
	}

	/**
	 * Read two unsigned bytes into an integer
	 * @param high Most significant byte
	 * @param low  Least significant byte
	 * @return
	 */
	private static int unPackTwoBytes(byte high, byte low) {
		
		//TODO: Add call to this method/remove
		
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
	
	/**
	 * Reads a single byte, will be interrupted after a while
	 * @param timeout The selected timeout enumeration chosen. Used to determine
	 * timeout length.
	 * @return -1 if end of stream encountered, otherwise 0-255
	 * @throws TimeoutException 
	 */
	private int read(TimeoutValues timeout) throws TimeoutException {
		return read(null, timeout);
	}
	
	/**
	 * Will attempt to fill the entire buffer, if unable to fill it the number of read
	 * bytes will be returned.
	 * @param buffer Array of bytes to store the read bytes
	 * @param timeout The selected timeout enumeration chosen. Used to determine
	 * timeout length.
	 * @return -1 if end of stream encountered, otherwise the number of bytes read
	 * @throws TimeoutException 
	 */
	private int read(byte[] buffer, TimeoutValues timeout) throws TimeoutException {
		long now = System.currentTimeMillis();
		if (!readWrapper.canAcceptWork()) {
			logger.logcat("read: Readwrapper wasn't ready to accept work", "v");
			return -1;
		}
		boolean accepted;
		
		//Check if single or buffered reading should be used
		if(buffer!=null) {
			logger.logcat("read: Buffer not null", "v");
			accepted = readWrapper.requestReadIntoBuffer(buffer);
		}
		else {
			accepted = readWrapper.requestReadByte();
		}
		
		if (!accepted) {
			logger.logcat("read: Job not accepted by wrapper", "d");
			return -1;
		}
		logger.logcat("read: Job accepted by wrapper", "v");
		//ask if reading is done
		while (!readWrapper.isDone()) {
			if (System.currentTimeMillis() >= now + timeout.getTimeout()) {
				if (readWrapper.checkIfFailed()) {
					logger.logcat("read: The wrapper failed, probably IOException.", "d");
					return -1;
				}
				logger.logcat("read: Timed out, cancelling request...", "d");
				readWrapper.cancelRequest();
				throw new TimeoutException("Reading timed out");
			}
		}
		logger.logcat("read: Wrapper reported job as complete", "v");
		return readWrapper.getResult();
	}
	
	/**
	 * Standard timeout values for how long to wait for reading results.
	 */
	private enum TimeoutValues{
		DEFAULT(1000),
		CONNECT(3000),
		READ(5000),
		WRITE(1000);
		
	    private final long timeout;

	    private TimeoutValues(long t)
	    {
	        timeout = t;
			}
		
		/**
	     * Get the milliseconds assigned to the timeout
	     * @return timeout as a long in milliseconds
		 */
	    public long getTimeout() {
	    	return timeout;
				}
			}
		
}
