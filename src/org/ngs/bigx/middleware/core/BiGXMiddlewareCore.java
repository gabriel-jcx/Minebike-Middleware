package org.ngs.bigx.middleware.core;

import java.awt.Event;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.Vector;

import org.ngs.bigx.dictionary.objects.clinical.BiGXPatientInfo;
import org.ngs.bigx.dictionary.objects.clinical.BiGXPatientPrescription;
import org.ngs.bigx.dictionary.objects.game.BiGXGameTag;
import org.ngs.bigx.dictionary.objects.game.BiGXObject;
import org.ngs.bigx.dictionary.objects.game.BiGXSuggestedGameProperties;
import org.ngs.bigx.dictionary.objects.game.GameServerList;
import org.ngs.bigx.dictionary.objects.game.BiGXSuggestedGameProperties.supportedGames;
import org.ngs.bigx.dictionary.objects.game.properties.Stage;
import org.ngs.bigx.dictionary.objects.game.properties.StageSettings;
import org.ngs.bigx.dictionary.protocol.Specification;
import org.ngs.bigx.middleware.core.BiGXConnectionToDevice.connectionTypeEnum;
import org.ngs.bigx.middleware.core.BiGXPin.pinToListenEnum;
import org.ngs.bigx.middleware.etc.BiGXGameServerDownloader;
import org.ngs.bigx.middleware.etc.BiGXMiddlewareLogger;
import org.ngs.bigx.middleware.etc.BiGXSuggestedGamePropertiesTimer;
import org.ngs.bigx.middleware.exceptions.BiGXDeviceIDNotFoundException;
import org.ngs.bigx.middleware.exceptions.BiGXDeviceIDOverlapException;
import org.ngs.bigx.middleware.exceptions.BiGXUnsupportedDeviceTypeException;
import org.ngs.bigx.net.gameplugin.common.BiGXNetPacket;
import org.ngs.bigx.net.gameplugin.exception.BiGXInternalGamePluginExcpetion;
import org.ngs.bigx.net.gameplugin.exception.BiGXNetException;
import org.ngs.bigx.net.gameplugin.exception.BiGXNetServerPortInUseException;
import org.ngs.bigx.net.gameplugin.server.BiGXNetServer;
import org.ngs.bigx.net.gameplugin.server.BiGXNetServerListener;

public class BiGXMiddlewareCore implements BiGXNetServerListener, BiGXConnectionToDeviceEventListener{
	private Hashtable<String, BiGXConnectionToDevice> DeviceConnectionArray;
	private Hashtable<Integer, String> DeviceConnectionArrayIndex;
	protected BiGXNetServer bigxServer;
	private BiGXMiddlewareTranslator bigxDataTranslter;
	public static int motorValue = 0;
	private static String currentUserName;
	
	private Hashtable<Integer, Vector<sensorDataElement>> dataDictionary;
	private Hashtable<Integer, String> fileNameArray;
	public static final int MAXSENSORVALUEQUEUESIZE  = 20;
	
	public static long HRDuration = 0;
	public static long HRGoal = 900000; // in Milliseconds 
	public static long HRGoalRangeMin = 140; //TODO: Need to get value from the doctors...
	
	public static Vector<BiGXGameTag> biGXGameTags = new Vector<BiGXGameTag>();
	
	public static String BigxMiddlewareBaseFolderPath;
	public static String BigxMiddlewareReportFolderPath;
	public static String BigxMiddlewareTempFolderPath;
	public static String BigxMiddlewareFormatedFileFolderPath;
	
	private static boolean[] deviceUniqueIDPool;
	private static final int deviceUniqueIDPoolSize = 256;

	private String iXerciseAccountName = "";
	private String iXercisePassword = "";
	private static BiGXPatientInfo iXercisePatientProfile = new BiGXPatientInfo();
	
	private static BiGXSuggestedGameProperties biGXSuggestedGameProperties = null;
	private static BiGXSuggestedGamePropertiesTimer biGXSuggestedGamePropertiesTimer = null;
	
	public static int iXerciseBackendConnected = 0;
	public static int iXercisePatientInfoRetrieved = 0;

	public final static String dataMangementSystemHost = "128.195.54.50"; 
//	public final static String dataMangementSystemHost = "192.168.0.51"; 
//	public final static String dataMangementSystemHost = "localhost"; 
	
