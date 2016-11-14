package com.sabre.bigdata.smav2.job;

import static com.sabre.bigdata.smav2.util.ApplicationConstants.AVRO_MAX_INPUT_SPLIT_SIZE_PROP;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.COMMA;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.DATE_RANGE_PROP;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.DEFAULT_DATE_RANGE;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.DEFAULT_NUM_REDUCERS;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.FIELDS_SEPARATOR_PROP;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.GIGABYTE;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.NO_COMPRESSION;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.NO_PROPERTY_VALUE;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.NUM_REDUCE_TASKS_PROP;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.OUTPUT_FILE_COMPRESS_PROP;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.PIPE;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.POS_BOOKING_CHANNELS;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.POS_BOOKING_CHANNELS_PROP;
import static com.sabre.bigdata.smav2.util.ApplicationConstants.ROBOTIC_FIELD_SEPARATOR_PROP;

import java.util.Arrays;

import org.apache.avro.mapreduce.AvroJob;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.io.compress.GzipCodec;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.lib.input.MultipleInputs;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;

import com.sabre.bigdata.smav2.comparator.TaggedJoiningGroupingComparator;
import com.sabre.bigdata.smav2.input.CombinedAvroKeyInputFormat;
import com.sabre.bigdata.smav2.mapper.RoboticShoppingMapper;
import com.sabre.bigdata.smav2.mapper.ShopRecordMapper;
import com.sabre.bigdata.smav2.output.RequestFileOutputFormat;
import com.sabre.bigdata.smav2.partitioner.TaggedJoiningPartitioner;
import com.sabre.bigdata.smav2.reducer.SMAReducer;
import com.sabre.bigdata.smav2.types.TaggedKey;
import com.sabre.bigdata.smav2.types.TaggedValue;
import com.sabre.bigdata.smav2.util.PathDateParser;
import com.sabre.bigdata.smav2.util.StatusFile;
import com.sabre.bigsky.avro.shoprecord.ShopRecord;

public class SMAV2ExtractJob extends Configured implements Tool {
	private static final Log LOG = LogFactory.getLog(SMAV2ExtractJob.class);
	private StatusFile statusFile;
	
	@Override
	public int run(String[] args) throws Exception {
		//TODO: in order to set parameters these must be passed before arguments
		
		if (args.length != 4) {
			System.err
					.println("Usage: SMAExtractJob <ShopRecord inpout path> <RoboticShopping inpout path> <REQ/RES Output path> <Status file path>");
			System.exit(-1);
		}
		
		Configuration config = getConf();
		statusFile = new StatusFile(PathDateParser.parseDateFromPath(args[0]), config, args[3]);
		statusFile.begin();
		int departureDateRange = config.getInt(DATE_RANGE_PROP, NO_PROPERTY_VALUE);
		if (NO_PROPERTY_VALUE == departureDateRange) {
			LOG.info("Configuration property: [" + DATE_RANGE_PROP + "] is not set. Using default value: ["
					+ DEFAULT_DATE_RANGE + "]");
			config.setInt(DATE_RANGE_PROP, DEFAULT_DATE_RANGE);
		}
		String fieldsSeparator = config.get(FIELDS_SEPARATOR_PROP);
		if (fieldsSeparator == null) {
			LOG.info("Configuration property: [" + FIELDS_SEPARATOR_PROP + "] is not set. Using default value: ["
					+ PIPE + "]");
			config.set(FIELDS_SEPARATOR_PROP, PIPE);
		}
		String roboticShoppingFieldsSeparator = config.get(ROBOTIC_FIELD_SEPARATOR_PROP);
		if (roboticShoppingFieldsSeparator == null) {
			LOG.info("Configuration property: [" + ROBOTIC_FIELD_SEPARATOR_PROP
					+ "] is not set. Using default value: [" + COMMA + "]");
			config.set(ROBOTIC_FIELD_SEPARATOR_PROP, COMMA);
		}
		String[] posBookingChannels = config.getStrings(POS_BOOKING_CHANNELS_PROP);
		if (posBookingChannels == null || posBookingChannels.length == 0) {
			LOG.info("Configuration property: [" + POS_BOOKING_CHANNELS_PROP + "] is not set. Using default value: ["
					+ Arrays.toString(POS_BOOKING_CHANNELS) + "]");
			config.setStrings(POS_BOOKING_CHANNELS_PROP, POS_BOOKING_CHANNELS);
		}

		Job job = Job.getInstance(config, "SMA_V2_Extract_Job");
		job.setJarByClass(SMAV2ExtractJob.class);

		job.setMapperClass(RoboticShoppingMapper.class);
		job.setMapperClass(ShopRecordMapper.class);
		job.setReducerClass(SMAReducer.class);
		job.setPartitionerClass(TaggedJoiningPartitioner.class);
		job.setGroupingComparatorClass(TaggedJoiningGroupingComparator.class);

		job.setMapOutputKeyClass(TaggedKey.class);
		job.setMapOutputValueClass(TaggedValue.class);

		job.setOutputKeyClass(Text.class);
		job.setOutputValueClass(NullWritable.class);

		AvroJob.setInputKeySchema(job, ShopRecord.getClassSchema());

		long maxInputSplitSize = config.getLong(AVRO_MAX_INPUT_SPLIT_SIZE_PROP, NO_PROPERTY_VALUE);
		if (maxInputSplitSize != NO_PROPERTY_VALUE) {
			CombinedAvroKeyInputFormat.setMaxInputSplitSize(job, maxInputSplitSize);
		} else {
			LOG.info("Configuration property: [" + AVRO_MAX_INPUT_SPLIT_SIZE_PROP
					+ "] is not set. Using default value: [" + GIGABYTE + "]");
			CombinedAvroKeyInputFormat.setMaxInputSplitSize(job, GIGABYTE);
		}

		MultipleInputs.addInputPath(job, new Path(args[0]), CombinedAvroKeyInputFormat.class, ShopRecordMapper.class);
		MultipleInputs.addInputPath(job, new Path(args[1]), TextInputFormat.class, RoboticShoppingMapper.class);

		RequestFileOutputFormat.setOutputPath(job, new Path(args[2]));

		boolean compressOutputFile = config.getBoolean(OUTPUT_FILE_COMPRESS_PROP, NO_COMPRESSION);
		if (compressOutputFile) {
			LOG.info("The output files will be compressed using gzip codec");
			RequestFileOutputFormat.setCompressOutput(job, true);
			RequestFileOutputFormat.setOutputCompressorClass(job, GzipCodec.class);
		} else {
			LOG.info("The output files will not be compressed");
		}

		job.setOutputFormatClass(RequestFileOutputFormat.class);

		int numberOfReduceTasks = config.getInt(NUM_REDUCE_TASKS_PROP, NO_PROPERTY_VALUE);
		if (NO_PROPERTY_VALUE != numberOfReduceTasks) {
			job.setNumReduceTasks(numberOfReduceTasks);
		} else {
			LOG.info("Configuration property: [" + NUM_REDUCE_TASKS_PROP + "] is not set. Using default value: ["
					+ DEFAULT_NUM_REDUCERS + "]");
			job.setNumReduceTasks(DEFAULT_NUM_REDUCERS);
		}

		boolean result = job.waitForCompletion(true);

		if(result){
			statusFile.success(job.getCounters());
		}else{
			statusFile.fail();
		}
		
		return result ? 0 : -1;
	}

	public static void main(String[] args) throws Exception {
		SMAV2ExtractJob job = new SMAV2ExtractJob();
		System.exit(ToolRunner.run(job, args));
	}

}
