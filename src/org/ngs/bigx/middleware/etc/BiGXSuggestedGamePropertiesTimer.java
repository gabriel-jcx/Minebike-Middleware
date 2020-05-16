package org.ngs.bigx.middleware.etc;

import java.util.Timer;
import java.util.TimerTask;

import org.ngs.bigx.dictionary.protocol.Specification;
import org.ngs.bigx.middleware.core.BiGXMiddlewareCore;
import org.ngs.bigx.net.gameplugin.common.BiGXNetPacket;

public class BiGXSuggestedGamePropertiesTimer {
	private Timer questDesignSenderTimer;
	private TimerTask questDesignSenderTimerTask;
	
	private BiGXMiddlewareCore biGXMiddlewareCore;
	
	public static final int pingPeriod = 11 * 60 * 1000; // Every Eleven Minutes
	
	public BiGXSuggestedGamePropertiesTimer(BiGXMiddlewareCore biGXMiddlewareCore)
	{
		this.biGXMiddlewareCore = biGXMiddlewareCore;
	}
	
	public BiGXMiddlewareCore getBiGXMiddlewareCore()
	{
		return this.biGXMiddlewareCore;
	}
	
	public void startQuestDesignSenderTimer()
	{
		this.questDesignSenderTimerTask = new QuestDesignSenderTimerTask(this);
		
		if(questDesignSenderTimer != null)
		{
			questDesignSenderTimer.cancel();
		}
		
		questDesignSenderTimer = new Timer(true);
		questDesignSenderTimer.schedule(questDesignSenderTimerTask, 0, pingPeriod);
	}
	
	class QuestDesignSenderTimerTask extends TimerTask
	{	
		private BiGXSuggestedGamePropertiesTimer biGXSuggestedGamePropertiesTimer = null;
		
		public QuestDesignSenderTimerTask(BiGXSuggestedGamePropertiesTimer biGXSuggestedGamePropertiesTimer)
		{
			this.biGXSuggestedGamePropertiesTimer = biGXSuggestedGamePropertiesTimer;
		}

		@Override
		public void run() {
			// INITIATE THE PROCESS ONLY WHEN THE MIDDLEWARE IS LOGGED INTO THE SERVER
			if(BiGXMiddlewareCore.iXercisePatientInfoRetrieved != 1)
			{
				return;
			}
			
			byte data[] = new byte[] {0,0,0,0,0,0,0,0,0,};
			
			// Send the Quest Design
			biGXSuggestedGamePropertiesTimer.getBiGXMiddlewareCore().sendSuggestedGameDesign(new BiGXNetPacket(Specification.Command.REQ_CONNECT, 0, 0, data));
		}
	}
}
