package org.ngs.bigx.middleware.exceptions;

public class BiGXDeviceIDOverlapException extends Exception
{
	private static final long serialVersionUID = 6973368413867269054L;
	
	public BiGXDeviceIDOverlapException(String message)
	{
		super(message);
	}
}
