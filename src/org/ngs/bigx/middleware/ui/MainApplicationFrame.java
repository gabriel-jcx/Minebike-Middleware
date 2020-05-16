package org.ngs.bigx.middleware.ui;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.GridLayout;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JMenuItem;

import jssc.SerialPortList;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JSplitPane;
import javax.swing.JLabel;
import javax.swing.JComboBox;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JList;

import org.ngs.bigx.data.BiGXDataEntity;
import org.ngs.bigx.data.BiGXDataFileStream;
import org.ngs.bigx.dictionary.objects.clinical.BiGXPatientPrescription;
import org.ngs.bigx.middleware.core.BiGXConnectionToDevice;
import org.ngs.bigx.middleware.core.BiGXConnectionToDevice.connectionTypeEnum;
import org.ngs.bigx.middleware.core.BiGXMiddlewareCore;
import org.ngs.bigx.middleware.etc.BiGXStatsGUIInterface;
import org.ngs.bigx.middleware.etc.LinuxInterfaceServer;
import org.ngs.bigx.middleware.etc.ScpTo;
import org.ngs.bigx.middleware.exceptions.BiGXDeviceIDOverlapException;
import org.ngs.bigx.middleware.exceptions.BiGXUnsupportedDeviceTypeException;

public class MainApplicationFrame implements BiGXStatsGUIInterface {
	private JFrame MainFrame;
	private List<String> USBSerialDeviceList;
	private LinuxInterfaceServer linuxInterfaceServer;
	private Thread linuxInterfaceServerThread;
	private MainApplicationFrame mainApplicationFrame;
	
	private static JTextField HRValue;
	private static JTextField RPMValue;
	private static JTextField WValue;
	private static JTextField textResistance;
	
	private enum ConnectionTypeEnum {
		NONE(0),
		USB_SERIAL(1);
		
		private final int value;
	    private ConnectionTypeEnum(int value) {
	        this.value = value;
	    }

	    public int getValue() {
	        return value;
	    }

	    public static ConnectionTypeEnum fromInt(int i) {
	        for (ConnectionTypeEnum b : ConnectionTypeEnum .values()) {
	            if (b.getValue() == i) { return b; }
	        }
	        return null;
	    }
		};
	private ConnectionTypeEnum CurrentTableContents;
	private JList<String> ConnectionListList;
	private DefaultListModel<String> ConnectionListListModel;
	private static BiGXMiddlewareCore BiGXCoreContext;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					MainApplicationFrame window = new MainApplicationFrame();
					
