package org.ngs.bigx.middleware.core;

import java.io.IOException;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedHashSet;
import java.util.Set;

import org.firmata4j.IOEvent;
import org.firmata4j.Pin;
import org.firmata4j.PinEventListener;
import org.firmata4j.firmata.FirmataDevice;
import org.ngs.bigx.middleware.core.BiGXPin.pinToListenEnum;
import org.ngs.bigx.middleware.exceptions.BiGXUnsupportedDeviceTypeException;

public class BiGXConnectionToDevice {
	/* ID should be unique. Naming convention DEVICETYPE|DEVICENAME|DEVICEPROPERTIES... 
	 * e.g) USBSERIAL|COM1 */
	private String strID;
	private int longID;
	public connectionTypeEnum connectionType;
	public Object deviceHandler;
	private final Set<BiGXConnectionToDeviceEventListener> listeners = Collections.synchronizedSet(new LinkedHashSet<BiGXConnectionToDeviceEventListener>());
	@SuppressWarnings("unused") // TODO: Need to work on this.
	private connectionStateEnum connectionState;
	
	/* Listener hash table key is enum pinToListenEnum */
	private Hashtable<Integer, Object> devicePinList;
	
	public enum connectionTypeEnum {
		USB_SERIAL,
	};
	
	public enum connectionStateEnum {
		PHYSICAL_DISCONNECTED (0),
		PHYSICAL_CONNECTED (1),

		ABSTRACT_CONNECTING_SERIAL (2),
		ABSTRACT_IDLE (3);
		
		private final int value;
	    private connectionStateEnum(int value) {
	        this.value = value;
	    }

	    public int getValue() {
	        return value;
	    }

	    public static connectionStateEnum fromInt(int i) {
	        for (connectionStateEnum b : connectionStateEnum.values()) {
	            if (b.getValue() == i) { return b; }
	        }
	        return null;
	    }
	};
	
	public enum pinConfigEnum{
		ARDUINO_DEL_ANALOG_START(14),
		
		ENDOF_PINCONFIGNUM(255);
		
		/* TO BE ADDED PER ADDITIONAL COMMUNICATION CHANNELS */
		
		private final int value;
	    private pinConfigEnum(int value) {
	        this.value = value;
	    }

	    public int getValue() {
	        return value;
	    }

	    public static pinConfigEnum fromInt(int i) {
	        for (pinConfigEnum b : pinConfigEnum.values()) {
	            if (b.getValue() == i) { return b; }
	        }
	        return null;
	    }
	};
	
	public BiGXConnectionToDevice(String strId, int intId, connectionTypeEnum connectionType) 
			throws BiGXUnsupportedDeviceTypeException
	{
		this.connectionState = connectionStateEnum.PHYSICAL_DISCONNECTED;
		this.strID = strId;
		this.longID = intId;
		this.connectionType = connectionType;
		this.devicePinList = new Hashtable<Integer, Object>();
		
		switch(this.connectionType)
		{
		case USB_SERIAL:
			this.deviceHandler = new FirmataDevice(strId.split("\\|")[1]);
			break;
		default:
			throw new BiGXUnsupportedDeviceTypeException("The current device is not being supported");
		}
	}
	
	public void addListener(BiGXConnectionToDeviceEventListener argListener)
	{
		this.listeners.add(argListener);
	}
	
	public void removeListener(BiGXConnectionToDeviceEventListener argListener)
	{
		this.listeners.remove(argListener);
	}
	
	public void setListenerPinFlag(pinToListenEnum pinToListen, int pinDataType) throws Exception
	{
		switch(this.connectionType)
		{
		case USB_SERIAL:
			FirmataDevice device = ((FirmataDevice)this.deviceHandler);
			
			int pinIdx = 0;
			
			if(pinToListen.getValue() < pinToListenEnum.ANALOG_END.getValue())
			{
				pinIdx = pinConfigEnum.ARDUINO_DEL_ANALOG_START.getValue() + pinToListen.getValue();
			}
			else if(pinToListen.getValue() < pinToListenEnum.DIGITAL_END.getValue())
			{
				pinIdx = pinToListen.getValue() - pinToListenEnum.DIGITAL_00.getValue();
			}
			
			System.out.println("** pinIndex: " + pinIdx);
			
			if(device.getPinsCount() == 0){
				System.out.println("DeviceIsReady[" + device.isReady() + "]?");
				device.start();
				System.out.println("DeviceIsReady[" + device.isReady() + "]!");

				//TODO: Need to figure out why...
				throw new Exception("Device is not ready!");				
			}
			
			BiGXPin bigxpin = new BiGXPin();
			bigxpin.pinDataType = pinDataType;
			bigxpin.pin = new PinEventListener() {
				
				@Override
				public void onValueChange(IOEvent event) {
					for(BiGXConnectionToDeviceEventListener listener : listeners)
					{
						try{
							byte pinindex = event.getPin().getIndex();
							
							if(((pinindex == 16) || (pinindex == 17) || (pinindex == 20) || (pinindex == 21)) && (event.getValue() == 1024))
							{
								continue;
							}
							
//							if((event.getValue() != 1024) && (pinindex == 17))
//							{
//								System.out.println("listenersCount["+listeners.size()+"] PinVal[" + pinindex + "][" + event.getValue() + "]");
//							}
							
							if(pinindex >= pinConfigEnum.ARDUINO_DEL_ANALOG_START.getValue())
								pinindex -= pinConfigEnum.ARDUINO_DEL_ANALOG_START.getValue();
							else
								pinindex += pinToListenEnum.DIGITAL_00.getValue();
							
							BiGXPin theCurrentPin = (BiGXPin)devicePinList.get((int)pinindex);
							
							int tempPinValue = theCurrentPin.pinDataType;
							
							listener.onMessageReceiveFromDevice(null, strID + "|" + event.getPin().getIndex() + "|" + event.getValue() + "|" + tempPinValue);
						}
						catch(Exception ee)
						{
							ee.printStackTrace();
						}
					}
				}
				
				@Override
				public void onModeChange(IOEvent event) {
				}
			};
			
			this.devicePinList.put(pinToListen.getValue(), bigxpin);
			
			device.getPin(pinIdx).addEventListener((PinEventListener) bigxpin.pin);
			
			break;
		default:
			throw new BiGXUnsupportedDeviceTypeException("The current device is not being supported");
		}
	}
	
