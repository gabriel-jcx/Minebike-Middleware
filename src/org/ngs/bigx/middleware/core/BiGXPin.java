package org.ngs.bigx.middleware.core;

public class BiGXPin {
	public int pinDataType;
	public Object pin;
	
	public enum pinToListenEnum{
		ANALOG_00(0),
		ANALOG_01(1),
		ANALOG_02(2),
		ANALOG_03(3),
		ANALOG_04(4),
		ANALOG_05(5),
		ANALOG_06(6),
		ANALOG_07(7),
		ANALOG_08(8),
		ANALOG_09(9),
		ANALOG_END(10),

		DIGITAL_00(11),
		DIGITAL_01(12),
		DIGITAL_02(13),
		DIGITAL_03(14),
		DIGITAL_04(15),
		DIGITAL_05(16),
		DIGITAL_06(17),
		DIGITAL_07(18),
		DIGITAL_08(19),
		DIGITAL_09(20),
		DIGITAL_10(21),
		DIGITAL_11(22),
		DIGITAL_12(23),
		DIGITAL_13(24),
		DIGITAL_14(25),
		DIGITAL_15(26),
		DIGITAL_16(27),
		DIGITAL_END(28),
		
		ENDOFPINTOLISTENENUM(255);
		
		/* TO BE ADDED PER ADDITIONAL COMMUNICATION CHANNELS */
		
		private final int value;
	    private pinToListenEnum(int value) {
	        this.value = value;
	    }

	    public int getValue() {
	        return value;
	    }

	    public static pinToListenEnum fromInt(int i) {
	        for (pinToListenEnum b : pinToListenEnum .values()) {
	            if (b.getValue() == i) { return b; }
	        }
	        return null;
	    }
	};

}