					window.MainFrame.setVisible(true);
					statsGUI();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	} 
	
	
	/**
	 * creates the window to display the hr, wattage, and rpm
	 */
	
	private static int hrValue = 1;
	
	public static void updateHRValue(int value)
	{
		hrValue = value;
	}
	
	public static void statsGUI()
	{
		JFrame stats = new JFrame("Stats");
		stats.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		GridLayout layout = new GridLayout(7,1);
		Container pane = stats.getContentPane();
		
		pane.setLayout(layout);
		
		JTextField HR = new JTextField("HR: ");
		//HR.setPreferredSize(new Dimension(100,30));
		HR.setEditable(false);
		pane.add(HR);
		
		HRValue = new JTextField();
		//HRValue.setPreferredSize(new Dimension(100,30));
		stats.getContentPane().add(HRValue);
		
		
		JTextField RPM = new JTextField("RPM: ");
		//RPM.setPreferredSize(new Dimension(100,30));
		RPM.setEditable(false);
		stats.getContentPane().add(RPM);
		
		RPMValue = new JTextField();
		//RPMValue.setPreferredSize(new Dimension(100,30));
		stats.getContentPane().add(RPMValue);
		
		
		JTextField WATTAGE = new JTextField("WATTAGE: ");
		//WATTAGE.setPreferredSize(new Dimension(100,30));
		WATTAGE.setEditable(false);
		stats.getContentPane().add(WATTAGE);
		
		WValue = new JTextField();
		//WValue.setPreferredSize(new Dimension(100,30));
		stats.getContentPane().add(WValue);

		pane = new JPanel();
		layout = new GridLayout(1,3);
		pane.setLayout(layout);
		JButton btnDecrease = new JButton("Start Communication");		
		JButton btnIncrease = new JButton("Start Communication");
		textResistance = new JTextField("0");
		
		btnDecrease.setText("-");
		btnIncrease.setText("+");
		
		btnDecrease.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent arg0) {
			}
			
			@Override
			public void mousePressed(MouseEvent arg0) {
			}
			
			@Override
			public void mouseExited(MouseEvent arg0) {
			}
			
			@Override
			public void mouseEntered(MouseEvent arg0) {
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				int resistance = Integer.parseInt(textResistance.getText());
				resistance--;
				if(resistance < 0)
					resistance = 0;
				BiGXCoreContext.setResistanceValue(resistance);
				textResistance.setText("" + resistance);
			}
		});
		btnIncrease.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent arg0) {
			}
			
			@Override
			public void mousePressed(MouseEvent arg0) {
			}
			
			@Override
			public void mouseExited(MouseEvent arg0) {
			}
			
			@Override
			public void mouseEntered(MouseEvent arg0) {
			}
			
			@Override
			public void mouseClicked(MouseEvent arg0) {
				int resistance = Integer.parseInt(textResistance.getText());
				resistance++;
				if(resistance > 10)
					resistance = 10;
				BiGXCoreContext.setResistanceValue(resistance);
				textResistance.setText("" + resistance);
			}
		});

		pane.add(btnDecrease);
		pane.add(textResistance);
		pane.add(btnIncrease);
		
		stats.getContentPane().add(pane);
		
		stats.setPreferredSize(new Dimension(300, 200));
		stats.setLocationRelativeTo(null);
		stats.pack();
		stats.setVisible(true);
		
		
	}

	/**
	 * Create the application.
	 */
	public MainApplicationFrame() {
		initialize();
    	
    	this.linuxInterfaceServer = new LinuxInterfaceServer(LinuxInterfaceServer.serverportnumber, BiGXCoreContext, this);
		
		if(this.linuxInterfaceServer.getIsReady()){
			this.linuxInterfaceServer.setReady(true);
	    	this.linuxInterfaceServerThread = new Thread(this.linuxInterfaceServer);
	    	this.linuxInterfaceServerThread.start();
	    	
	    	System.out.println("[Command Interfacer Server Port Number] " + LinuxInterfaceServer.serverportnumber);
			return;
		}
	}
	
    public static String[] requestPort() {
        String[] portNames = SerialPortList.getPortNames();
        
        if (portNames.length == 0) {
            JOptionPane.showMessageDialog(null, "Cannot find any serial port", "Error", JOptionPane.ERROR_MESSAGE);
        }
        
        return portNames;
    }

	/**
	 * Initialize the contents of the frame.
	 */
	private void initialize() {
		mainApplicationFrame = this;
		MainFrame = new JFrame();
		MainFrame.setTitle("BiGX Configuration Tool");
		MainFrame.setBounds(100, 100, 549, 420);
		MainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		JMenuBar menuBar = new JMenuBar();
		menuBar.setBackground(Color.LIGHT_GRAY);
		MainFrame.setJMenuBar(menuBar);
		
		JMenu mnFile = new JMenu("File");
		mnFile.setBackground(Color.LIGHT_GRAY);
		menuBar.add(mnFile);
		
		JMenuItem mntmLoginToServer = new JMenuItem("Log in");
		mntmLoginToServer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				LoginJFrame newFrame = new LoginJFrame(BiGXCoreContext);
	            newFrame.setTitle("Login to iXercise");
	            newFrame.setBounds(120, 120, 300, 300);
	            newFrame.setVisible(true);
	            try {
					newFrame.initLoginJFrame();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
			}
		});
		mnFile.add(mntmLoginToServer);
		
		JMenuItem mntmLogoutFromServer = new JMenuItem("Log out");
		mntmLogoutFromServer.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				
			}
		});
		mnFile.add(mntmLogoutFromServer);
		
		JMenu mnScanADevice = new JMenu("Scan devices");
		mnScanADevice.setBackground(Color.LIGHT_GRAY);
		menuBar.add(mnScanADevice);
		
		JMenuItem mntmUsbserial = new JMenuItem("USB-Serial");
		mnScanADevice.add(mntmUsbserial);
		
		JMenu mnView = new JMenu("View");
		mnView.setBackground(Color.LIGHT_GRAY);
		menuBar.add(mnView);
		
		JMenuItem mntmLog = new JMenuItem("Log");
		mnView.add(mntmLog);
		
		JMenu mnSimulator = new JMenu("Simulator");
		mnSimulator.setBackground(Color.LIGHT_GRAY);
		menuBar.add(mnSimulator);
		
		JMenuItem mntmOpen = new JMenuItem("Open");
		mntmOpen.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				SimulatorJFrame newFrame = new SimulatorJFrame(BiGXCoreContext);
	            newFrame.setTitle("Simulator");
	            newFrame.setBounds(120, 120, 549, 820);
	            newFrame.setVisible(true);	
			}
		});
		mnSimulator.add(mntmOpen);
		
		JMenu mnTest = new JMenu("Test");
		mnTest.setBackground(Color.LIGHT_GRAY);
		menuBar.add(mnTest);
		
		JMenuItem mntmSendToDms = new JMenuItem("Send to DMS");
		mntmSendToDms.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				BiGXDataFileStream testStream = new BiGXDataFileStream();
				BiGXDataEntity testDataEntity = new BiGXDataEntity();
				testDataEntity.setDatasize(4);
				testDataEntity.setDate("2016.11.28");
				testDataEntity.setEncoding("UTF-8");
				testDataEntity.setEquipmentID("EQ01");
				testDataEntity.setGameID("GM01");
				testDataEntity.setLocation("128.0.0.1");
				testDataEntity.setPatientid("PAT11");
				testDataEntity.setSensorID("SSO01");
				testDataEntity.setTime("13:14:56");
				testDataEntity.setVersion("00.01.001");
				testDataEntity.setBiGXPatientPrescription(new BiGXPatientPrescription());
				testDataEntity.makeSessionID();
				
				String outputFileName = testDataEntity.getSessionID() + testDataEntity.getEquipmentID() + testDataEntity.getSensorID() + "_" + System.currentTimeMillis() + ".txt";
				outputFileName = outputFileName.replaceAll(":", "_");
				
				testStream.encodeToBiGXDataFile(testDataEntity, BiGXMiddlewareCore.biGXGameTags, "src/resources/test/testMergedRawData.txt", 
						"src/resources/test/" + outputFileName);
				
				System.out.println("[Tags] sz[" + BiGXMiddlewareCore.biGXGameTags.size() + "]");
				
				// TODO: Need to implement Jsch to upload the output file.
				ScpTo testScpTo = new ScpTo(BiGXCoreContext);
				BiGXCoreContext.setiXerciseAccountName("jackie.chen@gmail.com");
				BiGXCoreContext.setiXercisePassword("jackie");
				testScpTo.sendFile(new String[]{"src/resources/test/" + outputFileName, 
						"testLogin@" + BiGXMiddlewareCore.dataMangementSystemHost + ":/" + outputFileName});
			}
		});
		mnTest.add(mntmSendToDms);
		
		JMenu mnHelp = new JMenu("Help");
		mnHelp.setBackground(Color.LIGHT_GRAY);
		menuBar.add(mnHelp);
		
		JMenuItem mntmBigxWebpage = new JMenuItem("BiGX Help page");
		mnHelp.add(mntmBigxWebpage);
		
		JMenuItem mntmAboutBigx = new JMenuItem("About BiGX");
		mnHelp.add(mntmAboutBigx);
		
		/* BiGX Context Pointer */
		this.BiGXCoreContext = new BiGXMiddlewareCore();
		
		JSplitPane MainContentSplitPane = new JSplitPane();
		MainContentSplitPane.setDividerSize(1);
		MainContentSplitPane.setEnabled(false);
		MainContentSplitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		MainFrame.getContentPane().add(MainContentSplitPane, BorderLayout.CENTER);
		
		JSplitPane ConnectionTypeSplitPane = new JSplitPane();
		ConnectionTypeSplitPane.setEnabled(false);
		ConnectionTypeSplitPane.setDividerSize(1);
		MainContentSplitPane.setLeftComponent(ConnectionTypeSplitPane);
		
		JLabel lblConnectionType = new JLabel(" Connection Type:    ");
		ConnectionTypeSplitPane.setLeftComponent(lblConnectionType);
		
		JComboBox<String> ConnectionTypeSelectionComboBox = new JComboBox<String>();
		ConnectionTypeSelectionComboBox.setModel(new DefaultComboBoxModel<String>(new String[] {"<Please select connection type>", "USB_SERIAL"}));
		ConnectionTypeSplitPane.setRightComponent(ConnectionTypeSelectionComboBox);
		ConnectionTypeSelectionComboBox.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				@SuppressWarnings("unchecked")
				JComboBox<String> cb = (JComboBox<String>)e.getSource();
				updateConnectionType(ConnectionTypeEnum.fromInt(cb.getSelectedIndex()));
			}
		});
		JScrollPane scrollPane = new JScrollPane();
		MainContentSplitPane.setRightComponent(scrollPane);
		
		ConnectionListListModel = new DefaultListModel<>();
		ConnectionListList = new JList<String>(ConnectionListListModel);
		scrollPane.setViewportView(ConnectionListList);
		ConnectionListList.addMouseListener(new MouseListener() {
			
			@Override
			public void mouseReleased(MouseEvent e) {
			}
			
			@Override
			public void mousePressed(MouseEvent e) {
			}
			
			@Override
			public void mouseExited(MouseEvent e) {
			}
			
			@Override
			public void mouseEntered(MouseEvent e) {
			}
			
			@Override
			public void mouseClicked(MouseEvent e) {
				@SuppressWarnings("rawtypes")
				JList list = (JList)e.getSource();
		        if (e.getClickCount() == 2) {
		            list.getSelectedValue();
		            BiGXConnectionToDevice BiGXDevice;
	            	
		            try {
		            	String DeviceID = "USBSERIAL|"+list.getSelectedValue();
		            	BiGXDevice = BiGXCoreContext.addDevice(DeviceID, connectionTypeEnum.USB_SERIAL);//, new FirmataDevice((String) list.getSelectedValue()));
			            
		            	DeviceConfigJFrame newFrame = new DeviceConfigJFrame(BiGXDevice, DeviceID, mainApplicationFrame);
			            newFrame.setTitle(DeviceID.replace("|", " : "));
			            newFrame.setBounds(120, 120, 549, 420);
			            newFrame.setVisible(true);		
			            
			            linuxInterfaceServer.setBiGXDevice(BiGXDevice);
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
			}
		});
		
		/* Init USB-Serial Device List */
		USBSerialDeviceList = new ArrayList<String>();
		
		this.CurrentTableContents = ConnectionTypeEnum.NONE;
		
		/* Scan Action */
		mntmUsbserial.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				String str[] = requestPort();
//				String strMessage = "";
//				for(String portString : str){
//					strMessage += "Port: " + portString + "\n";
//				}
//				JOptionPane.showMessageDialog(null, strMessage);

				/* Clear the current USB Serial Device List */
				USBSerialDeviceList.clear();
				
				/* Add found devices to the menu */
				for(String portString : str){
					USBSerialDeviceList.add(portString);
					updateConnectionTable(ConnectionTypeEnum.USB_SERIAL);
				}
			}
		});

		ConnectionTypeSelectionComboBox.setSelectedIndex(1);
	}
	
	private boolean updateConnectionTable(ConnectionTypeEnum connectionType)
	{
		if(this.CurrentTableContents != connectionType)
			return false;

		/* To update the list we have to remove all */
		this.ConnectionListListModel.removeAllElements();
		
		switch(connectionType)
		{
		case NONE:
			/* Probably we can do notification. */
			break;
		case USB_SERIAL:
			for(String usbserialdevicename: this.USBSerialDeviceList)
			{
				ConnectionListListModel.addElement(usbserialdevicename);
			}
			break;
		default:
			break;
		}
		
		return true;
	}
	
	private boolean updateConnectionType(ConnectionTypeEnum connectionType)
	{
		this.CurrentTableContents = connectionType;
		updateConnectionTable(connectionType);
		
		return true;
	}

	@Override
	public void updateRPM(int rpm) {
		// TODO Auto-generated method stub
		RPMValue.setText("" + rpm);
	}

	@Override
	public void updateWattage(int wattage) {
		// TODO Auto-generated method stub
		WValue.setText("" + wattage);
	}

	@Override
	public void updateHeartRate(int heartrate) {
		// TODO Auto-generated method stub
		HRValue.setText("" + heartrate);
	}

	@Override
	public void updateResistance(int resistance) {
		textResistance.setText("" + resistance);
	}
}
