package org.ngs.bigx.middleware.etc;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;

import org.ngs.bigx.dictionary.objects.game.GameServerStatus;
import org.ngs.bigx.dictionary.protocol.Specification;
import org.ngs.bigx.middleware.core.BiGXConnectionToDevice;
import org.ngs.bigx.middleware.core.BiGXMiddlewareCore;
import org.ngs.bigx.middleware.core.BiGXConnectionToDevice.connectionTypeEnum;
import org.ngs.bigx.middleware.etc.ScpTo.CUSTOMCOMMAND;
import org.ngs.bigx.middleware.exceptions.BiGXDeviceIDOverlapException;
import org.ngs.bigx.middleware.exceptions.BiGXUnsupportedDeviceTypeException;
import org.ngs.bigx.middleware.ui.DeviceConfigJFrame;
import org.ngs.bigx.middleware.ui.MainApplicationFrame;

public class LinuxInterfaceServer extends Thread {
	private DatagramSocket socket;
	public static final int serverportnumber = 2331;
	private boolean isReady = true;
	private BiGXMiddlewareCore biGXMiddlewareCore;
	private BiGXConnectionToDevice BiGXDevice = null;
	private MainApplicationFrame mainApplicationFrame;
	
	public static final int buffersize = 1024;
	
	public LinuxInterfaceServer(int port, BiGXMiddlewareCore biGXMiddlewareCore, MainApplicationFrame mainApplicationFrame) {
		this.biGXMiddlewareCore = biGXMiddlewareCore;
		this.mainApplicationFrame = mainApplicationFrame;
		
		try {
			// /if you want the port to listening, you need to enter the port
			// number
			this.socket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
			isReady = false;
		}
	}

	public boolean getIsReady() {
		return this.isReady;
	}

	public void setReady(boolean b) {
		this.isReady = b;
	}
	
	public BiGXConnectionToDevice getBiGXDevice() {
		return BiGXDevice;
	}

	public void setBiGXDevice(BiGXConnectionToDevice biGXDevice) {
		BiGXDevice = biGXDevice;
	}

