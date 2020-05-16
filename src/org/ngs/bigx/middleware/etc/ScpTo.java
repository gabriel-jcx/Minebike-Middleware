package org.ngs.bigx.middleware.etc;

/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/**
 * This program will demonstrate the file transfer from local to remote.
 *	 $ CLASSPATH=.:../build javac ScpTo.java
 *   $ CLASSPATH=.:../build java ScpTo file1 user@remotehost:file2
 * You will be asked passwd. 
 * If everything works fine, a local file 'file1' will copied to
 * 'file2' on 'remotehost'.
 *
 */
import com.google.gson.Gson;
import com.jcraft.jsch.*;

import java.io.*;

import org.ngs.bigx.dictionary.objects.clinical.BiGXPatientInfo;
import org.ngs.bigx.dictionary.objects.game.BiGXSuggestedGameProperties;
import org.ngs.bigx.middleware.core.BiGXConnectionToBackend;
import org.ngs.bigx.middleware.core.BiGXMiddlewareCore;

public class ScpTo{
	
	public BiGXMiddlewareCore bigxcontext;
	
	public enum CUSTOMCOMMAND 
	{
		GETPATIENTINFO,
		GETGAMEDESIGN,
	};
	
	public ScpTo(BiGXMiddlewareCore bigxcontext)
	{
		this.bigxcontext = bigxcontext;
	}
	
