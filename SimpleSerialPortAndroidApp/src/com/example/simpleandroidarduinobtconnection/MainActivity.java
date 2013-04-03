package com.example.simpleandroidarduinobtconnection;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;
import java.util.UUID;
import java.util.concurrent.Executor;

import no.group09.stk500_v1.Logger;
import no.group09.stk500_v1.STK500v1;

import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnFocusChangeListener;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

	private TextView input, console;
	private Button send, connect, execute;
	private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
	private BluetoothAdapter btAdapter = null;
	private BluetoothSocket btSocket = null;
	private OutputStream outStream = null;
	private InputStream inputStream = null;
	//	private static String address = "04:C0:6F:03:FE:7B";	//HUAWEI
	//private static String address = "00:06:66:07:AF:93";	//ArduinoBT
	private static String address = "00:13:02:20:93:35"; //iJacketClone
	private String text = "";
	Context ctx;
	Logger log;
	
	private Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
		}
	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		input = (TextView) findViewById(R.id.input);
		console = (TextView) findViewById(R.id.console);
		send = (Button) findViewById(R.id.send);
		connect = (Button) findViewById(R.id.connect);
		execute = (Button) findViewById(R.id.execute);
	
		ctx = getBaseContext();
		log = new Log(this, ctx);
		android.util.Log.d("BT-for-STK","App ready to connect");
		
		btAdapter = BluetoothAdapter.getDefaultAdapter();
		
		initializeExecuteButton();
		initializeSendButton();
		initialiseConnectButton();
		checkBTState();
	}
	
	private void initializeExecuteButton(){
		execute.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				new Thread(new Runnable() {

					@Override
					public void run() {
						byte[] binaryFile = new byte[10];
						STK500v1 p = new STK500v1(outStream, inputStream, log, binaryFile);
						log.debugTag("Protocol code stopped");
						log.printToConsole("Protocol code stopped");
						handler.sendEmptyMessage(0);
					}
					
				}).start();
			}
		});
	}
	
	private void initializeSendButton(){
		send.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				sendData(input.getText() + "");
				input.setText("");
			}
		});
	}
	
	private void initialiseConnectButton(){
		connect.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				BluetoothDevice device = btAdapter.getRemoteDevice(address);

				try {
					btSocket = createBluetoothSocket(device);
				} catch (IOException e1) {
					log.printToConsole("\n Failed to create BT socket");
				}

				btAdapter.cancelDiscovery();

				log.printToConsole("\n...Connecting...");

				try {
					btSocket.connect();
					log.printToConsole("\n... Connection OK...");
				} catch (IOException e) {
					try {
						btSocket.close();
					} catch (IOException e2) {
						log.printToConsole("\n Failed to close socket");
					}
				}

				try {
					outStream = btSocket.getOutputStream();
				} catch (IOException e) {
					log.printToConsole("\n...Output stream creating failed...");
				}
				try {
					inputStream = btSocket.getInputStream();
				} catch (IOException e) {
					log.printToConsole("\n...Input stream creation failed...");
				}
				log.printToConsole("\n...Sockets created...");
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
		if(Build.VERSION.SDK_INT >= 10){
			try {
				final Method  m = device.getClass().getMethod("createInsecureRfcommSocketToServiceRecord", new Class[] { UUID.class });
				return (BluetoothSocket) m.invoke(device, MY_UUID);
			} catch (Exception e) {

				log.printToConsole("\n Couldnt create insecure RFComm connection");
			}
		}
		return  device.createRfcommSocketToServiceRecord(MY_UUID);
	}

	@Override
	public void onResume() {
		super.onResume();
	}

	@Override
	public void onPause() {
		super.onPause();
		if (outStream != null) {
			try {
				outStream.flush();
			} catch (IOException e) {
			}
		}
		try     {
			btSocket.close();
		} catch (IOException e2) {
		}
	}

	private void checkBTState() {
		if(btAdapter==null) { 
		} else {
			if (btAdapter.isEnabled()) {
			} else {
				Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
				startActivityForResult(enableBtIntent, 1);
			}
		}
	}

	private void sendData(String message) {
		byte[] msgBuffer = message.getBytes();

		try {
			outStream.write(msgBuffer);
		} catch (IOException e) {
			String msg = "";
			if (address.equals("00:00:00:00:00:00")) 
				msg += "\nCheck that the SPP UUID: \n" + MY_UUID.toString() + "\nexists on server.";

			log.printToConsole(msg);   
		}
	}

	//	public static void printToConsoleStatic(MainActivity c, String msg){
	//		c.printToConsole(msg);
	//	}
	//	
	public void printToConsole(String msg){
		text += msg;
		console.setText(text);
	}


}
