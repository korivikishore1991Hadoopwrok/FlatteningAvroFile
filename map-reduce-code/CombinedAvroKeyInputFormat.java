package com.sabre.bigdata.smav2.input;

import java.io.IOException;

import org.apache.avro.Schema;
import org.apache.avro.mapred.AvroKey;
import org.apache.avro.mapreduce.AvroJob;
import org.apache.avro.mapreduce.AvroKeyRecordReader;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapreduce.InputSplit;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.lib.input.CombineFileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.CombineFileRecordReader;
import org.apache.hadoop.mapreduce.lib.input.CombineFileSplit;
import org.apache.hadoop.mapreduce.lib.input.FileSplit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CombinedAvroKeyInputFormat<T> extends
		CombineFileInputFormat<AvroKey<T>, NullWritable> {
	
	private static final Logger LOG = LoggerFactory.getLogger(CombinedAvroKeyInputFormat.class);

	public static class CombinedAvroKeyRecordReader<T> extends
			AvroKeyRecordReader<T> {
		private CombineFileSplit inputSplit;
		private Integer idx;

		public CombinedAvroKeyRecordReader(CombineFileSplit inputSplit,
				TaskAttemptContext context, Integer idx) {
			super(AvroJob.getInputKeySchema(context.getConfiguration()));
			this.inputSplit = inputSplit;
			this.idx = idx;
		}

		@Override
		public void initialize(InputSplit inputSplit, TaskAttemptContext context)
				throws IOException, InterruptedException {
			this.inputSplit = (CombineFileSplit) inputSplit;

			FileSplit fileSplit = new FileSplit(this.inputSplit.getPath(idx),
					this.inputSplit.getOffset(idx),
					this.inputSplit.getLength(idx),
					this.inputSplit.getLocations());

			super.initialize(fileSplit, context);
		}
	}

	@SuppressWarnings("unchecked")
	@Override
	public RecordReader<AvroKey<T>, NullWritable> createRecordReader(
			InputSplit inputSplit, TaskAttemptContext context)
			throws IOException {
		Schema readerSchema = AvroJob.getInputKeySchema(context
				.getConfiguration());
		if (null == readerSchema) {
			LOG.warn("Reader schema was not set. Use AvroJob.setInputKeySchema() if desired.");
			LOG.info("Using a reader schema equal to the writer schema.");
		}

		Object c = CombinedAvroKeyRecordReader.class;
		return new CombineFileRecordReader<AvroKey<T>, NullWritable>(
				(CombineFileSplit) inputSplit, context,
				(Class<? extends RecordReader<AvroKey<T>, NullWritable>>) c);
	}
}
