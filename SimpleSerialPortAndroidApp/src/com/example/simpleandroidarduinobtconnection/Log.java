package com.example.simpleandroidarduinobtconnection;

import android.content.Context;
import android.widget.Toast;

public class Log {

	Context ctxt;
	MainActivity main;
	
	public Log(MainActivity main, Context ctxt){
		this.ctxt = ctxt;
		this.main = main;
	}
	
	/** prints a msg on the UI screen **/
	public void println(String msg){
		Toast.makeText(ctxt, msg, Toast.LENGTH_SHORT).show();
	}
	
	public void printToConsole(String msg){
		
		main.printToConsole(msg);
	}

}