	public void logincheck(String user, String host)
	{
		boolean connected = true;
		
		try
		{	
			JSch jsch=new JSch();
			Session session=jsch.getSession(user, host, BiGXConnectionToBackend.ScpServerPort);
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
					if(bigxcontext.getiXercisePassword() != "")
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
					return bigxcontext.getiXercisePassword();
				}
				
				@Override
				public String getPassphrase() {
					return null;
				}
			});
			session.setPassword(bigxcontext.getiXercisePassword());
			session.connect();
			session.disconnect();
		}
		catch (JSchException e)
		{
			connected = false;
			e.printStackTrace();
		}
		finally
		{
			if(connected)
			{
				BiGXMiddlewareCore.iXerciseBackendConnected = 1;
			}
			else
			{
				BiGXMiddlewareCore.iXerciseBackendConnected = -1;
			}
		}
	}
	
	/**
	 * The function sends out the source file to the destination
	 * @param arg arg[0]: source file, arg[1]: user@remotehost:file2
	 */
	public boolean sendFile(String[] arg)
	{
		if(arg.length!=2){
		      System.err.println("usage: java ScpTo file1 user@remotehost:file2");
		}

	    FileInputStream fis=null;
	    boolean returnValue = true;

	    System.out.println("arg[0][" + arg[0] + "]");
	    System.out.println("arg[1][" + arg[1] + "]");
	    
		try{
			String lfile=arg[0];
			arg[1]=arg[1].substring(arg[1].indexOf('@')+1);
			String host=arg[1].substring(0, arg[1].indexOf(':'));
			String rfile=arg[1].substring(arg[1].indexOf(':')+1);

		      JSch jsch=new JSch();
		      Session session=jsch.getSession(bigxcontext.getiXerciseAccountName(), host, BiGXConnectionToBackend.ScpServerPort);
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
						if(bigxcontext.getiXercisePassword() != "")
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
						return bigxcontext.getiXercisePassword();
					}
					
					@Override
					public String getPassphrase() {
						return null;
					}
		      });
		      session.setPassword(bigxcontext.getiXercisePassword());
		      session.connect();

		      boolean ptimestamp = false;

		      // exec 'scp -t rfile' remotely
		      String command="scp " + (ptimestamp ? "-p " :"") +"-t "+rfile;
		      Channel channel=session.openChannel("exec");
		      ((ChannelExec)channel).setCommand(command);

		      // get I/O streams for remote scp
		      OutputStream out=channel.getOutputStream();
		      InputStream in=channel.getInputStream();

		      channel.connect();

		      if(checkAck(in)!=0){
		    	  System.err.println("checkAck(in)[" + checkAck(in) + "]: line[" + new Exception().getStackTrace()[0].getLineNumber() + "]");
		    	  return false;
		      }

		      File _lfile = new File(lfile);

		      if(ptimestamp){
		        command="T "+(_lfile.lastModified()/1000)+" 0";
		        // The access time should be sent here,
		        // but it is not accessible with JavaAPI ;-<
		        command+=(" "+(_lfile.lastModified()/1000)+" 0\n"); 
		        out.write(command.getBytes()); out.flush();
		        if(checkAck(in)!=0){
			    	  System.err.println("checkAck(in)[" + checkAck(in) + "]: line[" + new Exception().getStackTrace()[0].getLineNumber() + "]");
			    	  return false;
		        }
		      }

		      // send "C0644 filesize filename", where filename should not include '/'
		      long filesize=_lfile.length();
		      command="C0644 "+filesize+" ";
		      if(lfile.lastIndexOf('/')>0){
		        command+=lfile.substring(lfile.lastIndexOf('/')+1);
		      }
		      else{
		        command+=lfile;
		      }
		      command+="\n";
		      out.write(command.getBytes()); out.flush();
		      if(checkAck(in)!=0){
		    	  System.err.println("checkAck(in)[" + checkAck(in) + "]: line[" + new Exception().getStackTrace()[0].getLineNumber() + "]");
		    	  return false;
		      }

		      // send a content of lfile
		      fis=new FileInputStream(lfile);
		      byte[] buf=new byte[1024];
		      while(true){
		        int len=fis.read(buf, 0, buf.length);
			if(len<=0) break;
		        out.write(buf, 0, len); //out.flush();
		      }
		      fis.close();
		      fis=null;
		      // send '\0'
		      buf[0]=0; out.write(buf, 0, 1); out.flush();
		      if(checkAck(in)!=0){
		    	  System.err.println("checkAck(in)[" + checkAck(in) + "]: line[" + new Exception().getStackTrace()[0].getLineNumber() + "]");
		    	  return false;
		      }
		      out.close();

		      channel.disconnect();
		      session.disconnect();
		    }
		    catch(Exception e){
		    	e.printStackTrace();
		    	returnValue = false;
		      try{if(fis!=null)fis.close();}
		      catch(Exception ee){
		    	  return false;
		    	  }
		    }
		return returnValue;
	}
	
	@SuppressWarnings("unused")
	public void sendCustomCommand(String[] arg, CUSTOMCOMMAND commandenum)
	{
		if(arg.length!=2){
		      System.err.println("usage: java ScpTo file1 user@remotehost:file2");
		}			

	    FileInputStream fis=null;

	    System.out.println("arg[0][" + arg[0] + "]");
	    System.out.println("arg[1][" + arg[1] + "]");
	    
		try{
			arg[1]=arg[1].substring(arg[1].indexOf('@')+1);
			String host=arg[1].substring(0, arg[1].indexOf(':'));

		      JSch jsch=new JSch();
		      Session session=jsch.getSession(bigxcontext.getiXerciseAccountName(), host, BiGXConnectionToBackend.ScpServerPort);
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
						if(bigxcontext.getiXercisePassword() != "")
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
						return bigxcontext.getiXercisePassword();
					}
					
					@Override
					public String getPassphrase() {
						return null;
					}
		      });
		      session.setPassword(bigxcontext.getiXercisePassword());
		      session.connect();

		      // exec 'scp -t rfile' remotely
		      String command = "";
		      
		      switch(commandenum)
		      {
		      case GETGAMEDESIGN:
		    	  command="bigx-getGameDesign " + bigxcontext.getiXerciseAccountName() + " " + bigxcontext.getiXercisePassword() + " " + this.bigxcontext.getiXercisePatientProfile().getCaseid();
		    	  break;
		      case GETPATIENTINFO:
		    	  command="bigx-getPatientInfo " + bigxcontext.getiXerciseAccountName() + " " + bigxcontext.getiXercisePassword();
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
		      case GETGAMEDESIGN:
		    	  if( (output.length() != 0) && (!output.equals("{}")) )
			      {
			    	  BiGXSuggestedGameProperties biGXPatientInfo = new Gson().fromJson(output, BiGXSuggestedGameProperties.class);
			    	  this.bigxcontext.setBiGXSuggestedGameProperties(biGXPatientInfo);
			    	  BiGXMiddlewareCore.iXercisePatientInfoRetrieved = 1;
			      }
			      else{
			    	  BiGXMiddlewareCore.iXercisePatientInfoRetrieved = -1;
			      }
		    	  break;
		      case GETPATIENTINFO:
		    	  if( (output.length() != 0) && (!output.equals("{}")) )
			      {
			    	  BiGXPatientInfo biGXPatientInfo = new Gson().fromJson(output, BiGXPatientInfo.class);
			    	  this.bigxcontext.setiXercisePatientProfile(biGXPatientInfo);
			    	  BiGXMiddlewareCore.iXercisePatientInfoRetrieved = 1;
			      }
			      else{
			    	  BiGXMiddlewareCore.iXercisePatientInfoRetrieved = -1;
			      }
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