	public BiGXConnectionToBackend biGXConnectionToBackend;
	
	private BiGXGameServerDownloader biGXGameServerDownloader;
	private GameServerList gameServerList;
	
	public BiGXGameServerDownloader getBiGXGameServerDownloader() {
		return biGXGameServerDownloader;
	}

	public GameServerList getGameServerList() {
		return gameServerList;
	}

	public void setGameServerList(GameServerList gameServerList) {
		this.gameServerList = gameServerList;
	}

	private synchronized void initDeviceUIDPool()
	{
		deviceUniqueIDPool = new boolean[deviceUniqueIDPoolSize];
		Arrays.fill(deviceUniqueIDPool, false);
	}
	
	public BiGXSuggestedGamePropertiesTimer getBiGXSuggestedGamePropertiesTimer() {
		return biGXSuggestedGamePropertiesTimer;
	}

	public synchronized int getNextAvailableDeviceUID() throws Exception
	{
		int i = 0;
		int returnValue = -1;
		
		for(i=0; i<deviceUniqueIDPoolSize; i++)
		{
			if(deviceUniqueIDPool[i] == false) {
				returnValue = i;
				deviceUniqueIDPool[i] = true;
				break;
			}
		}
		
		if(i == deviceUniqueIDPoolSize){
			throw new Exception("255 device is connected already!");
		}
		
		return returnValue;
	}
	
	public BiGXSuggestedGameProperties getBiGXSuggestedGameProperties() {
		return biGXSuggestedGameProperties;
	}

	public void setBiGXSuggestedGameProperties(
			BiGXSuggestedGameProperties biGXSuggestedGameProperties) {
		BiGXMiddlewareCore.biGXSuggestedGameProperties = biGXSuggestedGameProperties;
	}

	public void assignTestVariables()
	{
		biGXSuggestedGameProperties = new BiGXSuggestedGameProperties();
		
		biGXSuggestedGameProperties.getCharacterProperties().setLuck(10);
		biGXSuggestedGameProperties.getCharacterProperties().setSkill(20);
		biGXSuggestedGameProperties.getCharacterProperties().setSpeedRate(30);
		biGXSuggestedGameProperties.getCharacterProperties().setStrength(40);
		biGXSuggestedGameProperties.getCharacterProperties().setWeight(50);
		
		biGXSuggestedGameProperties.getPlayerProperties().setGame(supportedGames.MINECRAFT);
		biGXSuggestedGameProperties.getPlayerProperties().setGameplayskill(11);

		biGXSuggestedGameProperties.getQuestProperties().getDifficultyDefinitionTable().exerciseDifficultyTable.add(new BiGXObject());
		biGXSuggestedGameProperties.getQuestProperties().getDifficultyDefinitionTable().exerciseDifficultyTable.add(new BiGXObject());
		biGXSuggestedGameProperties.getQuestProperties().getDifficultyDefinitionTable().gameDifficultyTable.add(new BiGXObject());
		biGXSuggestedGameProperties.getQuestProperties().getDifficultyDefinitionTable().gameDifficultyTable.add(new BiGXObject());
		biGXSuggestedGameProperties.getQuestProperties().getDifficultyDefinitionTable().gameDifficultyTable.add(new BiGXObject());
		biGXSuggestedGameProperties.getQuestProperties().setDuration(12);
		biGXSuggestedGameProperties.getQuestProperties().setGame(supportedGames.MINECRAFT);
		ArrayList<Stage> teststages = new ArrayList<Stage>();
		Stage tStage = new Stage();
		tStage.duration = 1;
		tStage.exerciseSettings = 1;
		tStage.questSettings = 1;
		teststages.add(tStage);
		tStage.duration = 22;
		tStage.exerciseSettings = 22;
		tStage.questSettings = 22;
		teststages.add(tStage);
		ArrayList<StageSettings> teststagesettingsarray = new ArrayList<StageSettings>();
		StageSettings testStagesettings = new StageSettings();
		testStagesettings.stages = teststages;
		testStagesettings.totalduration = 23;
		teststagesettingsarray.add(testStagesettings);
		biGXSuggestedGameProperties.getQuestProperties().setStageSettingsArray(teststagesettingsarray);
		
		System.out.println(biGXSuggestedGameProperties.toJsonString());
	}
	
