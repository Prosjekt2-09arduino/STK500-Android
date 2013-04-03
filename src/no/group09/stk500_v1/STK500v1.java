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
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			int response = -1;
			try {
				response = input.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (response == -1) {
				logger.debugTag("End of stream encountered in getSynchronization");
				break;
			}
			
			else {
				byte byteResponse = (byte) response;
				
				if (byteResponse == ConstantsStk500v1.STK_INSYNC) {
					try {
						response = input.read();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					
					if (response == -1) {
						logger.debugTag("End of stream encountered in getSynchronization");
						break;
					}
					//Input is not equal to -1. Cast to byte
					byteResponse = (byte)response;
					
					if (byteResponse == ConstantsStk500v1.STK_OK) {
						logger.debugTag("Back in sync. Returning");
						return true;
					}
				}
			}
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
			
			uploadFile[0] = ConstantsStk500v1.STK_PROG_FLASH;
			uploadFile[1] = flash_low;
			uploadFile[2] = flash_high;
			uploadFile[3] = ConstantsStk500v1.CRC_EOP;
			
			try {
				output.write(uploadFile);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			int intInput = -1;
			try {
				intInput = input.read();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			if (intInput == -1) {
				logger.debugTag("End of stream encountered in uploadFile");
				break;
			}
			
			//Input is not equal to -1. Cast to byte
			byte byteInput = (byte)intInput;
			
			if (intInput == ConstantsStk500v1.STK_INSYNC){
				try {
					intInput = input.read();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				if (intInput == -1) {
					logger.debugTag("End of stream encountered in uploadFile");
					break;
				}
				//Input is not equal to -1. Cast to byte
				byteInput = (byte)intInput;
				
				if (byteInput == ConstantsStk500v1.STK_OK) {
					//Increment position in binary array
					i += 2;
					//Two bytes sent. Respons OK. Repeat.
					continue;
				}
			}
			else if (intInput == ConstantsStk500v1.STK_NOSYNC) {
				logger.debugTag("Response was STK_NOSYNC in uploadFile");
				//Get synchronization
				getSynchronization();
				//Synchronization is in place. Try again.
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
