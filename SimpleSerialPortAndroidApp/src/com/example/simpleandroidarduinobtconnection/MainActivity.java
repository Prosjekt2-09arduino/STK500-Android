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
//						byte[] binaryFile;
						
						String hexData = 
								"3A100000000C9462000C948A000C948A000C948A0070" +
										"3A100010000C948A000C948A000C948A000C948A0038" +
										"3A100020000C948A000C948A000C948A000C948A0028" +
										"3A100030000C948A000C948A000C948A000C948A0018" +
										"3A100040000C94AC020C948A000C948A000C948A00E4" +
										"3A100050000C948A000C948A000C948A000C948A00F8" +
										"3A100060000C948A000C948A000000000024002700F1" +
										"3A100070002A0000000000250028002B0000000000DE" +
										"3A1000800023002600290004040404040404040202DA" +
										"3A100090000202020203030303030301020408102007" +
										"3A1000A0004080010204081020010204081020000012" +
										"3A1000B0000007000201000003040600000000000029" +
										"3A1000C00000008C0011241FBECFEFD8E0DEBFCDBFF3" +
										"3A1000D00011E0A0E0B1E0ECECF8E002C005900D9278" +
										"3A1000E000A031B107D9F711E0A0E1B1E001C01D9244" +
										"3A1000F000AE32B107E1F710E0C4ECD0E004C02297C3" +
										"3A10010000FE010E946004C23CD107C9F70E94250489" +
										"3A100110000C9464040C940000CF92EF920F9380E152" +
										"3A1001200091E067E048E029E00AE05BE0E52E3CE092" +
										"3A10013000C32E0E9473020F91EF90CF90089564EF49" +
										"3A1001400071E080E090E00E94F40280E191E00E9482" +
										"3A10015000B50108950F931F9300E111E0C80160E11C" +
										"3A1001600042E020E00E94BD01C80160E040E00E9442" +
										"3A1001700086011F910F9108950F931F938C01FC012D" +
										"3A10018000868160E00E94D10381E090E00E944D03EF" +
										"3A10019000F801868161E00E94D10381E090E00E9435" +
										"3A1001A0004D03F801868160E00E94D10384E690E06F" +
										"3A1001B0000E944D031F910F910895CF92DF92EF920D" +
										"3A1001C000FF920F931F93CF93DF93D82EC92E282F22" +
										"3A1001D000392FC9018C01C0E0D0E0E62EFF24F801E0" +
										"3A1001E000878161E00E949203B7010C2E02C07595D1" +
										"3A1001F00067950A94E2F76170F80187810E94D10344" +
										"3A1002000021960F5F1F4FC830D10549F78D2D9C2DCA" +
										"3A100210000E94BC00DF91CF911F910F91FF90EF9052" +
										"3A10022000DF90CF900895CF92DF92EF92FF920F93DD" +
										"3A100230001F93CF93DF93D82EC92E282F392FC901B2" +
										"3A100240008C01C0E0D0E0E62EFF24F801878161E058" +
										"3A100250000E949203B7010C2E02C0759567950A940F" +
										"3A10026000E2F76170F80187810E94D10321960F5F48" +
										"3A100270001F4FC430D10549F78D2D9C2D0E94BC0025" +
										"3A10028000DF91CF911F910F91FF90EF90DF90CF9072" +
										"3A1002900008951F93CF93DF93EC01162F8C81642F69" +
										"3A1002A0000E94D1038D818F3F19F060E00E94D1033D" +
										"3A1002B0008F8584FF05C0CE01612F0E94DD000EC036" +
										"3A1002C000612F70E084E0759567958A95E1F7CE011E" +
										"3A1002D0000E941301CE01612F0E941301DF91CF9183" +
										"3A1002E0001F91089541E00E94490181E090E0089546" +
										"3A1002F00040E00E9449010895FC016089862F8460D6" +
										"3A10030000808B6C60CF010E9478010895DF93CF93BA" +
										"3A10031000CDB7DEB728970FB6F894DEBF0FBECDBFBE" +
										"3A100320009C01FE013196A8E0B1E088E00D900192B9" +
										"3A100330008150E1F7F9018389481710F0482F4150A7" +
										"3A10034000E42FF0E0EE0FFF1FEC0FFD1F8181680F1F" +
										"3A100350006068C9010E94780128960FB6F894DEBF44" +
										"3A100360000FBECDBFCF91DF91089561E00E9478016B" +
										"3A1003700080ED97E00E944D030895CF93DF93EC0149" +
										"3A10038000423018F08F8588608F874B8B1C8A222320" +
										"3A1003900029F0413019F48F8584608F8780E593ECD4" +
										"3A1003A0000E944D038C8160E00E94D1038E8160E049" +
										"3A1003B0000E94D1038D818F3F19F060E00E94D1032C" +
										"3A1003C0006F8564FD1DC0CE0163E00E94130184E9C6" +
										"3A1003D00091E10E944D03CE0163E00E94130184E984" +
										"3A1003E00091E10E944D03CE0163E00E94130186E972" +
										"3A1003F00090E00E944D03CE0162E00E94130116C0FE" +
										"3A100400006062CE010E94780184E991E10E944D036F" +
										"3A100410006F856062CE010E94780186E990E00E94BB" +
										"3A100420004D036F856062CE010E9478016F85606226" +
										"3A10043000CE010E94780184E0888BCE010E947C016D" +
										"3A10044000CE010E94B50182E0898BCE0166E00E9458" +
										"3A100450007801DF91CF9108956F927F928F92AF9242" +
										"3A10046000CF92EF920F931F93DF93CF93CDB7DEB769" +
										"3A100470003C01162F842FF301448325830683E782F2" +
										"3A10048000C086A18682869D8593879E8594879F8559" +
										"3A1004900095879889968761E00E949203F301858190" +
										"3A1004A0008F3F19F061E00E949203F301868161E0C1" +
										"3A1004B0000E949203112319F0F301178603C080E113" +
										"3A1004C000F3018787C30160E141E020E00E94BD01A4" +
										"3A1004D000CF91DF911F910F91EF90CF90AF908F90C0" +
										"3A1004E0007F906F9008958F92AF92CF92EF920F937B" +
										"3A1004F0001F93CF93DF93DC01362F542F722F102FD1" +
										"3A10050000AE2C8C2C13961C921E92129784E091E0D4" +
										"3A1005100011969C938E9300D000D0EDB7FEB7319624" +
										"3A10052000CDB7DEB71982118212821382CD0161E04C" +
										"3A10053000432F2FEF052FE72EC12E0E942C020F9084" +
										"3A100540000F900F900F90DF91CF911F910F91EF902F" +
										"3A10055000CF90AF908F9008951F920F920FB60F9289" +
										"3A1005600011242F933F938F939F93AF93BF938091C9" +
										"3A10057000290190912A01A0912B01B0912C01309179" +
										"3A100580002D010196A11DB11D232F2D5F2D3720F0C8" +
										"3A100590002D570196A11DB11D20932D018093290196" +
										"3A1005A00090932A01A0932B01B0932C0180912501F7" +
										"3A1005B00090912601A0912701B09128010196A11DDB" +
										"3A1005C000B11D8093250190932601A0932701B0933C" +
										"3A1005D0002801BF91AF919F918F913F912F910F90E3" +
										"3A1005E0000FBE0F901F9018959B01AC017FB7F89438" +
										"3A1005F0008091250190912601A0912701B0912801B9" +
										"3A1006000066B5A89B05C06F3F19F00196A11DB11DED" +
										"3A100610007FBFBA2FA92F982F8827860F911DA11D64" +
										"3A10062000B11D62E0880F991FAA1FBB1F6A95D1F701" +
										"3A10063000BC012DC0FFB7F89480912501909126014F" +
										"3A10064000A0912701B0912801E6B5A89B05C0EF3F16" +
										"3A1006500019F00196A11DB11DFFBFBA2FA92F982F28" +
										"3A1006600088278E0F911DA11DB11DE2E0880F991FF3" +
										"3A10067000AA1FBB1FEA95D1F7861B970B885E934094" +
										"3A10068000C8F2215030404040504068517C4F211505" +
										"3A1006900031054105510571F60895019739F0880F2C" +
										"3A1006A000991F880F991F02970197F1F70895789481" +
										"3A1006B00084B5826084BD84B5816084BD85B5826067" +
										"3A1006C00085BD85B5816085BDEEE6F0E08081816005" +
										"3A1006D0008083E1E8F0E01082808182608083808105" +
										"3A1006E00081608083E0E8F0E0808181608083E1EBDD" +
										"3A1006F000F0E0808184608083E0EBF0E080818160C5" +
										"3A100700008083EAE7F0E0808184608083808182607A" +
										"3A100710008083808181608083808180688083109263" +
										"3A10072000C1000895CF93DF93482F50E0CA01865649" +
										"3A100730009F4FFC0134914A575F4FFA0184918823FF" +
										"3A1007400069F190E0880F991FFC01E859FF4FA591CE" +
										"3A10075000B491FC01EE58FF4FC591D491662351F43A" +
										"3A100760002FB7F8948C91932F909589238C9388813F" +
										"3A1007700089230BC0623061F42FB7F8948C91932FCA" +
										"3A10078000909589238C938881832B88832FBF06C003" +
										"3A100790009FB7F8948C91832B8C939FBFDF91CF915F" +
										"3A1007A0000895482F50E0CA0182559F4FFC012491C3" +
										"3A1007B000CA0186569F4FFC0194914A575F4FFA0138" +
										"3A1007C0003491332309F440C0222351F1233071F0D6" +
										"3A1007D000243028F42130A1F0223011F514C0263045" +
										"3A1007E000B1F02730C1F02430D9F404C080918000EA" +
										"3A1007F0008F7703C0809180008F7D8093800010C030" +
										"3A1008000084B58F7702C084B58F7D84BD09C0809187" +
										"3A10081000B0008F7703C08091B0008F7D8093B000CF" +
										"3A10082000E32FF0E0EE0FFF1FEE58FF4FA591B491BC" +
										"3A100830002FB7F894662321F48C919095892302C0F8" +
										"3A100840008C91892B8C932FBF0895CF93DF930E94B7" +
										"3A1008500057030E94AA00C0E0D0E00E949F002097AA" +
										"3A10086000E1F30E940000F9CFCF92DF92EF92FF9266" +
										"3A100870000F931F93CF93DF937C016B018A01C0E03C" +
										"3A10088000D0E00FC0D6016D916D01D701ED91FC91C3" +
										"3A100890000190F081E02DC7010995C80FD91F0150C3" +
										"3A1008A00010400115110571F7CE01DF91CF911F9115" +
										"3A1008B0000F91FF90EF90DF90CF900895EE0FFF1F04" +
										"3A0C08C0000590F491E02D0994F894FFCF0E" +
										"3A1008CC0000000000720134040000400014005400C9" +
										"3A00000001FF";
						
						//Convert string to byte array
						byte[] binaryFile = new byte[hexData.length() / 2];
						for(int i=0; i < hexData.length(); i+=2)
						{
							binaryFile[i/2] = Integer.decode("0x" + hexData.substring(i, i + 2)).byteValue();
						}
						
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
				log.debugTag("Connecting...");

				try {
					btSocket.connect();
					log.debugTag("Connection OK...");
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
				log.debugTag("Sockets created");
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
