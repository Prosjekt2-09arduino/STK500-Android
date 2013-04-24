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
//	private static String address = "00:13:02:20:93:35"; //iJacketClone
	private static String address = "00:06:66:42:9B:C1"; //BTJacket
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
						
//						//blink sketch
//						hexData = "3A100000000C9461000C947E000C947E000C947E0095" +
//								"3A100010000C947E000C947E000C947E000C947E0068" +
//								"3A100020000C947E000C947E000C947E000C947E0058" +
//								"3A100030000C947E000C947E000C947E000C947E0048" +
//								"3A100040000C949A000C947E000C947E000C947E001C" +
//								"3A100050000C947E000C947E000C947E000C947E0028" +
//								"3A100060000C947E000C947E00000000002400270009" +
//								"3A100070002A0000000000250028002B0000000000DE" +
//								"3A1000800023002600290004040404040404040202DA" +
//								"3A100090000202020203030303030301020408102007" +
//								"3A1000A0004080010204081020010204081020000012" +
//								"3A1000B0000007000201000003040600000000000029" +
//								"3A1000C000000011241FBECFEFD8E0DEBFCDBF11E08E" +
//								"3A1000D000A0E0B1E0EAEFF3E002C005900D92A0309D" +
//								"3A1000E000B107D9F711E0A0E0B1E001C01D92A9303D" +
//								"3A1000F000B107E1F70E94F4010C94FB010C9400009D" +
//								"3A100100008DE061E00E949C0168EE73E080E090E089" +
//								"3A100110000E94E2008DE060E00E949C0168EE73E0C6" +
//								"3A1001200080E090E00E94E20008958DE061E00E948E" +
//								"3A10013000760108951F920F920FB60F9211242F93FC" +
//								"3A100140003F938F939F93AF93BF93809104019091BE" +
//								"3A100150000501A0910601B0910701309108010196B7" +
//								"3A10016000A11DB11D232F2D5F2D3720F02D57019696" +
//								"3A10017000A11DB11D209308018093040190930501F6" +
//								"3A10018000A0930601B09307018091000190910101B5" +
//								"3A10019000A0910201B09103010196A11DB11D8093B0" +
//								"3A1001A000000190930101A0930201B0930301BF915C" +
//								"3A1001B000AF919F918F913F912F910F900FBE0F9014" +
//								"3A1001C0001F9018959B01AC017FB7F89480910001B6" +
//								"3A1001D00090910101A0910201B091030166B5A89B25" +
//								"3A1001E00005C06F3F19F00196A11DB11D7FBFBA2F49" +
//								"3A1001F000A92F982F8827860F911DA11DB11D62E0A0" +
//								"3A10020000880F991FAA1FBB1F6A95D1F7BC012DC08B" +
//								"3A10021000FFB7F8948091000190910101A091020133" +
//								"3A10022000B0910301E6B5A89B05C0EF3F19F0019618" +
//								"3A10023000A11DB11DFFBFBA2FA92F982F88278E0FA0" +
//								"3A10024000911DA11DB11DE2E0880F991FAA1FBB1FC0" +
//								"3A10025000EA95D1F7861B970B885E9340C8F2215030" +
//								"3A1002600030404040504068517C4F211531054105D8" +
//								"3A10027000510571F60895789484B5826084BD84B583" +
//								"3A10028000816084BD85B5826085BD85B5816085BD91" +
//								"3A10029000EEE6F0E0808181608083E1E8F0E01082AA" +
//								"3A1002A000808182608083808181608083E0E8F0E0EB" +
//								"3A1002B000808181608083E1EBF0E0808184608083D5" +
//								"3A1002C000E0EBF0E0808181608083EAE7F0E080810C" +
//								"3A1002D000846080838081826080838081816080836C" +
//								"3A1002E0008081806880831092C1000895482F50E07B" +
//								"3A1002F000CA0186569F4FFC0124914A575F4FFA016D" +
//								"3A1003000084918823C1F0E82FF0E0EE0FFF1FE85939" +
//								"3A10031000FF4FA591B491662341F49FB7F8948C9157" +
//								"3A10032000209582238C939FBF08959FB7F8948C915A" +
//								"3A10033000822B8C939FBF0895482F50E0CA018255AD" +
//								"3A100340009F4FFC012491CA0186569F4FFC013491B6" +
//								"3A100350004A575F4FFA019491992309F444C022232C" +
//								"3A1003600051F1233071F0243028F42130A1F02230F3" +
//								"3A1003700011F514C02630B1F02730C1F02430D9F483" +
//								"3A1003800004C0809180008F7703C0809180008F7DB2" +
//								"3A100390008093800010C084B58F7702C084B58F7DB4" +
//								"3A1003A00084BD09C08091B0008F7703C08091B000F8" +
//								"3A1003B0008F7D8093B000E92FF0E0EE0FFF1FEE5825" +
//								"3A1003C000FF4FA591B491662341F49FB7F8948C91A7" +
//								"3A1003D000309583238C939FBF08959FB7F8948C9199" +
//								"3A1003E000832B8C939FBF08950E943B010E94950030" +
//								"3A0A03F0000E948000FDCFF894FFCFBB" +
//								"3A00000001FF"; 
						
						// Reset mode
						String hexData =
								"3A100000000C9461000C947E000C947E000C947E0095" +
								"3A100010000C947E000C947E000C947E000C947E0068" +
								"3A100020000C947E000C947E000C947E000C947E0058" +
								"3A100030000C947E000C947E000C947E000C947E0048" +
								"3A100040000C9490000C947E000C947E000C947E0026" +
								"3A100050000C947E000C947E000C947E000C947E0028" +
								"3A100060000C947E000C947E00000000002400270009" +
								"3A100070002A0000000000250028002B0000000000DE" +
								"3A1000800023002600290004040404040404040202DA" +
								"3A100090000202020203030303030301020408102007" +
								"3A1000A0004080010204081020010204081020000012" +
								"3A1000B0000007000201000003040600000000000029" +
								"3A1000C000000011241FBECFEFD8E0DEBFCDBF11E08E" +
								"3A1000D000A0E0B1E0E0E2F4E002C005900D92A030B3" +
								"3A1000E000B107D9F711E0A0E0B1E001C01D92A9303D" +
								"3A1000F000B107E1F70E94FF010C940E020C9400007E" +
								"3A10010000089584E061E00E946C016CE271E080E09F" +
								"3A1001100090E00E94D80084E060E00E94AB01089566" +
								"3A100120001F920F920FB60F9211242F933F938F932C" +
								"3A100130009F93AF93BF938091040190910501A0918B" +
								"3A100140000601B0910701309108010196A11DB11D72" +
								"3A10015000232F2D5F2D3720F02D570196A11DB11DA6" +
								"3A10016000209308018093040190930501A093060158" +
								"3A10017000B09307018091000190910101A0910201CB" +
								"3A10018000B09103010196A11DB11D809300019093D0" +
								"3A100190000101A0930201B0930301BF91AF919F9120" +
								"3A1001A0008F913F912F910F900FBE0F901F90189538" +
								"3A1001B0009B01AC017FB7F8948091000190910101FF" +
								"3A1001C000A0910201B091030166B5A89B05C06F3FE5" +
								"3A1001D00019F00196A11DB11D7FBFBA2FA92F982F2D" +
								"3A1001E0008827860F911DA11DB11D62E0880F991F00" +
								"3A1001F000AA1FBB1F6A95D1F7BC012DC0FFB7F894A9" +
								"3A100200008091000190910101A0910201B091030140" +
								"3A10021000E6B5A89B05C0EF3F19F00196A11DB11DE1" +
								"3A10022000FFBFBA2FA92F982F88278E0F911DA11DD0" +
								"3A10023000B11DE2E0880F991FAA1FBB1FEA95D1F7F5" +
								"3A10024000861B970B885E9340C8F221503040404097" +
								"3A10025000504068517C4F211531054105510571F61B" +
								"3A100260000895789484B5826084BD84B5816084BD2E" +
								"3A1002700085B5826085BD85B5816085BDEEE6F0E01F" +
								"3A10028000808181608083E1E8F0E01082808182607B" +
								"3A100290008083808181608083E0E8F0E080818160FC" +
								"3A1002A0008083E1EBF0E0808184608083E0EBF0E02C" +
								"3A1002B000808181608083EAE7F0E0808184608083D0" +
								"3A1002C000808182608083808181608083808180687A" +
								"3A1002D00080831092C1000895CF93DF93482F50E0A0" +
								"3A1002E000CA0186569F4FFC0134914A575F4FFA016D" +
								"3A1002F0008491882369F190E0880F991FFC01E859E7" +
								"3A10030000FF4FA591B491FC01EE58FF4FC591D491D8" +
								"3A10031000662351F42FB7F8948C91932F90958923ED" +
								"3A100320008C93888189230BC0623061F42FB7F894D5" +
								"3A100330008C91932F909589238C938881832B88832C" +
								"3A100340002FBF06C09FB7F8948C91832B8C939FBFCF" +
								"3A10035000DF91CF910895482F50E0CA0182559F4FF9" +
								"3A10036000FC012491CA0186569F4FFC0194914A5783" +
								"3A100370005F4FFA013491332309F440C0222351F135" +
								"3A10038000233071F0243028F42130A1F0223011F50F" +
								"3A1003900014C02630B1F02730C1F02430D9F404C0A5" +
								"3A1003A000809180008F7703C0809180008F7D809343" +
								"3A1003B000800010C084B58F7702C084B58F7D84BD66" +
								"3A1003C00009C08091B0008F7703C08091B0008F7D0D" +
								"3A1003D0008093B000E32FF0E0EE0FFF1FEE58FF4FC9" +
								"3A1003E000A591B4912FB7F894662321F48C91909540" +
								"3A1003F000892302C08C91892B8C932FBF0895CF93B2" +
								"3A10040000DF930E9431010E948100C0E0D0E00E9491" +
								"3A1004100080002097E1F30E940000F9CFF894FFCF0D" +
								"3A00000001FF";
						
						//Convert string to byte array
						byte[] binaryFile = new byte[hexData.length() / 2];
						for(int i=0; i < hexData.length(); i+=2)
						{
							binaryFile[i/2] = Integer.decode("0x" + hexData.substring(i, i + 2)).byteValue();
						}
						
						STK500v1 p = new STK500v1(outStream, inputStream, log, binaryFile);
						
						// Upload
						p.programUsingOptiboot(true, 128);
						
						// Terminate readWrapper
						p.terminateWrapper();
						
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
