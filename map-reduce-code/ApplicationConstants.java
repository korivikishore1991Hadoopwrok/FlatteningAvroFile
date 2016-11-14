package com.sabre.bigdata.smav2.util;

import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.NullWritable;

public interface ApplicationConstants {

	IntWritable SHOP_RECORD_DATA = new IntWritable(1);
	
	IntWritable ROBOTIC_SHOPPING_DATA = new IntWritable(0);
	
	String POS_BOOKING_CHANNELS_PROP = "mapper.shoprecord.posBookingChannels";
	
	String ROBOTIC_FIELD_SEPARATOR_PROP = "mapper.robotic.shopping.field.separator";
	
	String FIELDS_SEPARATOR_PROP = "outputfile.fields.separator";
	
	String DATE_RANGE_PROP = "mapper.shoprecord.departure.daterange";
	
	String AVRO_MAX_INPUT_SPLIT_SIZE_PROP = "avro.max.input.split.size";
	
	String OUTPUT_FILE_COMPRESS_PROP = "output.file.compress";
	
	String NUM_REDUCE_TASKS_PROP = "reducers";
	
	String ONE_WAY = "ONE-WAY";

	String ROUND_TRIP = "ROUND-TRIP";
	
	String PIPE = "|";
	
	String EMPTY = "";

	String ZERO = "0";
	
	String COMMA = ",";
	
	String[] POS_BOOKING_CHANNELS = {"1S", "CSI", "EXPD", "GTHR", "LMC", "SABREPAC", "TN", "TTRIP", "TVLYEU", "TVLY", "TVLYTEST"};

	int TN_PCC_MAX_LENGTH = 4;
	
	char SIX = '6';
	
	char EIGHT = '8';
	
	char D = 'D';
	
	int DEFAULT_DATE_RANGE = 363;
	
	int NO_PROPERTY_VALUE = -1;
	
	long GIGABYTE = 1073741824L;
	
	int DEFAULT_NUM_REDUCERS = 1;
	
	boolean NO_COMPRESSION = false;
	
	NullWritable NULL = NullWritable.get();
}
