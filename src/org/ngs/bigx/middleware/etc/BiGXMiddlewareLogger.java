package org.ngs.bigx.middleware.etc;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BiGXMiddlewareLogger {
	public static boolean isLoggingOn = false;
	
	public static void print(String className, String message)
	{
		if(BiGXMiddlewareLogger.isLoggingOn)
		{
			Logger localLogger = Logger.getLogger(className);
			localLogger.log(Level.INFO, message);
		}
	}
	
	public static void printSevere(String className, String message)
	{
		if(BiGXMiddlewareLogger.isLoggingOn)
		{
			Logger localLogger = Logger.getLogger(className);
			localLogger.log(Level.SEVERE, message);
		}
	}
}