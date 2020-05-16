package org.ngs.bigx.middleware.ui;

import javax.swing.JFrame;
import org.ngs.bigx.middleware.core.BiGXMiddlewareCore;

import javax.swing.JSplitPane;
import java.awt.BorderLayout;

public class SimulatorJFrame extends JFrame {
	private static final long serialVersionUID = -3167348851520306202L;
	
	private SimulatorUnitClass simulatorUnitArray[];
	private BiGXMiddlewareCore bigxcontext;
	
	public final int numberOfSimulators = 4; 

	public SimulatorJFrame(BiGXMiddlewareCore bigxContextArg) {
		this.simulatorUnitArray = new SimulatorUnitClass[numberOfSimulators];
		
		for(int i=0; i<this.simulatorUnitArray.length; i++)
		{
			this.simulatorUnitArray[i] = new SimulatorUnitClass(this.bigxcontext);
		}
		
		this.bigxcontext = bigxContextArg;
		
		JSplitPane splitPane = new JSplitPane();
		splitPane.setResizeWeight(0.5);
		splitPane.setOrientation(JSplitPane.VERTICAL_SPLIT);
		
		JSplitPane splitPane_top = new JSplitPane();
		JSplitPane splitPane_down = new JSplitPane();
		
		getContentPane().add(splitPane, BorderLayout.CENTER);
		
		SimulatorUnitClass contentPane;
		splitPane_top.setResizeWeight(0.5);
		splitPane_top.setEnabled(false);
		splitPane_top.setOrientation(JSplitPane.VERTICAL_SPLIT);
		contentPane = new SimulatorUnitClass(this.bigxcontext);
		splitPane_top.setTopComponent(contentPane); 
		contentPane = new SimulatorUnitClass(this.bigxcontext);
		splitPane_top.setBottomComponent(contentPane);
		splitPane.setTopComponent(splitPane_top);
		
		splitPane_down.setEnabled(false);
		splitPane_down.setOrientation(JSplitPane.VERTICAL_SPLIT);
		splitPane_down.setResizeWeight(0.5);
		contentPane = new SimulatorUnitClass(this.bigxcontext);
		splitPane_down.setTopComponent(contentPane);
		contentPane = new SimulatorUnitClass(this.bigxcontext);
		splitPane_down.setBottomComponent(contentPane);
		splitPane.setBottomComponent(splitPane_down);
	}
}









