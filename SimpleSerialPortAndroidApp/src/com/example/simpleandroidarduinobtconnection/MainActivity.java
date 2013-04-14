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
//	private static String address = "00:06:66:07:AF:93";	//ArduinoBT
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
//						byte[] binaryFile;
						
						// arduinoSimple
						String hexData = 
								"3A100000000C9461000C947E000C947E000C947E0095" +
										"3A100010000C947E000C947E000C947E000C947E0068" +
										"3A100020000C947E000C947E000C947E000C947E0058" +
										"3A100030000C947E000C947E000C947E000C947E0048" +
										"3A100040000C949A000C947E000C947E000C947E001C" +
										"3A100050000C947E000C947E000C947E000C947E0028" +
										"3A100060000C947E000C947E00000000002400270009" +
										"3A100070002A0000000000250028002B0000000000DE" +
										"3A1000800023002600290004040404040404040202DA" +
										"3A100090000202020203030303030301020408102007" +
										"3A1000A0004080010204081020010204081020000012" +
										"3A1000B0000007000201000003040600000000000029" +
										"3A1000C000000011241FBECFEFD8E0DEBFCDBF11E08E" +
										"3A1000D000A0E0B1E0E4E3F4E002C005900D92A030AE" +
										"3A1000E000B107D9F711E0A0E0B1E001C01D92A9303D" +
										"3A1000F000B107E1F70E9409020C9418020C94000069" +
										"3A100100008DE061E00E94B50164E670E080E090E07F" +
										"3A100110000E94E2008DE060E00E94B50164E670E0BC" +
										"3A1001200080E090E00E94E20008958DE061E00E948E" +
										"3A10013000760108951F920F920FB60F9211242F93FC" +
										"3A100140003F938F939F93AF93BF93809104019091BE" +
										"3A100150000501A0910601B0910701309108010196B7" +
										"3A10016000A11DB11D232F2D5F2D3720F02D57019696" +
										"3A10017000A11DB11D209308018093040190930501F6" +
										"3A10018000A0930601B09307018091000190910101B5" +
										"3A10019000A0910201B09103010196A11DB11D8093B0" +
										"3A1001A000000190930101A0930201B0930301BF915C" +
										"3A1001B000AF919F918F913F912F910F900FBE0F9014" +
										"3A1001C0001F9018959B01AC017FB7F89480910001B6" +
										"3A1001D00090910101A0910201B091030166B5A89B25" +
										"3A1001E00005C06F3F19F00196A11DB11D7FBFBA2F49" +
										"3A1001F000A92F982F8827860F911DA11DB11D62E0A0" +
										"3A10020000880F991FAA1FBB1F6A95D1F7BC012DC08B" +
										"3A10021000FFB7F8948091000190910101A091020133" +
										"3A10022000B0910301E6B5A89B05C0EF3F19F0019618" +
										"3A10023000A11DB11DFFBFBA2FA92F982F88278E0FA0" +
										"3A10024000911DA11DB11DE2E0880F991FAA1FBB1FC0" +
										"3A10025000EA95D1F7861B970B885E9340C8F2215030" +
										"3A1002600030404040504068517C4F211531054105D8" +
										"3A10027000510571F60895789484B5826084BD84B583" +
										"3A10028000816084BD85B5826085BD85B5816085BD91" +
										"3A10029000EEE6F0E0808181608083E1E8F0E01082AA" +
										"3A1002A000808182608083808181608083E0E8F0E0EB" +
										"3A1002B000808181608083E1EBF0E0808184608083D5" +
										"3A1002C000E0EBF0E0808181608083EAE7F0E080810C" +
										"3A1002D000846080838081826080838081816080836C" +
										"3A1002E0008081806880831092C1000895CF93DF934E" +
										"3A1002F000482F50E0CA0186569F4FFC0134914A575F" +
										"3A100300005F4FFA018491882369F190E0880F991F6B" +
										"3A10031000FC01E859FF4FA591B491FC01EE58FF4F45" +
										"3A10032000C591D491662351F42FB7F8948C91932FF3" +
										"3A10033000909589238C93888189230BC0623061F466" +
										"3A100340002FB7F8948C91932F909589238C93888163" +
										"3A10035000832B88832FBF06C09FB7F8948C91832B83" +
										"3A100360008C939FBFDF91CF910895482F50E0CA0131" +
										"3A1003700082559F4FFC012491CA0186569F4FFC0174" +
										"3A1003800094914A575F4FFA013491332309F440C0E6" +
										"3A10039000222351F1233071F0243028F42130A1F0D0" +
										"3A1003A000223011F514C02630B1F02730C1F02430CE" +
										"3A1003B000D9F404C0809180008F7703C080918000C1" +
										"3A1003C0008F7D8093800010C084B58F7702C084B584" +
										"3A1003D0008F7D84BD09C08091B0008F7703C080916C" +
										"3A1003E000B0008F7D8093B000E32FF0E0EE0FFF1F91" +
										"3A1003F000EE58FF4FA591B4912FB7F894662321F4DE" +
										"3A100400008C919095892302C08C91892B8C932FBF5E" +
										"3A100410000895CF93DF930E943B010E949500C0E0B6" +
										"3A10042000D0E00E9480002097E1F30E940000F9CF05" +
										"3A04043000F894FFCF6E" +
										"3A00000001FF";
						
						//Convert string to byte array
						byte[] binaryFile = new byte[hexData.length() / 2];
						for(int i=0; i < hexData.length(); i+=2)
						{
							binaryFile[i/2] = Integer.decode("0x" + hexData.substring(i, i + 2)).byteValue();
						}
						
						STK500v1 p = new STK500v1(outStream, inputStream, log, binaryFile);
						log.logcat("initializeExecuteButton: Protocol code stopped", "d");
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
				log.logcat("initialiseConnectButton: Connecting...", "d");

				try {
					btSocket.connect();
					log.logcat("initialiseConnectButton: Connection OK...", "d");
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
				log.logcat("initialiseConnectButton: Sockets created", "d");
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
