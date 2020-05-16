package org.ngs.bigx.middleware.core;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.Vector;

import org.ngs.bigx.data.BiGXDataEntity;
import org.ngs.bigx.data.BiGXDataFileStream;
import org.ngs.bigx.data.BiGXDataFileStream.sensorDataElement;
import org.ngs.bigx.dictionary.objects.clinical.BiGXPatientPrescription;
import org.ngs.bigx.dictionary.objects.game.BiGXGameTag;
import org.ngs.bigx.middleware.etc.ScpTo;

public class BiGXConnectionToBackend {
	private Timer fileSenderTimer;
	private TimerTask fileSenderTimerTask;
	private TimerTask immadiateFileSenderTimerTask;
	protected BiGXMiddlewareCore biGXMiddlewareCore;
	public static final int ScpServerPort = 9001;
	
	public static final int pingPeriod = 30 * 60 * 1000; // Every Thirty Minutes
	
	public BiGXConnectionToBackend(BiGXMiddlewareCore biGXMiddlewareCore)
	{
		this.biGXMiddlewareCore = biGXMiddlewareCore;
	}
	
	public void startfileSenderTimer()
	{
		this.fileSenderTimerTask = new FileSenderTimerTask(this);
		
		if(fileSenderTimer != null)
		{
			fileSenderTimer.cancel();
		}
		
		fileSenderTimer = new Timer(true);
		fileSenderTimer.schedule(fileSenderTimerTask, 0, pingPeriod);
	}
	
	public void sendFileNowAndRefreshTimer()
	{
		this.immadiateFileSenderTimerTask = new FileSenderTimerTask(this);
		
		this.stopfileSenderTimer();
		this.startfileSenderTimer();
		
		new Timer(true).schedule(immadiateFileSenderTimerTask, 100);
	}
	
	public void stopfileSenderTimer()
	{
		if(this.fileSenderTimer == null)
			return;
		
		this.fileSenderTimer.cancel();
	}
	
	public void sendFileWrapper(Vector<BiGXGameTag> gameTags, String rawFileName)
	{
		DateFormat dateFormat = new SimpleDateFormat("yyyy.MM.dd HH:mm:ss");
		Date date = new Date();
		
		BiGXDataFileStream testStream = new BiGXDataFileStream();
		BiGXDataEntity testDataEntity = new BiGXDataEntity();
		testDataEntity.setDatasize(4);
		testDataEntity.setDate(dateFormat.format(date).split(" ")[0]);
		testDataEntity.setEncoding("UTF-8");
		testDataEntity.setEquipmentID("EQ01");
		testDataEntity.setGameID("GM01");
		testDataEntity.setLocation("128.0.0.1");
		testDataEntity.setPatientid(this.biGXMiddlewareCore.getiXercisePatientProfile().getCaseid());
		testDataEntity.setSensorID("SSO01");
		testDataEntity.setTime(dateFormat.format(date).split(" ")[1]);
		testDataEntity.setVersion("00.01.001");
		testDataEntity.makeSessionID();
		testDataEntity.setVersion("00.01.001");
		
		String outputFileName = testDataEntity.getSessionID() + testDataEntity.getEquipmentID() + testDataEntity.getSensorID() + "_" + System.currentTimeMillis() + ".txt";
		outputFileName = outputFileName.replaceAll(":", "_");
		
		BiGXPatientPrescription biGXPatientPrescription = new BiGXPatientPrescription(); 
		boolean prescriptionFound = false;
		
		if(BiGXMiddlewareCore.iXercisePatientInfoRetrieved == 1)
		{
			List<BiGXPatientPrescription> prescriptionList = biGXMiddlewareCore.getiXercisePatientProfile().getPrescriptions();
			
			for(BiGXPatientPrescription prescription : prescriptionList)
			{
				if(prescription == null)
					continue;
				
				if(prescription.getDataType() == null)
					continue;
				
				if( (prescription.isActive()) &&
					(prescription.getDataType().equals("SSO01"))
					)
				{
					prescriptionFound = true;
					biGXPatientPrescription.setTargetMax(prescription.getTargetMax());
					biGXPatientPrescription.setTargetMin(prescription.getTargetMin());
					biGXPatientPrescription.setDataType("SSO01");
					break;
				}
			}
		}
		
		if(prescriptionFound == false)
		{
			biGXPatientPrescription.setTargetMin(100);
			biGXPatientPrescription.setTargetMax(200);
			biGXPatientPrescription.setDataType("SSO01");
			
			System.out.println("Prescription Not Retrieved or Not Generated");
		}
			
		testDataEntity.setBiGXPatientPrescription(biGXPatientPrescription);
		
		testStream.encodeToBiGXDataFile(testDataEntity, gameTags, rawFileName, 
				BiGXMiddlewareCore.BigxMiddlewareFormatedFileFolderPath + "/" + outputFileName);
		
		System.out.println("[Tags] sz[" + gameTags.size() + "]");
		
		// TODO: Remove the temp file
		File tempFile = new File(rawFileName);
		tempFile.delete();
		
		// Upload the formatted file
		ScpTo scpTo = new ScpTo(biGXMiddlewareCore);
		scpTo.sendFile(new String[]{BiGXMiddlewareCore.BigxMiddlewareFormatedFileFolderPath + "/" + outputFileName, 
				"random@" + BiGXMiddlewareCore.dataMangementSystemHost + ":/" + outputFileName});

		File formattedFile = new File(BiGXMiddlewareCore.BigxMiddlewareFormatedFileFolderPath + "/" + outputFileName);
		formattedFile.delete();
	}
	
	class FileSenderTimerTask extends TimerTask
	{
		private BiGXConnectionToBackend biGXConnectionToBackend;
		
