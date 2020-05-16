package org.ngs.bigx.middleware.ui;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Random;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.Timer;
import javax.swing.JLabel;
import javax.swing.JComboBox;

import org.ngs.bigx.middleware.core.BiGXMiddlewareCore;
import org.ngs.bigx.net.gameplugin.common.BiGXNetPacket;

public class SimulatorUnitClass extends JPanel implements ActionListener 
{
	private static final long serialVersionUID = -5975618679637868919L;

	private Timer timer;
	private JButton timerStartButton;
	private JSplitPane mainSplitPane;
	private JPanel leftPane;
	private JPanel rightPane;
	private JComboBox<String> comboBoxCommand;
	private JComboBox<String> comboBoxDeviceID;
	private JComboBox<String> comboBoxDataType;
	private JCheckBox checkboxRandomValue;
	private JTextField textfieldTimerInterval;
	private JTextField textfieldDataMin;
	private JTextField textfieldDataMax;
	private JTextField textfieldSteps;
	private BiGXMiddlewareCore bigxcontext;
	
	public final int GAP = 5;
	
	private boolean isTimerRunning = false;
	private boolean isRandomValue = false;
	private int dataMin = 0;
	private int dataMax = 1024;
	private int dataCurrent = 0;
	private int changeSteps = 1;
//	private J
	
	public SimulatorUnitClass(BiGXMiddlewareCore bigxcontextarg)
	{
		this.timer = null;
		this.isTimerRunning = false;
		this.setLayout(new BorderLayout());
		this.addUI();
		this.bigxcontext = bigxcontextarg;
	}
	
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void addUI()
	{
		this.timerStartButton = new JButton();
		timerStartButton.setText("Start!");
		
		this.mainSplitPane = new JSplitPane();
		this.mainSplitPane.setResizeWeight(0.5);
		this.mainSplitPane.setDividerSize(1);
		this.mainSplitPane.setEnabled(false);
		
		this.leftPane = new JPanel();
		this.leftPane.setLayout(new BoxLayout(leftPane, BoxLayout.Y_AXIS));
		String[] comboBoxText = {
				"Command: 0x1101",
				"Command: 0x1102",
				"Command: 0x1001",
		};
		this.comboBoxCommand = new JComboBox(comboBoxText);
		
		String[] comboBoxDeviceIDText = {
				"Device ID: 0x01",
		};
		this.comboBoxDeviceID = new JComboBox(comboBoxDeviceIDText);
		
		String[] comboBoxDataTypeText = {
				"0x181A: Rotation State",
				"0x1005: Heart Rate",
				"0x181B: Move Forward/Backward",
				"0x181E: HR Duration",
		};
		this.comboBoxDataType = new JComboBox(comboBoxDataTypeText);
		
		this.checkboxRandomValue = new JCheckBox("Require Random Value", false); 
		
		this.textfieldDataMin = new JTextField();
		this.textfieldDataMin.setText("0");
		this.textfieldDataMax = new JTextField();
		this.textfieldDataMax.setText("1024");
		this.textfieldSteps = new JTextField();
		this.textfieldSteps.setText("1");

		this.leftPane.add(comboBoxCommand);
		this.leftPane.add(comboBoxDeviceID);
		this.leftPane.add(comboBoxDataType);
		this.leftPane.add(checkboxRandomValue);
		this.leftPane.add(textfieldDataMin);
		this.leftPane.add(textfieldDataMax);
		this.leftPane.add(textfieldSteps);
		
		this.rightPane = new JPanel();
		this.textfieldTimerInterval = new JTextField();
		this.textfieldTimerInterval.setText("1000");
		this.rightPane.setLayout(new BoxLayout(rightPane, BoxLayout.Y_AXIS));
		this.rightPane.add(new JLabel("Timer Interval (ms)"));
		this.rightPane.add(this.textfieldTimerInterval);
		this.rightPane.add(this.timerStartButton);
		
		this.mainSplitPane.setLeftComponent(this.leftPane);
		this.mainSplitPane.setRightComponent(this.rightPane);
		this.add(mainSplitPane);
		
		this.timerStartButton.addActionListener(new ActionListener() {
			
			@Override
			public void actionPerformed(ActionEvent e) {
				try{
					if(isTimerRunning)
					{
						stopTimer();
						timerStartButton.setText("Start Timer");
						
						checkboxRandomValue.setEnabled(true);
						comboBoxDataType.setEnabled(true);
						comboBoxCommand.setEnabled(true);
						comboBoxDeviceID.setEnabled(true);
						textfieldTimerInterval.setEditable(true);
						textfieldDataMin.setEditable(true);
						textfieldDataMax.setEditable(true);
						textfieldSteps.setEditable(true);
					}
					else{
						int interval = Integer.parseInt(textfieldTimerInterval.getText());
						
						if(Integer.parseInt(textfieldDataMax.getText()) <= Integer.parseInt(textfieldDataMin.getText()))
						{
							throw new Exception("Max value should be larger than minimum.");
						}
						
						changeSteps = Integer.parseInt(textfieldSteps.getText());
							
						dataMax = Integer.parseInt(textfieldDataMax.getText());
						dataMin = Integer.parseInt(textfieldDataMin.getText());
						
						startTimer(interval);
						timerStartButton.setText("Stop Timer");

						checkboxRandomValue.setEnabled(false);
						comboBoxDataType.setEnabled(false);
						comboBoxCommand.setEnabled(false);
						comboBoxDeviceID.setEnabled(false);
						textfieldTimerInterval.setEditable(false);
						textfieldDataMin.setEditable(false);
						textfieldDataMax.setEditable(false);
						textfieldSteps.setEditable(false);
					}
				}
				catch (Exception ee)
				{
					ee.printStackTrace();
				}
			}
		});
		
	}
	
