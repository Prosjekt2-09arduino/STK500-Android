package com.example.simpleandroidarduinobtconnection;

import no.group09.stk500.Logger;
import android.content.Context;
import android.widget.Toast;

public class Log implements Logger {

	Context ctxt;
	MainActivity main;
	
	public Log(MainActivity main, Context ctxt){
		this.ctxt = ctxt;
		this.main = main;
	}
	
	/** prints a msg on the UI screen **/
	public void makeToast(String msg){
		final String out = msg;
		main.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				Toast.makeText(ctxt, out, Toast.LENGTH_SHORT).show();
				
			}
		});
	}
	
	public void printToConsole(String msg){
		final String out = msg;
		main.runOnUiThread(new Runnable() {
			
			@Override
			public void run() {
				main.printToConsole(out);
			}
		});
	}
	
	public void debugTag(String msg) {
		android.util.Log.d("BT-for-STK", msg);
	}

}