		public FileSenderTimerTask(BiGXConnectionToBackend biGXConnectionToBackend)
		{
			this.biGXConnectionToBackend = biGXConnectionToBackend;
		}

		@Override
		public synchronized void run() {
			// INITIATE THE PROCESS ONLY WHEN THE MIDDLEWARE IS LOGGED INTO THE SERVER
			if(BiGXMiddlewareCore.iXercisePatientInfoRetrieved != 1)
			{
				return;
			}
			
			// CHECK RAW FILES IN THE FOLDER (WHILE LOOP)
			File folder = new File(BiGXMiddlewareCore.BigxMiddlewareReportFolderPath);
			
			if(folder.isDirectory()){
				int i=0;
				List<String> fileList = Arrays.asList(folder.list());
				int filecountInTempFolder = fileList.size();
				String outputFileName = "tempSensorDataFile_" + new Date().getTime();
				String currentDataType = "";
				PrintWriter writer;
				long timstampbegin = Long.MAX_VALUE;
				long timstampend = Long.MIN_VALUE;
				boolean isEmptyFile = true;
				
				Collections.sort(fileList);
				
				if(filecountInTempFolder > 0)
				{
					try{
						// Check if the file is a Zero Data File
						// Delete File
						for(i=0; i<filecountInTempFolder; i++) {
							String currentFileName = fileList.get(i);
							// READ FILE CONTENTS
							Vector<sensorDataElement> dataList = new BiGXDataFileStream().readValueFromRawFile(
									BiGXMiddlewareCore.BigxMiddlewareReportFolderPath + "/" + currentFileName);
							for(int j=0; j<dataList.size(); j++)
							{
								if(Double.parseDouble(dataList.get(j).value) != 0.0)
								{
									isEmptyFile = false;
									break;
								}
							}
							
							if(!isEmptyFile)
								break;
						}
						
						if(isEmptyFile)
						{
							if(!BiGXMiddlewareCore.biGXGameTags.isEmpty())
							{
								isEmptyFile = !isEmptyFile;
							}
						}

						// Remove all zero data files
						if(isEmptyFile)
						{
							for(i=0; i<filecountInTempFolder; i++) {
								String currentFileName = fileList.get(i);
								File tempFile = new File(BiGXMiddlewareCore.BigxMiddlewareReportFolderPath + "/" + currentFileName);
								tempFile.delete();
							}
							System.out.println("[BiGX] Zero Data File Removal Logic Triggered.");
							return;
						}
						
					    writer = new PrintWriter(BiGXMiddlewareCore.BigxMiddlewareTempFolderPath + "/" + outputFileName, "UTF-8");
					    writer.print("{");
					    writer.print("\"databysensors\":[");
					
						for(i=0; i<filecountInTempFolder; i++) {
							String currentFileName = fileList.get(i);
							
							if(!currentFileName.substring(0, 5).equals(currentDataType))
							{
								if(!currentDataType.equals("")) {
									writer.print("]},");
								}
								
								currentDataType = currentFileName.substring(0, 5);
								
								writer.print("{");
								writer.print("\"sensorID\":");
								writer.print("\"" + currentDataType + "\",");
								writer.print("\"data\":[");
							}
							else{
								writer.print(",");
							}
							
							// READ FILE CONTENTS
							Vector<sensorDataElement> dataList = new BiGXDataFileStream().readValueFromRawFile(
									BiGXMiddlewareCore.BigxMiddlewareReportFolderPath + "/" + currentFileName);
							for(int j=0; j<dataList.size(); j++)
							{
								writer.print("{");
								writer.print("\"t\":\"" + dataList.get(j).timestamp + "\",");
								writer.print("\"v\":\"" + dataList.get(j).value + "\"");
								writer.print("}");
								
								if(Long.parseLong(dataList.get(j).timestamp) == 0);
								else if(timstampbegin > Long.parseLong(dataList.get(j).timestamp))
								{
									timstampbegin = Long.parseLong(dataList.get(j).timestamp);
								}
								
								if(timstampend < Long.parseLong(dataList.get(j).timestamp))
								{
									timstampend = Long.parseLong(dataList.get(j).timestamp);
								}
								
								if( (dataList.size() - 1) != j )
								{
									writer.print(",");
								}
							}
							
							if(i == (filecountInTempFolder-1))
							{
								writer.print("]}");
							}
							
							// Delete File
							File tempFile = new File(BiGXMiddlewareCore.BigxMiddlewareReportFolderPath + "/" + currentFileName);
							tempFile.delete();
						}

						// ADD "]"
					    writer.print("],");
						writer.print("\"timebegin\":");
						writer.print("\"" + timstampbegin + "\",");
						writer.print("\"timeend\":");
						writer.print("\"" + timstampend + "\"");
						writer.print("}");
					    writer.close();
					    

						
//						file = new File(BiGXMiddlewareCore.BigxMiddlewareReportFolderPath + "\\" + currentFileName);
//						
//						// MOVE THE RAW FILE TO THE TEMP FOLDER
//						if(file.renameTo(new File(BiGXMiddlewareCore.BigxMiddlewareTempFolderPath + "\\" + outputFileName))){
//							System.out.println("File is moved successful!");
//						}else{
//							System.out.println("File is failed to move!");
//						}
					} catch (IOException e) {
					   e.printStackTrace();
					}
					
					
					
					// CALL SEND FILE WRAPPER FUNCTION TO SEND THE FILE TO THE SERVER FROM TEMP FOLDER
					biGXConnectionToBackend.sendFileWrapper(BiGXMiddlewareCore.biGXGameTags, BiGXMiddlewareCore.BigxMiddlewareTempFolderPath + "/" + outputFileName);
					
					BiGXMiddlewareCore.biGXGameTags.clear();
				}
			} else{
				System.out.println("This is not a directory");
			}
		}
	}
}
