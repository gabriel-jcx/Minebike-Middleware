package org.ngs.bigx.middleware.etc;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Timer;
import java.util.TimerTask;

import org.ngs.bigx.dictionary.objects.game.GameServerList;
import org.ngs.bigx.middleware.core.BiGXConnectionToBackend;
import org.ngs.bigx.middleware.core.BiGXMiddlewareCore;

import com.google.gson.Gson;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.UserInfo;

public class BiGXGameServerDownloader {
	public BiGXMiddlewareCore context;

	public static final int serverportnumber = LinuxInterfaceServer.serverportnumber;
	public static final int timeoutGameServer = 3000;
	
	private Timer timer;
	
	public enum CUSTOMCOMMAND 
	{
		GETGAMESERVERLIST,
		LEASEGAMESERVERREQUEST,
		RELEASEGAMESERVERREQUEST,
	};
	
	public BiGXGameServerDownloader(BiGXMiddlewareCore context)
	{
		this.context = context;
	}
	
	public void startCheckingGameServerList() {
		System.out.println("[BiGX] startCheckingGameServerList()");
		sendCustomCommand(CUSTOMCOMMAND.GETGAMESERVERLIST, null);
		
		timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() 
			{
				@Override
				public void run() {
				    if(context.getiXerciseAccountName().equals("")) {
				    	this.cancel();
				    }
				    else if(context.getiXercisePassword().equals("")) {
				    	this.cancel();
				    }
				    
					sendCustomCommand(CUSTOMCOMMAND.GETGAMESERVERLIST, null);
				}
			},
		0, timeoutGameServer);
	}
	
	public void stopCheckingGameServerList() {
		if(timer!= null)
			timer.cancel();
		else
			return;
	}
	
	@SuppressWarnings("unused")
	public void sendCustomCommand(CUSTOMCOMMAND commandenum, String[] args)
	{
	    FileInputStream fis=null;
	    
	    String host = BiGXMiddlewareCore.dataMangementSystemHost;
	    String serveraccount = this.context.getiXerciseAccountName();
	    final String serverpassword = this.context.getiXercisePassword();
	    
		try{
		      JSch jsch=new JSch();
		      Session session=jsch.getSession(serveraccount, host, BiGXConnectionToBackend.ScpServerPort);
		      session.setUserInfo(new UserInfo() {
			
					@Override
					public void showMessage(String arg0) {
					}
					
					@Override
					public boolean promptYesNo(String arg0) {
						return true;
					}
					
					@Override
					public boolean promptPassword(String arg0) {
						if(serverpassword != "")
							return true;
						else
							return false;
					}
					
					@Override
					public boolean promptPassphrase(String arg0) {
						return true;
					}
					
					@Override
					public String getPassword() {
						return serverpassword;
					}
					
					@Override
					public String getPassphrase() {
						return null;
					}
		      });
		      session.setPassword(serverpassword);
		      session.connect();

		      // exec 'scp -t rfile' remotely
		      String command = "";
		      String ip = "128.195.55.199";
//		      String ip = "192.168.0.53";
			    try {
			    	boolean ipfound = false;
			        Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
			        while (interfaces.hasMoreElements()) {
			            NetworkInterface iface = interfaces.nextElement();
			            // filters out 127.0.0.1 and inactive interfaces
			            if (iface.isLoopback() || !iface.isUp())
			                continue;

			            Enumeration<InetAddress> addresses = iface.getInetAddresses();
			            while(addresses.hasMoreElements()) {
			                InetAddress addr = addresses.nextElement();
			                ip = addr.getHostAddress();
			                
			                if(ip.split("\\.").length == 4)
			                {
			                	if(!ip.split("\\.")[0].equals("192"))
			                	{
			                		ipfound = true;
					                break;
			                	}
			                }
			                
			                if(ipfound)
			                	break;
			            }
			        }
			    } catch (SocketException e) {
			        throw new RuntimeException(e);
			    }

		      switch(commandenum)
		      {
		      case GETGAMESERVERLIST:
		    	  command="bigx-getgameserverlist " + serveraccount + " " + serverpassword;
		    	  break;
		      case LEASEGAMESERVERREQUEST:
		    	  command="bigx-leasegameserver " + serveraccount + " " + serverpassword + " " + args[0] + " " + ip + " " + "lease";
		    	  break;
		      case RELEASEGAMESERVERREQUEST:
		    	  command="bigx-leasegameserver " + serveraccount + " " + serverpassword + " " + args[0] + " " + ip + " " + "release";
		    	  break;
		      };
		      
		      Channel channel=session.openChannel("exec");
		      ((ChannelExec)channel).setCommand(command);

		      // get I/O streams for remote scp
		      OutputStream out=channel.getOutputStream();
		      InputStream in=channel.getInputStream();

		      channel.connect();
		      
		      byte[] buffer = new byte[10240];
		      in.read(buffer);
		      String output = (new String(buffer)).trim();
		      
		      out.close();
		      
		      switch(commandenum)
		      {
		      case GETGAMESERVERLIST:
		    	  if( (output.length() != 0) && (!output.equals("{}")) )
					{
						this.context.setGameServerList(new Gson().fromJson(output, GameServerList.class));
					}
					else if(output.equals("{}"))
					{
						this.context.setGameServerList(new GameServerList());
					}
		    	  break;
		      case LEASEGAMESERVERREQUEST:
		    	  break;
		      case RELEASEGAMESERVERREQUEST:
		    	  break;
		      };
		      
		      channel.disconnect();
		      session.disconnect();
		    }
		    catch(Exception e){
		    	e.printStackTrace();
		    	
			    try{
			    	if(fis != null)
			    		fis.close();
		    	}
			    catch(Exception ee){
			    	ee.printStackTrace();
		    	}
		    }
	}

	public int checkAck(InputStream in) throws IOException{
		int b=in.read();
		// b may be 0 for success,
		//					1 for error,
		//					2 for fatal error,
		//					-1
		if(b==0) return b;
		if(b==-1) return b;

		if(b==1 || b==2){
			StringBuffer sb=new StringBuffer();
			int c;
			do {
				c=in.read();
				sb.append((char)c);
			}
			while(c!='\n');
			if(b==1){ // error
				System.out.print(sb.toString());
			}
			if(b==2){ // fatal error
				System.out.print(sb.toString());
			}
		}
		return b;
	}
}