	public synchronized void startTimer(int delay) throws Exception
	{
		if(this.timer != null)
		{
			throw new Exception("Simulator Timer is running.");
		}
		
		this.timer = new Timer(delay, this);
		this.isTimerRunning = true;
		this.isRandomValue = this.checkboxRandomValue.isSelected();
		this.timer.start();
	}
	
	public synchronized void stopTimer() throws Exception
	{
		if(this.timer == null)
		{
			throw new Exception("Simulator Timer is no running.");
		}
		
		this.timer.stop();
		this.timer = null;
		this.isTimerRunning = false;
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		int dataTypeValue = 0;
		switch(comboBoxDataType.getSelectedIndex())
		{
		case 0:
			dataTypeValue = 0x181A;
			break;
		case 1:
			dataTypeValue = 0x1005;
			break;
		case 2:
			dataTypeValue = 0x181B;
			break;
		case 3:
			dataTypeValue = 0x181E;
			break;
		default:
			dataTypeValue = 0x1000;
			break;
		};
		
		try{
			if(this.isRandomValue == false)
			{
				int value = (this.dataCurrent + this.changeSteps);
				
				if(this.dataMin >= 0)
				{
					if(value >= this.dataMax)
					{
						value %= (this.dataMax - this.dataMin);
						value += this.dataMin;
					}
				}
				else{
					value -= this.dataMin;

					if(value >= (this.dataMax - this.dataMin))
					{
						value %= (this.dataMax - this.dataMin);
					}
					
					value += this.dataMin;
				}
				
				this.dataCurrent = value;
				
				System.out.println("[BiGXSimulator] Current value: " + value);
				
				byte data[] = {0,(byte) (value & 0xFF),(byte) ((value & 0xFF00)>>8),0,0,0,0,0,0};
				
				bigxcontext.sendSimulatorMessage(new BiGXNetPacket(
						comboBoxCommand.getSelectedIndex() + 0x1001, 
						comboBoxDeviceID.getSelectedIndex() + 0x01, 
						dataTypeValue, 
						data));
			}
			else
			{
				Random rand = new Random();
				
				int value = rand.nextInt((this.dataMax - this.dataMin)) + this.dataMin;
				
				if(value < 0)
				{
					value *= -1;
					value += 512;
				}
				
//				System.out.println("[BiGXSimulator] Generate Ramdom value: " + value);
				
				byte data[] = {0,(byte) (value & 0xFF),(byte) ((value & 0xFF00)>>8),0,0,0,0,0,0};
				
				bigxcontext.sendSimulatorMessage(new BiGXNetPacket(
						comboBoxCommand.getSelectedIndex() + 0x1001, 
						comboBoxDeviceID.getSelectedIndex() + 0x01, 
						dataTypeValue, 
						data));
			}
		}
		catch (Exception ee)
		{
			ee.printStackTrace();
		}
	}
	
}
