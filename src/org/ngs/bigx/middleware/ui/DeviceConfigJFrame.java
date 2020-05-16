package org.ngs.bigx.middleware.ui;

import javax.swing.JFrame;
import javax.swing.JScrollPane;

import org.ngs.bigx.dictionary.protocol.Specification;
import org.ngs.bigx.middleware.core.BiGXConnectionToDevice;
import org.ngs.bigx.middleware.core.BiGXConnectionToDevice.pinConfigEnum;
import org.ngs.bigx.middleware.core.BiGXConnectionToDeviceEventListener;
import org.ngs.bigx.middleware.core.BiGXPin.*;
import org.ngs.bigx.middleware.etc.BiGXStatsGUIInterface;
import org.ngs.bigx.middleware.exceptions.BiGXUnsupportedDeviceTypeException;

import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import java.awt.BorderLayout;
import java.awt.Event;
import java.io.IOException;
import java.util.Vector;

public class DeviceConfigJFrame extends JFrame {
	private static final long serialVersionUID = -7382434718108602536L;
	
	private BiGXConnectionToDevice connectionHandler;
	private JTable DeviceConfigTable;
	private BiGXConnectionToDeviceEventListener connectionEventListner;
	private BiGXStatsGUIInterface biGXStatsGUIInterface;
	
	public DeviceConfigJFrame(BiGXConnectionToDevice deviceHandler, String id, BiGXStatsGUIInterface biGXStatsGUIInterface) 
			throws	BiGXUnsupportedDeviceTypeException, 
					IOException, InterruptedException
	{
		super();
		
		this.connectionHandler = deviceHandler;
		this.biGXStatsGUIInterface = biGXStatsGUIInterface;
		
		DeviceConfigTable = new JTable();
		getContentPane().add(new JScrollPane(DeviceConfigTable), BorderLayout.CENTER);
		DeviceConfigTableModel model = new DeviceConfigTableModel();
//		model.addTableModelListener(new TableModelListener() {
//			
//			@Override
//			public void tableChanged(TableModelEvent ev) {
//				if(ev.getColumn() == 2)
//				{
//					try{
//						if((boolean) DeviceConfigTable.getValueAt(ev.getFirstRow(), ev.getColumn())){
//							connectionHandler.setListenerPinFlag(pinToListenEnum.fromInt(ev.getFirstRow()), specification.dataType.ROTATIONSTATE);
//						}
//						else{
//							connectionHandler.freeListenerPinFlag(pinToListenEnum.fromInt(ev.getFirstRow()));
//						}
//					}
//					catch(Exception ex)
//					{
//						ex.printStackTrace();
//					}
//				}
//			}
//		});
		
		DeviceConfigTable.setModel(model);
		
		connectionHandler.start();
		
		try {
			connectionHandler.setListenerPinFlag(pinToListenEnum.ANALOG_03, Specification.DataType.MOVE_FORWARDBACKWARD);
//			connectionHandler.setListenerPinFlag(pinToListenEnum.ANALOG_02, Specification.DataType.HEART);
			connectionHandler.setListenerPinFlag(pinToListenEnum.ANALOG_06, Specification.DataType.ROTATE);
			connectionHandler.setListenerPinFlag(pinToListenEnum.ANALOG_07, Specification.DataType.TORQE);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		connectionHandler.addListener(connectionEventListner = new BiGXConnectionToDeviceEventListener() {
			
			@Override
			public void onMessageReceiveFromDevice(Event event, String message) {
				String tempString[] = message.split("\\|");
				DeviceConfigTable.setValueAt(Integer.parseInt(tempString[3]), Integer.parseInt(tempString[2]) - pinConfigEnum.ARDUINO_DEL_ANALOG_START.getValue() + 1, 2);
				
				if( (Integer.parseInt(tempString[2]) - pinConfigEnum.ARDUINO_DEL_ANALOG_START.getValue() + 1) == 7 )
				{
					// Update RPM
					biGXStatsGUIInterface.updateRPM(Integer.parseInt(tempString[3]));
				}
				else if( (Integer.parseInt(tempString[2]) - pinConfigEnum.ARDUINO_DEL_ANALOG_START.getValue() + 1) == 8 )
				{
					// Update Wattage
					biGXStatsGUIInterface.updateWattage(Integer.parseInt(tempString[3]));
				}
			}
		});
	}
	
	public void resetConnection() throws IOException, BiGXUnsupportedDeviceTypeException
	{
		connectionHandler.stop();
		connectionHandler.removeListener(connectionEventListner);
	}
	
	public class DeviceConfigTableModel extends DefaultTableModel
	{
		private static final long serialVersionUID = -6207327784244122166L;

		public DeviceConfigTableModel()
		{
			super(new Object[][] {
					{0, "Analog 0", -1, 0, 0, false},
					{1, "Analog 1", -1, 0, 0, false},
					{2, "Analog 2", -1, 0, 0, false},
					{3, "Analog 3", -1, 0, 0, false},
					{4, "Analog 4", -1, 0, 0, false},
					{5, "Analog 5", -1, 0, 0, false},
					{6, "Analog 6", -1, 0, 0, false},
					{7, "Analog 7", -1, 0, 0, false},
					{8, "Analog 8", -1, 0, 0, false},
					{9, "Analog 9", -1, 0, 0, false},
					{11, "Digital 0", -1, 0, 0, false},
					{12, "Digital 1", -1, 0, 0, false},
					{13, "Digital 2", -1, 0, 0, false},
					{14, "Digital 3", -1, 0, 0, false},
					{15, "Digital 4", -1, 0, 0, false},
					{16, "Digital 5", -1, 0, 0, false},
					{17, "Digital 6", -1, 0, 0, false},
					{18, "Digital 7", -1, 0, 0, false},
					{19, "Digital 8", -1, 0, 0, false},
					{20, "Digital 9", -1, 0, 0, false},
					{21, "Digital 10", -1, 0, 0, false},
					{22, "Digital 11", -1, 0, 0, false},
					{23, "Digital 12", -1, 0, 0, false},
					{24, "Digital 13", -1, 0, 0, false},
					{25, "Digital 14", -1, 0, 0, false},
					{26, "Digital 15", -1, 0, 0, false},
					{27, "Digital 16", -1, 0, 0, false},
					},
				new String[]{"Index", "Sensor Name", "Raw Data", "Sensor Type", "Sensor Translated Data", "Enabled"});
		}

		@Override
		public Class<?> getColumnClass(int columnIndex)
		{
			@SuppressWarnings("rawtypes")
			Class clazz = String.class;
			switch (columnIndex) {
			case 1:
				clazz = Integer.class;
				break;
			case 2:
				clazz = Long.class;
				break;
			case 3:
				clazz = Integer.class;
				break;
			case 4:
				clazz = Integer.class;
				break;
			case 5:
				clazz = Boolean.class;
				break;
			}
			return clazz;
		}

		@Override
		public boolean isCellEditable(int row, int column)
		{
			return true;
		}

		@SuppressWarnings("unchecked")
		@Override
		public void setValueAt(Object aValue, int row, int column)
		{
			if (aValue instanceof Boolean && column == 4) {
				@SuppressWarnings("rawtypes")
				Vector rowData = (Vector)getDataVector().get(row);
				rowData.set(4, (Boolean)aValue);
				fireTableCellUpdated(row, column);
			}
			else
			{
				@SuppressWarnings("rawtypes")
				Vector rowData = (Vector)getDataVector().get(row);
				rowData.set(column, aValue);
				fireTableCellUpdated(row, column);
			}
		}

	}
}