	public void freeListenerPinFlag(pinToListenEnum pinToListen) throws Exception
	{
		/* TODO: Need to Implement this part! in synchronous ways */
		switch(this.connectionType)
		{
		case USB_SERIAL:
			FirmataDevice device = ((FirmataDevice)this.deviceHandler);
			
			int pinIdx = 0;
			
			if(pinToListen.getValue() < pinToListenEnum.ANALOG_END.getValue())
			{
				pinIdx = pinConfigEnum.ARDUINO_DEL_ANALOG_START.getValue() + pinToListen.getValue();
			}
			else if(pinToListen.getValue() < pinToListenEnum.DIGITAL_END.getValue())
			{
				pinIdx = pinToListen.getValue() - pinToListenEnum.DIGITAL_00.getValue();
			}
			
			//System.out.println("** pinIndex: " + pinIdx);
			
			if(device.getPinsCount() == 0){
				throw new Exception("Device is not ready.");				
			}
			
			PinEventListener usbserialpinlistener;
			usbserialpinlistener = (PinEventListener)this.devicePinList.remove(pinToListen.getValue());
			
			device.getPin(pinIdx).removeEventListener(usbserialpinlistener);
			
			break;
		default:
			throw new BiGXUnsupportedDeviceTypeException("The current device is not being supported");
		} 
	}

	public void start() 
			throws BiGXUnsupportedDeviceTypeException, IOException, InterruptedException {
		switch(this.connectionType)
		{
		case USB_SERIAL:
			((FirmataDevice)this.deviceHandler).start();
			((FirmataDevice)this.deviceHandler).ensureInitializationIsDone();
			this.connectionState = connectionStateEnum.PHYSICAL_CONNECTED;
			break;
		default:
			throw new BiGXUnsupportedDeviceTypeException("The current device is not being supported");
		}
	}

	public void stop() throws IOException, BiGXUnsupportedDeviceTypeException {
		switch(this.connectionType)
		{
		case USB_SERIAL:
			((FirmataDevice)this.deviceHandler).stop();
			this.connectionState = connectionStateEnum.PHYSICAL_DISCONNECTED;
			break;
		default:
			throw new BiGXUnsupportedDeviceTypeException("The current device is not being supported");
		}
	}
	
	public void setValue(pinToListenEnum targetPin, long value) 
			throws Exception
	{
		switch(this.connectionType)
		{
		case USB_SERIAL:
			FirmataDevice device = ((FirmataDevice)this.deviceHandler);
			
			int pinIdx = 0;
			
			if(targetPin.getValue() < pinToListenEnum.ANALOG_END.getValue())
			{
				pinIdx = pinConfigEnum.ARDUINO_DEL_ANALOG_START.getValue() + targetPin.getValue();
			}
			else if(targetPin.getValue() < pinToListenEnum.DIGITAL_END.getValue())
			{
				pinIdx = targetPin.getValue() - pinToListenEnum.DIGITAL_00.getValue();
			}
			
			if(device.getPinsCount() == 0){
				throw new Exception("Device is not ready.");				
			}
			
			Pin pintowrite = device.getPin(pinIdx);
			
			if(pintowrite == null)
				throw new Exception("Pin is not ready");
			
			pintowrite.setValue(value);
			break;
		default:
			break;
//			throw new BiGXUnsupportedDeviceTypeException("The current device is not being supported");
		}
	}
	
	// TODO: Need to see!
	public long getUUID()
	{
		return this.longID;
	}
	
	// TODO: This is the function that needs to be implemented soon. :)
//	private void getDeviceSerialNumber() throws Exception
//	{
//		if(this.connectionState.getValue() >= connectionStateEnum.PHYSICAL_CONNECTED.getValue())
//		{
//			throw new Exception("Device is not ready for abstract connection establishment!");
//		}
//		
//		switch(this.connectionType)
//		{
//		case USB_SERIAL:
////			TODO: Here is the place where I left.   this.deviceHandler
//			this.connectionState = connectionStateEnum.PHYSICAL_CONNECTED;
//			break;
//		default:
//			throw new BiGXUnsupportedDeviceTypeException("The current device is not being supported");
//		}
//	}
}