	public void run() 
	{
		while (true) 
		{
			byte[] data = new byte[buffersize];
			DatagramPacket packet = new DatagramPacket(data, data.length);
			System.out.println("Linux Interface Server Running!");
			try {
				socket.receive(packet);
				System.out.println("Packet Received!!?");
				this.onMessageReceive(packet.getData());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

	}

	@SuppressWarnings("unused")
	public void onMessageReceive(byte[] data) throws FileNotFoundException, UnsupportedEncodingException
	{
		int i=0;
		String command = (new String(data)).trim();
		String[] commandarray = command.split(" ");
		
		System.out.println("[Command Interfacer] CMD[" + command + "]");
		
		if(     (commandarray[0].equals("REMOTE")) &&
        		(commandarray[1].equals("LIST")) &&
        		(commandarray[2].equals("USBSERIAL")))
        {
			String[] usbports = MainApplicationFrame.requestPort();
			
			PrintWriter writer = new PrintWriter("listOfDeviceOnUSBSerial.list", "UTF-8");
			for(i=0; i<usbports.length; i++) {
				writer.println(usbports[i]);
			}
			writer.close();
        }
        else if((commandarray[0].equals("REMOTE")) &&
        		(commandarray[1].equals("CONNECT")) &&
        		(commandarray[2].equals("USBSERIAL")))
        {	
            try {
            	String DeviceID = "USBSERIAL|" + commandarray[3].trim();
            	this.BiGXDevice = biGXMiddlewareCore.addDevice(DeviceID, connectionTypeEnum.USB_SERIAL);//, new FirmataDevice((String) list.getSelectedValue()));
	            
            	DeviceConfigJFrame newFrame = new DeviceConfigJFrame(BiGXDevice, DeviceID, mainApplicationFrame);
	            newFrame.setTitle(DeviceID.replace("|", " : "));
	            newFrame.setBounds(120, 120, 549, 420);
	            newFrame.setVisible(true);			            
			} catch (BiGXDeviceIDOverlapException e1) {
				e1.printStackTrace();
				System.out.println("You have already the device connected and keep that in the list.");
			} catch (BiGXUnsupportedDeviceTypeException e1) {
				e1.printStackTrace();
				System.out.println("Current device type is not supported.");
			} catch (IOException e1) {
				e1.printStackTrace();
			} catch (InterruptedException e1) {
				e1.printStackTrace();
			} catch (Exception ee) {
				ee.printStackTrace();
			}
        }
        else if((commandarray[0].equals("REMOTE")) &&
        		(commandarray[1].equals("CONNECT")) &&
        		(commandarray[2].equals("BTAUDIO")))
        {
			// INITIATE BT AUDIO CONNECTION
			try {
				new ProcessBuilder("/home/pi/Middleware/CheckBT").start();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        }
        else if((commandarray[0].equals("REMOTE")) &&
        		(commandarray[1].equals("STATUS")) &&
        		(commandarray[2].equals("USBSERIAL")))
        {
        	System.out.println("[BiGX] Command[" + command + "] Not available yet.");
        }
        else if((commandarray[0].equals("LOCAL")) &&
        		(commandarray[1].equals("DATA")) &&
        		(commandarray[2].equals("BLEHR")))
        {
			System.out.println("[BiGX] Received a command via LOCAL DATA BLRHR Interface");
			System.out.println("BiGXDevice is: null?"+ (this.BiGXDevice == null));
        	if(this.BiGXDevice != null)
        	{
        		int value = Integer.parseInt(commandarray[3].trim());
				System.out.println("The application received a heartRate");
        		int DataType = Specification.DataType.HEART;
        		int valueType = 0; // First byte for every packet's data field		
        		
        		this.biGXMiddlewareCore.sendAndLogRecievedMessage(this.BiGXDevice, value, DataType, valueType);
        		
        		mainApplicationFrame.updateHeartRate(value);
        	}
        	else
        	{
        		System.out.println("[BiGX] Exercise device is not connected yet");
        	}
        }
        else if((commandarray[0].equals("REMOTE")) &&
        		(commandarray[1].equals("ACCOUNT")) &&
        		(commandarray[2].equals("LOGIN")))
        {	
        	this.biGXMiddlewareCore.setiXerciseAccountName(commandarray[3].trim());
        	this.biGXMiddlewareCore.setiXercisePassword(commandarray[4].trim());
        	
        	ScpTo loginTest = new ScpTo(this.biGXMiddlewareCore);
        	loginTest.logincheck(this.biGXMiddlewareCore.getiXerciseAccountName(), BiGXMiddlewareCore.dataMangementSystemHost);
        	loginTest.sendCustomCommand(new String[]{"src/resources/test/" + "asdf", 
					"testLogin@" + BiGXMiddlewareCore.dataMangementSystemHost + ":/" + "asdf"}, CUSTOMCOMMAND.GETPATIENTINFO);
        	loginTest.sendCustomCommand(new String[]{"src/resources/test/" + "asdf", 
					"testLogin@" + BiGXMiddlewareCore.dataMangementSystemHost + ":/" + "asdf"}, CUSTOMCOMMAND.GETGAMEDESIGN);
        	
        	if(BiGXMiddlewareCore.iXercisePatientInfoRetrieved == 1)
        	{
        		// START PERIODIC TIMER TO SEND PATIENT DATA EVERY
        		this.biGXMiddlewareCore.biGXConnectionToBackend.startfileSenderTimer();
        		this.biGXMiddlewareCore.getBiGXSuggestedGamePropertiesTimer().startQuestDesignSenderTimer();
        		
        		// START PERIODIC TIMER TO GET GAME SERVER LIST
        		this.biGXMiddlewareCore.getBiGXGameServerDownloader().startCheckingGameServerList();
        	}
        }
        else if((commandarray[0].equals("REMOTE")) &&
        		(commandarray[1].equals("ACCOUNT")) &&
        		(commandarray[2].equals("RELOGIN")))
        {	
        	this.biGXMiddlewareCore.setiXerciseAccountName(commandarray[3].trim());
        	this.biGXMiddlewareCore.setiXercisePassword(commandarray[4].trim());
        	
        	ScpTo loginTest = new ScpTo(this.biGXMiddlewareCore);
        	loginTest.logincheck(this.biGXMiddlewareCore.getiXerciseAccountName(), BiGXMiddlewareCore.dataMangementSystemHost);
        	loginTest.sendCustomCommand(new String[]{"src/resources/test/" + "asdf", 
					"testLogin@" + BiGXMiddlewareCore.dataMangementSystemHost + ":/" + "asdf"}, CUSTOMCOMMAND.GETPATIENTINFO);
        	loginTest.sendCustomCommand(new String[]{"src/resources/test/" + "asdf", 
					"testLogin@" + BiGXMiddlewareCore.dataMangementSystemHost + ":/" + "asdf"}, CUSTOMCOMMAND.GETGAMEDESIGN);
        	
        	if(BiGXMiddlewareCore.iXercisePatientInfoRetrieved == 1)
        	{
//        		asdf
        		// START PERIODIC TIMER TO SEND PATIENT DATA EVERY
        		this.biGXMiddlewareCore.biGXConnectionToBackend.stopfileSenderTimer();
        		this.biGXMiddlewareCore.biGXConnectionToBackend.startfileSenderTimer();
        		
        		this.biGXMiddlewareCore.getBiGXSuggestedGamePropertiesTimer().startQuestDesignSenderTimer();
        	}
        }
        else if((commandarray[0].equals("REMOTE")) &&
        		(commandarray[1].equals("ACCOUNT")) &&
        		(commandarray[2].equals("STATUS")))
        {
        	PrintWriter writer = new PrintWriter("loginattemptresult.dat", "UTF-8");
			writer.println("STATUS:" + BiGXMiddlewareCore.iXerciseBackendConnected);
			writer.close();
        }
        else if((commandarray[0].equals("REMOTE")) &&
        		(commandarray[1].equals("TRIGGER")) &&
        		(commandarray[2].equals("SENDDATATODMS")))
        {
        	this.biGXMiddlewareCore.biGXConnectionToBackend.sendFileNowAndRefreshTimer();
        }
        else if((commandarray[0].equals("REMOTE")) &&
        		(commandarray[1].equals("TOGGLE")) &&
        		(commandarray[2].equals("EXERCISEPROGRAM")))
        {
        	boolean toggleValue = Boolean.parseBoolean(commandarray[3].trim());
        	
//        	fff
        }
        else if((commandarray[0].equals("REMOTE")) &&
        		(commandarray[1].equals("SELECT")) &&
        		(commandarray[2].equals("HRMONITOR")))
        {
        	if(commandarray.length <=3)
        	{
        		return;
        	}
        	else{
        		PrintWriter monitor = new PrintWriter("BT.dat", "UTF-8");
        		monitor.println(commandarray[3]);//send the monitor number to BT.dat
        		monitor.close();
        		try{
            		//Runtime.getRuntime().exec("cmd.exe /c cd /home/pi/Middleware & nohup cmd /k\"sudo ./ReadHeartrate\"");
        			new ProcessBuilder("lxterminal", "-e", "/home/pi/Middleware/ReadHeartrate").start();
        		}
        		catch(IOException e)
        		{
        			e.printStackTrace();
        		}
        	}
        }
		
        else if((commandarray[0].equals("REMOTE")) &&
        		(commandarray[1].equals("START")) &&
        		(commandarray[2].equals("GAME")))
        {
        	// Start Game
        	if(commandarray.length <= 3)
        	{
        		return;
        	}
        	else
        	{
        		if(commandarray[3].equals("mc-windows"))
        		{
        			ExecCommand commandRunnerMinecraft = new ExecCommand("\"C:\\Program Files (x86)\\Minecraft\\MinecraftLauncher.exe\"");
        			ExecCommand commandRunnerLogitechController = new ExecCommand("\"C:\\Program Files\\Logitech\\Gaming Software\\LWEMon.exe\"");
        		}
        		
        		if(commandarray[3].equals("minecraft"))
        		{
        			// Check game server
        			GameServerStatus gameServerStatus = null;
        			for(GameServerStatus gsstatus : this.biGXMiddlewareCore.getGameServerList().getGameserverlist())
        			{
        				for(String availablegame:gsstatus.getGamelist())
        				{
        					if(availablegame.equals("minecraft"))
        					{
        						gameServerStatus = gsstatus;
        						break;
        					}
        				}
        				
        				if(gameServerStatus != null)
        				{
        					break;
        				}
        			}
        			
        			if(gameServerStatus != null)
    				{
        				String[] args = new String[1];
        				args[0] = gameServerStatus.getIpserver();
            			this.biGXMiddlewareCore.getBiGXGameServerDownloader().sendCustomCommand(BiGXGameServerDownloader.CUSTOMCOMMAND.LEASEGAMESERVERREQUEST, args);
    				}
        			
        			// INITIATE START GAME SCRIPT
        			// /home/pi/Middleware/GameStream
        			try {
						new ProcessBuilder("lxterminal", "-e", "/home/pi/Middleware/GameStream").start();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
        		}
        	}
        }
	}
}
