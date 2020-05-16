package org.ngs.bigx.middleware.core;

import org.ngs.bigx.dictionary.protocol.*;

public class BiGXMiddlewareTranslator {
	// Do I have to store the raw data? Yes!
	// Current data?
	// History? (Let's use the Queue)
	// Hashtable to accelerate to access the data history (Queue)
	// Need to store the data in a file.
	// The Hash table's key represents the data value type e.g. speed/heartrate the value is from our protocol's spec
	// The value part leads you to the actual data set. The queue will hold first 256 data set and the system will archive the value in the file. 
	public BiGXMiddlewareTranslator() {}
	
	public synchronized double updateRawData(int key, int valueType, double value) throws Exception
	{	
		double translatedValue = 0;
		double returnValue = Double.MIN_VALUE;
		
		// Translate!!!
		translatedValue = translationFunction.translateRawToAbstract(key, valueType, value);
		returnValue = translatedValue;
		
		return returnValue;
	}
	
	public double translate(int key, double rawValue)
	{
		
		return 0.0;
	}
}
