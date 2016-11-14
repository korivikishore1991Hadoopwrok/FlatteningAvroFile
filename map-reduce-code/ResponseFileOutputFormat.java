package com.sabre.bigdata.smav2.output;

import java.io.IOException;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.TaskAttemptContext;
import org.apache.hadoop.mapreduce.TaskAttemptID;
import org.apache.hadoop.mapreduce.lib.output.FileOutputCommitter;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

public class ResponseFileOutputFormat<K, V> extends TextOutputFormat<K, V> {

	public static final String FILE_PREFIX = "RES-file-";

	@Override
	public Path getDefaultWorkFile(TaskAttemptContext context, String extension)
			throws IOException {
		FileOutputCommitter committer = (FileOutputCommitter) getOutputCommitter(context);
		return new Path(committer.getWorkPath(), createFileName(context,
				extension));
	}

	private String createFileName(TaskAttemptContext context, String extension) {
		TaskAttemptID taskAttemptID = context.getTaskAttemptID();
		return FILE_PREFIX + taskAttemptID.getTaskID().getId() + "-"
				+ taskAttemptID.getId() + extension;
	}

}