	public String getiXerciseAccountName() {
		return iXerciseAccountName;
	}

	public void setiXerciseAccountName(String iXerciseAccountName) {
		this.iXerciseAccountName = iXerciseAccountName;
	}

	public String getiXercisePassword() {
		return iXercisePassword;
	}

	public void setiXercisePassword(String iXercisePassword) {
		this.iXercisePassword = iXercisePassword;
	}
	
	public BiGXPatientInfo getiXercisePatientProfile() {
		return iXercisePatientProfile;
	}

	public void setiXercisePatientProfile(BiGXPatientInfo iXercisePatientProfile) {
		BiGXMiddlewareCore.iXercisePatientProfile = iXercisePatientProfile;
	}

	public BiGXMiddlewareCore()
	{
		this.DeviceConnectionArray = new Hashtable<String, BiGXConnectionToDevice>();
		this.DeviceConnectionArrayIndex = new Hashtable<Integer, String>();
		this.bigxServer = new BiGXNetServer();
		initDeviceUIDPool();
		
		try {
			this.bigxServer.init();
		} catch (BiGXNetServerPortInUseException e) {
			e.printStackTrace();
		}
		
		this.biGXGameServerDownloader = new BiGXGameServerDownloader(this);
		
		this.bigxServer.setReceiveListener(this);
		this.bigxDataTranslter = new BiGXMiddlewareTranslator();

		this.dataDictionary = new Hashtable<Integer, Vector<sensorDataElement>>();
		this.fileNameArray = new Hashtable<Integer, String>();
		
		this.fileNameArray.put(org.ngs.bigx.dictionary.protocol.Specification.DataType.HEART, "heart");
		
		// Obtain the bigxfolder path
		BigxMiddlewareBaseFolderPath = System.getProperty("user.dir") + "/out";
		BigxMiddlewareReportFolderPath = BigxMiddlewareBaseFolderPath + "/raw";
		BigxMiddlewareTempFolderPath = BigxMiddlewareBaseFolderPath + "/temp";
		BigxMiddlewareFormatedFileFolderPath = BigxMiddlewareBaseFolderPath + "/formatted";
		
		// Create a folder if not exist
		try
		{
			new File(BigxMiddlewareReportFolderPath).mkdirs();
		}
		catch(Exception ee)
		{
			ee.printStackTrace();
		}

		// Create a temp folder if not exist
		try
		{
			new File(BigxMiddlewareTempFolderPath).mkdirs();
		}
		catch(Exception ee)
		{
			ee.printStackTrace();
		}

		// Create a formatted file folder if not exist
		try
		{
			new File(BigxMiddlewareFormatedFileFolderPath).mkdirs();
		}
		catch(Exception ee)
		{
			ee.printStackTrace();
		}
		
		this.biGXConnectionToBackend = new BiGXConnectionToBackend(this);
		
		// TODO: Need to remove at the production
		this.assignTestVariables();
		
		BiGXMiddlewareCore.biGXSuggestedGamePropertiesTimer = new BiGXSuggestedGamePropertiesTimer(this);
	}
	
	/* Store the current device in the list, and returns  */
	public synchronized BiGXConnectionToDevice addDevice(String id, connectionTypeEnum connectionType)
        throws Exception 
	{
		/* If the name of the connection is the same, then reject it */
		if (this.DeviceConnectionArray.containsKey(id))
			throw new BiGXDeviceIDOverlapException("Device's ID overlaps. Please check.");

		int deviceuidkey = (int) getNextAvailableDeviceUID();
		
		BiGXConnectionToDevice device = new BiGXConnectionToDevice(id, deviceuidkey, connectionType);
		
		// TODO: Need to add logic to handle duplicate uuid
		this.DeviceConnectionArray.put(id, device);
		this.DeviceConnectionArrayIndex.put(deviceuidkey, id);
		
		device.addListener(this);
		
		return device;
	}

