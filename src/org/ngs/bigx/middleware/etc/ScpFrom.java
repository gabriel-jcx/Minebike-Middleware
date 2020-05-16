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
import com.jcraft.jsch.*;

import java.io.*;

import org.ngs.bigx.middleware.core.BiGXConnectionToBackend;
import org.ngs.bigx.middleware.core.BiGXMiddlewareCore;

public class ScpFrom{
	
	public BiGXMiddlewareCore bigxcontext;
	
	public ScpFrom(BiGXMiddlewareCore bigxcontext)
	{
		this.bigxcontext = bigxcontext;
	}
	/**
	 * The function sends out the source file to the destination
	 * @param arg arg[0]: source file, arg[1]: user@remotehost:file2
	 */
	public void sendFile(String[] arg)
	{
		if(arg.length!=2){
			System.err.println("usage: java ScpFrom user@remotehost:file1 file2");
		}			

	    FileOutputStream fos=null;

	    System.out.println("arg[0][" + arg[0] + "]");
	    System.out.println("arg[1][" + arg[1] + "]");
	    
		try{
		      String user=arg[1].substring(0, arg[1].indexOf('@'));
		      arg[1]=arg[1].substring(arg[1].indexOf('@')+1);
		      String host=arg[1].substring(0, arg[1].indexOf(':'));
		      String rfile=arg[1].substring(arg[1].indexOf(':')+1);
		      String lfile=arg[0];

		      String prefix=null;
		      if(new File(lfile).isDirectory()){
		        prefix=lfile+File.separator;
		      }

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

		      // exec 'scp -f rfile' remotely
		      String command="scp -f "+rfile;
		      Channel channel=session.openChannel("exec");
		      ((ChannelExec)channel).setCommand(command);

		      // get I/O streams for remote scp
		      OutputStream out=channel.getOutputStream();
		      InputStream in=channel.getInputStream();

		      channel.connect();

		      byte[] buf=new byte[1024];

		      // send '\0'
		      buf[0]=0; out.write(buf, 0, 1); out.flush();

		      while(true){
		    	  int c=checkAck(in);
		    	  if(c!='C'){
		    		  break;
		    	  }

		    	  // read '0644 '
		    	  in.read(buf, 0, 5);

		    	  long filesize=0L;
		    	  while(true){
		    		  if(in.read(buf, 0, 1)<0){
		    			  // error
		    			  break; 
		    		  }
		    		  if(buf[0]==' ')break;
		    		  filesize=filesize*10L+(long)(buf[0]-'0');
		    	  }

		    	  String file=null;
		    	  for(int i=0;;i++){
		    		  in.read(buf, i, 1);
		    		  if(buf[i]==(byte)0x0a){
		    			  file=new String(buf, 0, i);
		    			  break;
		    		  }
		    	  }

		    	  // System.out.println("filesize="+filesize+", file="+file);

		    	  // send '\0'
		    	  buf[0]=0; out.write(buf, 0, 1); out.flush();

		    	  // read a content of lfile
		    	  fos=new FileOutputStream(prefix==null ? lfile : prefix+file);
		    	  int foo;
		    	  while(true){
		    		  if(buf.length<filesize) foo=buf.length;
		    		  else foo=(int)filesize;
		    		  foo=in.read(buf, 0, foo);
		    		  if(foo<0){
		    			  // error 
		    			  break;
		    		  }
		    		  fos.write(buf, 0, foo);
		    		  filesize-=foo;
		    		  if(filesize==0L) break;
		    	  }
		    	  fos.close();
		    	  fos=null;

		    	  if(checkAck(in)!=0){
		    		  System.err.println("check ack[" + checkAck(in) + "]");
		    	  }

		    	  // send '\0'
		    	  buf[0]=0; out.write(buf, 0, 1); out.flush();
		      }

		      session.disconnect();
		}
		catch(Exception e){
			e.printStackTrace();
			try{if(fos!=null)fos.close();}catch(Exception ee){}
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