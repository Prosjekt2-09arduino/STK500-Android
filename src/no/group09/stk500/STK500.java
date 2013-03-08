package no.group09.stk500;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;

public class STK500 {
	
	private BufferedOutputStream output;
	private BufferedInputStream input;
	
	public STK500 (BufferedOutputStream output, BufferedInputStream input) {
		this.output = output;
		this.input = input;
	}
	
	public void send(byte[] data) {
		
	}
	
	private byte[] read() {
		return null;
	}
}