	/* Remove the current device in the list */
	public synchronized void removeDevice(String id)
        throws BiGXDeviceIDNotFoundException, IOException, 
        		BiGXUnsupportedDeviceTypeException, BiGXDeviceIDOverlapException
	{
		BiGXConnectionToDevice tempDevice = this.DeviceConnectionArray.remove(id);
		
		if(tempDevice == null)
			throw new BiGXDeviceIDOverlapException("Device Not Found.");

		this.DeviceConnectionArrayIndex.remove(tempDevice.getUUID());
		this.DeviceConnectionArray.remove(tempDevice);
		
		tempDevice.removeListener(this);
		
		tempDevice.stop();
	}
	
	public void sendSuggestedGameDesign(BiGXNetPacket bigxpacket)
	{
		bigxpacket.commandId = Specification.Command.ACK_GAME_DESIGN_HANDSHAKE;
		
		if(biGXSuggestedGameProperties == null)
		{
			bigxpacket.sourceDevice = 0;
			bigxpacket.deviceEvent = 0;
			
			try {
				this.bigxServer.send(bigxpacket);
			} catch (BiGXNetException | BiGXInternalGamePluginExcpetion e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{	
			byte[] bSuggestedGamePropertiesJsonString = null;
			
			try {
				// Serialize the Json string
				if(BiGXMiddlewareCore.biGXSuggestedGameProperties == null)
					bSuggestedGamePropertiesJsonString = new byte[0];
				else
					bSuggestedGamePropertiesJsonString =  BiGXMiddlewareCore.biGXSuggestedGameProperties.toJsonString().getBytes("US-ASCII");
			} catch (UnsupportedEncodingException e1) {
				// Auto-generated catch block
				e1.printStackTrace();
			}
			
			int totalSgpjstrFragmentationNumber = bSuggestedGamePropertiesJsonString.length / (bigxpacket.DATALENGTH-2) / 256;
			int totalSgpjstrChunkNumber = bSuggestedGamePropertiesJsonString.length / (bigxpacket.DATALENGTH-2) % 256;
			
			totalSgpjstrFragmentationNumber++;
			totalSgpjstrChunkNumber++;

			int fragmentationIdx = 0;
			int chunkIdx = 0;
			
			bigxpacket.sourceDevice = totalSgpjstrFragmentationNumber;
			bigxpacket.deviceEvent = totalSgpjstrChunkNumber;
			
			try {
				this.bigxServer.send(bigxpacket);
			} catch (BiGXNetException | BiGXInternalGamePluginExcpetion e) {
				// Auto-generated catch block
				e.printStackTrace();
			}
			
			// Start to send each chunks
			bigxpacket.commandId = Specification.Command.TX_GAME_DESIGN;
			
			// TODO: Need to rethink about the index count for cases where the total index == 0 and total fragmentation index == 0
			for(fragmentationIdx=0; fragmentationIdx<totalSgpjstrFragmentationNumber; fragmentationIdx++)
			{
				int chunknumberToIterate = 256;
				
				if((totalSgpjstrFragmentationNumber-1) == fragmentationIdx)
				{
					chunknumberToIterate = totalSgpjstrChunkNumber;
				}
				for(chunkIdx=0; chunkIdx<chunknumberToIterate; chunkIdx++)
				{
					Arrays.fill(bigxpacket.data, (byte) 0 );
					
					bigxpacket.sourceDevice = fragmentationIdx;
					bigxpacket.deviceEvent = chunkIdx;
					
					bigxpacket.data[0] = (byte) ((chunkIdx & 0xFF00)>>8);
					bigxpacket.data[1] = (byte) (chunkIdx & 0xFF);
					
					int startpoint = fragmentationIdx*(bigxpacket.DATALENGTH-2)*256
							+ chunkIdx*(bigxpacket.DATALENGTH-2);
					int byteleft = ((bSuggestedGamePropertiesJsonString.length - startpoint)>=(bigxpacket.DATALENGTH-2))?(bigxpacket.DATALENGTH-2):(bSuggestedGamePropertiesJsonString.length - startpoint);
					
					System.arraycopy(bSuggestedGamePropertiesJsonString, startpoint, bigxpacket.data, 2, byteleft);
					
					try {
						this.bigxServer.send(bigxpacket);
					} catch (BiGXNetException | BiGXInternalGamePluginExcpetion e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			
			// Need to send the TX DONE packet to the game.
			bigxpacket.commandId = Specification.Command.REQ_GAME_DESIGN_DOWNLOAD_VALIDATE;
			
			try {
				this.bigxServer.send(bigxpacket);
			} catch (BiGXNetException | BiGXInternalGamePluginExcpetion e) {
				// Auto-generated catch block
				e.printStackTrace();
			}
			
			System.out.println("DONE Sending");
		}
		
		return;
	}

	// TODO Need refactoring the current function.
	// Callback function triggered by UDP communication.
	@Override
	public void onMessageReceive(BiGXNetPacket bigxpacket)
	{
		// TODO Need to come up with an idea for data translator
		System.out.println("RECEIVED?");

		if(bigxpacket.commandId == org.ngs.bigx.dictionary.protocol.Specification.Command.REQ_SET_USERNAME)
		{
			currentUserName = new String(bigxpacket.data).substring(0,4);
			currentUserName += new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
			
			BiGXMiddlewareLogger.print(BiGXMiddlewareCore.class.toString(), "Received Name: " + currentUserName);
			
			HRDuration = 0;
			return;
		}
		else if(bigxpacket.commandId == org.ngs.bigx.dictionary.protocol.Specification.Command.TX_GAME_TAG)
		{
			byte timestampbytearray[] = new byte[8];
			
			BiGXGameTag biGXGameTag = new BiGXGameTag();
			biGXGameTag.setTagName(Specification.fromTagNumberToTagString(bigxpacket.deviceEvent));

			System.arraycopy(bigxpacket.data, 0, timestampbytearray, 0, timestampbytearray.length);
			ByteBuffer buffer = ByteBuffer.allocate(Long.BYTES);
	        buffer.put(timestampbytearray);
	        buffer.flip();//need flip 
	        long timestamp = buffer.getLong();
	        biGXGameTag.setTimestamp(Long.toString(timestamp));
			
	        biGXGameTags.add(biGXGameTag);
	        
	        System.out.println("A gameTag" + biGXGameTag.getTagName() + " received ");
			
			return;
		}
		else if(bigxpacket.commandId == Specification.Command.REQ_GAME_TAG_RESET)
		{
			biGXGameTags = new Vector<BiGXGameTag>();
			
			return;
		}
		else if(bigxpacket.commandId == Specification.Command.REQ_GAME_DESIGN_HANDSHAKE)
		{
			this.sendSuggestedGameDesign(bigxpacket);
		}
		
		String strid = this.DeviceConnectionArrayIndex.get(bigxpacket.sourceDevice);
		
		BiGXMiddlewareLogger.print(BiGXMiddlewareCore.class.toString(), "Received: " + new String(bigxpacket.data));
		BiGXMiddlewareLogger.print(BiGXMiddlewareCore.class.toString(), "strId: " + strid);
		BiGXMiddlewareLogger.print(BiGXMiddlewareCore.class.toString(), "Source Device: " + bigxpacket.sourceDevice);
		
		if(strid == null)
		{
			try {
				throw new BiGXDeviceIDOverlapException("Device Not Found from the devie connection index table");
			} catch (BiGXDeviceIDOverlapException e) {
				e.printStackTrace();
				return;
			}
		}
		
		BiGXConnectionToDevice tempDevice = this.DeviceConnectionArray.get(strid);
		
		if(tempDevice == null){
			try {
				throw new BiGXDeviceIDOverlapException("Device Not Found.");
			} catch (BiGXDeviceIDOverlapException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return;
			}
		}		
		
		switch(bigxpacket.deviceEvent)
		{
		    case org.ngs.bigx.dictionary.protocol.Specification.DataType.RESISTANCE:
	    		BiGXMiddlewareCore.motorValue = bigxpacket.data[1] | (bigxpacket.data[2]<<8);
		    	setResistanceValue(motorValue);
				System.out.println("Received Resistance ["+(BiGXMiddlewareCore.motorValue)+"]");
		    	break;
		    default:
		    	new Exception("dataType is not supported." + bigxpacket.deviceEvent).printStackTrace();
		    	break;
		};
	}
	
	public void setResistanceValue(int value)
	{
		BiGXConnectionToDevice tempDevice = this.DeviceConnectionArray.get(this.DeviceConnectionArray.keys().nextElement());
		
		if(tempDevice == null){
			try {
				throw new BiGXDeviceIDOverlapException("Device Not Found.");
			} catch (BiGXDeviceIDOverlapException e) {
				e.printStackTrace();
				return;
			}
		}	
		
		try {
    		BiGXMiddlewareCore.motorValue = value;
    		
    		int difficulty = 0;
    		
    		if(BiGXMiddlewareCore.biGXSuggestedGameProperties != null)
    		{
    			List<BiGXPatientPrescription> prescriptionList = BiGXMiddlewareCore.biGXSuggestedGameProperties.getPlayerProperties().getPatientPrescriptions();
    			
    			if(prescriptionList != null)
    			{
    				for(BiGXPatientPrescription prescription : prescriptionList)
    				{
    					if(prescription.getDataType() == null)
    					{
    						continue;
    					}
    					
    					if(prescription.isActive()
    							&& prescription.getDataType().equals("SSO03"))
    					{
    						difficulty = prescription.getTargetMin();
    						break;
    					}
    				}
    			}
    		}
    		
			tempDevice.setValue(pinToListenEnum.ANALOG_05, BiGXMiddlewareCore.motorValue + difficulty);
			
			System.out.println("Send it to pin A5 val["+(BiGXMiddlewareCore.motorValue+difficulty)+"] difficulty["+difficulty+"]");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// Callback function triggered by devices
	@Override
	public void onMessageReceiveFromDevice(Event event, String message) {
		BiGXConnectionToDevice tempDevice = this.DeviceConnectionArray.get(message.split("\\|")[0] + "|" + message.split("\\|")[1]);
		
		if(tempDevice == null)
		{
			System.out.println("Currently No device is detected.");
			return;
		}

		int value = Integer.parseInt(message.split("\\|")[3]);
		int DataType = Integer.parseInt(message.split("\\|")[4]);
		int valueType = 0; // First byte for every packet's data field		
		
		this.sendAndLogRecievedMessage(tempDevice, value, DataType, valueType);
	}
	
	public void sendAndLogRecievedMessage(BiGXConnectionToDevice biGXConnectionToDevice, int value, int DataType, int valueType)
	{
		try {
			
			double translatedData = bigxDataTranslter.updateRawData(DataType, valueType, value);
			
			if(translatedData != Double.MIN_VALUE)
			{
				int intformattranslateData = (int)translatedData;
				byte datavalue[] = {0, (byte) (intformattranslateData & 0xFF),(byte) ((intformattranslateData & 0xFF00)>>8),0,0,0,0,0,0};
				BiGXNetPacket packet = new BiGXNetPacket(32, (int)biGXConnectionToDevice.getUUID(), DataType, datavalue);
				this.bigxServer.send(packet);
			}
			
			/* Data Backup + HR Calculation */
			Vector<sensorDataElement> sensorValueDictionary = this.dataDictionary.get(DataType);
			
			if(sensorValueDictionary == null)
			{
				sensorValueDictionary = new Vector<sensorDataElement>();
				this.dataDictionary.put(DataType, sensorValueDictionary);
			}

			// Obtain the current value
			int index = sensorValueDictionary.size();
			
			if(index != 0){
				sensorDataElement tempValue = new sensorDataElement();
				tempValue.value = translatedData;
				tempValue.timestamp = System.currentTimeMillis();
				
				sensorValueDictionary.add(tempValue);
				
				this.calcHRDuration(sensorValueDictionary, (int)biGXConnectionToDevice.getUUID(), DataType, tempValue);
			}
			else{
				sensorDataElement tempValue = new sensorDataElement();
				tempValue.value = translatedData;
				tempValue.timestamp = System.currentTimeMillis();
				
				sensorValueDictionary.add(tempValue);
			}
			
			if(sensorValueDictionary.size() >= MAXSENSORVALUEQUEUESIZE)
			{
				this.writeValueToFile(DataType, sensorValueDictionary);
			}
		} catch (BiGXNetException | BiGXInternalGamePluginExcpetion ee) {
			// TODO Auto-generated catch block
			ee.printStackTrace();
		} catch (Exception ee) {
			// TODO Auto-generated catch block
			ee.printStackTrace();
		}
	}
	
	public void sendSimulatorMessage(BiGXNetPacket packet)
	{
		try {
			this.bigxServer.send(packet);
		} catch (BiGXNetException e) {
			e.printStackTrace();
		} catch (BiGXInternalGamePluginExcpetion e) {
			e.printStackTrace();
		}
	}
	
	private void calcHRDuration(Vector<sensorDataElement> sensorValueDictionary, int deviceUUID, int valueType, sensorDataElement currentValue) throws BiGXNetException, BiGXInternalGamePluginExcpetion
	{
		sensorDataElement previousValue = null;
		
		int numOfDataInDictionary = sensorValueDictionary.size();
		
		// Filter out any other cases
		if(valueType != Specification.DataType.HEART)
			return;
		
		if(numOfDataInDictionary > 2)
		{
			// TODO: Send HR
			previousValue = sensorValueDictionary.elementAt(numOfDataInDictionary - 2);
			
			if(previousValue.value >= HRGoalRangeMin)
			{
				HRDuration += (currentValue.timestamp - previousValue.timestamp);
			}
		}
		
		double percentage = (HRDuration / (double)HRGoal) * 100;
		percentage = percentage>=100?100:percentage;
		
		int fixedpointfirsttwonums = (int)((percentage - ((int) (percentage))) * 100);
		byte datavalue[] = {0, (byte) ((int) (percentage) & 0xFF),(byte) ((fixedpointfirsttwonums & 0xFF)>>8),0,0,0,0,0,0};
		BiGXNetPacket packet = new BiGXNetPacket(32, deviceUUID, Specification.DataType.TIMELAPSE_HEARTRATEREQUIREMENT, datavalue);
		this.bigxServer.send(packet);
	}
	
	public synchronized void writeValueToFile(int valueType, Vector<sensorDataElement> sensorValueDictionary)
	{
		sensorDataElement element = null;
		PrintWriter out;
		String fileName = "";
		int samplingRate = 1;  // Write a data per sampling rate
		double sampleAverage = 0;
		long sampleAverageTimeStamp = 0;
		
		// Prevent Writing a file when the game is not connected. Probably we will update this logic later
		if( (iXerciseBackendConnected != 1) || (iXercisePatientInfoRetrieved != 1) )
		{
			// System.out.println("[BiGX] Middleware is not logged into iXercise Backend");
			return;
		}
		
		// TODO: Need to write the pedaling statistics
		switch(valueType)
		{
		case Specification.DataType.HEART:
			fileName = "SSO01";
			samplingRate = 1;
			break;
		case Specification.DataType.ROTATE:
			fileName = "SSO02";
			samplingRate = 1;
			break;
		case Specification.DataType.RESISTANCE:
			fileName = "SSO03";
			samplingRate = 2;
			break;
		case Specification.DataType.TORQE:
			fileName = "SSO04";
			samplingRate = 1;
			break;
		default:
			return;
		}
		
		try{
			fileName = BigxMiddlewareReportFolderPath + "/" + fileName + "_" +
					iXerciseAccountName.replace('@', '_').replace('.', '_') + "_" + new Date().getTime() + ".dat";
			
			System.out.println("fileName: " + fileName);
			
			out = new PrintWriter(new BufferedWriter(new FileWriter(fileName, true)));
			
			for(int i=0; i<MAXSENSORVALUEQUEUESIZE ; i++)
			{
				if(sensorValueDictionary.size() == 0)
					break;
				
				sampleAverage = 0;
				sampleAverageTimeStamp = 0;
				
				for(int j=0; j<samplingRate; j++)
				{
					if(sensorValueDictionary.size() == 0)
						break;
					
					element = sensorValueDictionary.remove(0);
					sampleAverage += (element.value/samplingRate);
					sampleAverageTimeStamp += (element.timestamp/samplingRate);
				}
				
				if(sampleAverageTimeStamp == 0)
					continue;
				
				// WRITE
			    out.println(sampleAverageTimeStamp + "\t" + (Double.toString(sampleAverage)));
			}
			out.close();
		}
		catch (Exception ee)
		{
			ee.printStackTrace();
		}
	}
	
	class sensorDataElement
	{
		public double value;
		public long   timestamp;
	}

	@Override
	public void onClientDisconnected(String clientIPAndPort) {
		BiGXMiddlewareLogger.print(BiGXMiddlewareCore.class.toString(), "TIMEOUT-CLIENT[" + clientIPAndPort + "]");
		System.out.println(BiGXMiddlewareCore.class.toString() + "TIMEOUT-CLIENT[" + clientIPAndPort + "]");
	}
}
