package com.tenzenway.arduino.toy;

public class Constant {
	public static final int PACKET_SIZE = 18;
	public static final int SAMPLE_RATE = 128;
	public static final int SENSING_LEVEL = 1024;
	public static final int DOMAIN_BOUNDARY = SAMPLE_RATE * 4;
	
	// for DataLink state machine
	public static final int SYNC_STATE = 0;
	public static final int SEQ_STATE = 1;
	public static final int DATA_STATE = 2;
	
	public static final int DATA_SIZE = 8;
}
